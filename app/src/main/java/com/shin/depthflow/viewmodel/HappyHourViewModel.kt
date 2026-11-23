package com.shin.depthflow.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shin.depthflow.R
import com.shin.depthflow.data.ApiException
import com.shin.depthflow.data.DepthFlowRepository
import com.shin.depthflow.data.HappyHourSession
import com.shin.depthflow.data.HappyHourStatus
import com.shin.depthflow.data.InboxItem
import com.shin.depthflow.push.PushRegistrar
import com.shin.depthflow.util.HappyHourTime
import com.shin.depthflow.util.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.util.UUID

sealed class HappyHourEvent {
    data class Message(val text: String) : HappyHourEvent()

    /** A call returned 401: the session is no longer valid and the user must log in again. */
    object Unauthorized : HappyHourEvent()
}

/**
 * Happy Hour + notification inbox state. The server is the only authority on entitlement,
 * tokens and expiry. This class only mirrors /happy-hour/status and runs a display-only
 * countdown anchored to the server clock (skew), never persisted.
 */
class HappyHourViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DepthFlowRepository(application)

    // "Bearer <jwt>" or null when logged out. Pushed in from DepthFlowApp.
    private var bearer: String? = null

    private val _status = MutableStateFlow<HappyHourStatus?>(null)
    val status: StateFlow<HappyHourStatus?> = _status.asStateFlow()

    private val _secondsLeft = MutableStateFlow(0L)
    val secondsLeft: StateFlow<Long> = _secondsLeft.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isActivating = MutableStateFlow(false)
    val isActivating: StateFlow<Boolean> = _isActivating.asStateFlow()

    private val _history = MutableStateFlow<List<HappyHourSession>>(emptyList())
    val history: StateFlow<List<HappyHourSession>> = _history.asStateFlow()

    private val _inbox = MutableStateFlow<List<InboxItem>>(emptyList())
    val inbox: StateFlow<List<InboxItem>> = _inbox.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private val _events = MutableSharedFlow<HappyHourEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<HappyHourEvent> = _events.asSharedFlow()

    /** True while a session is running with time and a video slot left (UI hint only; the server decides). */
    val isUnlimited: StateFlow<Boolean> = combine(_status, _secondsLeft) { st, secs ->
        st != null && st.active && secs > 0 && st.remaining_video_allowance > 0
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private var skew: Duration = Duration.ZERO
    private var tickerJob: Job? = null

    // One Idempotency-Key per user tap. Kept only while a tap is unresolved (network error),
    // so retrying the same tap can never spend a second token.
    private var pendingActivationKey: String? = null

    /** Called whenever the logged-in token changes. */
    fun onAuthChanged(rawToken: String?) {
        val newBearer = rawToken?.takeIf { it.isNotEmpty() }?.let { "Bearer $it" }
        if (newBearer == bearer) return
        bearer = newBearer
        if (newBearer == null) {
            clearState()
        } else {
            refresh()
            refreshUnreadCount()
            // Idempotent on the server, so safe on every login and app start.
            viewModelScope.launch { PushRegistrar.register(newBearer) }
        }
    }

    private fun clearState() {
        tickerJob?.cancel()
        _status.value = null
        _secondsLeft.value = 0L
        _history.value = emptyList()
        _inbox.value = emptyList()
        _unreadCount.value = 0
        _isActivating.value = false
        pendingActivationKey = null
        skew = Duration.ZERO
    }

    /** GET /happy-hour/status. Call on resume, after purchase/generation, and when the countdown hits 0. */
    fun refresh() {
        val token = bearer ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                applyStatus(repository.getHappyHourStatus(token))
            } catch (e: Exception) {
                handleError(e, showMessage = false)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun activate() {
        val token = bearer ?: return
        if (_isActivating.value) return
        val key = pendingActivationKey ?: UUID.randomUUID().toString().also { pendingActivationKey = it }
        viewModelScope.launch {
            _isActivating.value = true
            try {
                applyStatus(repository.activateHappyHour(token, key))
                pendingActivationKey = null
            } catch (e: ApiException) {
                // Definitive server answer: the tap is resolved, the next tap gets a new key.
                pendingActivationKey = null
                handleError(e, showMessage = true)
                if (e.code == "SESSION_ACTIVE" || e.code == "NO_TOKENS" ||
                    e.code == "MONTHLY_LIMIT" || e.code == "NOT_SUBSCRIBED"
                ) refresh()
            } catch (e: Exception) {
                // Network/timeout: keep the key so a retry of this same tap is idempotent.
                handleError(e, showMessage = true)
            } finally {
                _isActivating.value = false
            }
        }
    }

    fun loadHistory() {
        val token = bearer ?: return
        viewModelScope.launch {
            try {
                _history.value = repository.getHappyHourHistory(token)
            } catch (e: Exception) {
                handleError(e, showMessage = false)
            }
        }
    }

    // --- Inbox ---
    fun loadInbox() {
        val token = bearer ?: return
        viewModelScope.launch {
            try {
                val resp = repository.getNotifications(token)
                _inbox.value = resp.items
                _unreadCount.value = resp.unread_count
            } catch (e: Exception) {
                handleError(e, showMessage = false)
            }
        }
    }

    fun refreshUnreadCount() {
        val token = bearer ?: return
        viewModelScope.launch {
            try {
                _unreadCount.value = repository.getUnreadCount(token).unread_count
            } catch (e: Exception) {
                handleError(e, showMessage = false)
            }
        }
    }

    fun markRead(id: Long) {
        val token = bearer ?: return
        viewModelScope.launch {
            try {
                val resp = repository.markNotificationRead(token, id)
                _unreadCount.value = resp.unread_count
                _inbox.value = _inbox.value.map {
                    if (it.id == id && it.read_at == null) it.copy(read_at = "read") else it
                }
            } catch (e: Exception) {
                handleError(e, showMessage = false)
            }
        }
    }

    fun markAllRead() {
        val token = bearer ?: return
        viewModelScope.launch {
            try {
                val resp = repository.markAllNotificationsRead(token)
                _unreadCount.value = resp.unread_count
                _inbox.value = _inbox.value.map { if (it.read_at == null) it.copy(read_at = "read") else it }
            } catch (e: Exception) {
                handleError(e, showMessage = false)
            }
        }
    }

    // --- Internals ---
    private fun applyStatus(st: HappyHourStatus) {
        skew = HappyHourTime.skew(st.server_time)
        _status.value = st
        updateSecondsLeft()
        restartTicker(st)
    }

    private fun updateSecondsLeft() {
        val st = _status.value
        _secondsLeft.value = if (st?.active == true) {
            HappyHourTime.secondsLeft(st.session?.expires_at, skew)
        } else 0L
    }

    // Display-only ticker, one tick per second, only while a session is active.
    private fun restartTicker(st: HappyHourStatus) {
        tickerJob?.cancel()
        if (!st.active) return
        tickerJob = viewModelScope.launch {
            while (true) {
                updateSecondsLeft()
                if (_secondsLeft.value <= 0L) {
                    refresh() // countdown hit 0: ask the server what happened
                    break
                }
                delay(1000)
            }
        }
    }

    private suspend fun handleError(e: Exception, showMessage: Boolean) {
        Logger.e("HappyHour", "Request failed: ${e.message}", e)
        if (e is ApiException && e.httpCode == 401) {
            _events.emit(HappyHourEvent.Unauthorized)
            return
        }
        if (!showMessage) return
        val text = when (e) {
            is ApiException -> e.detail
            else -> getApplication<Application>().getString(R.string.err_network_error)
        }
        _events.emit(HappyHourEvent.Message(text))
    }
}
