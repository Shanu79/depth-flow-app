package com.shin.depthflow.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shin.depthflow.data.ApiException
import com.shin.depthflow.data.DepthFlowRepository
import com.shin.depthflow.data.PromoItem
import com.shin.depthflow.data.WhatsNewPrefs
import com.shin.depthflow.data.WhatsNewRelease
import com.shin.depthflow.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * Admin-managed content (see ANDROID_ADMIN_CONTENT.md): What's New modal, home popup and
 * pricing offer banner. The app only reads. Every failure means "show nothing".
 *
 * What's New is not a popup: it is listed in the Notifications screen as expandable banners.
 */
class ContentViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DepthFlowRepository(application)
    private val prefs = WhatsNewPrefs(application)

    // --- What's New (listed under Notifications, never auto-opened) ---
    private val _releases = MutableStateFlow<List<WhatsNewRelease>>(emptyList())
    val releases: StateFlow<List<WhatsNewRelease>> = _releases.asStateFlow()

    /** True while the current `announce_version` has not been opened yet. Drives the bell badge and "NEW" chip. */
    private val _hasUnseenWhatsNew = MutableStateFlow(false)
    val hasUnseenWhatsNew: StateFlow<Boolean> = _hasUnseenWhatsNew.asStateFlow()

    private var announceVersion: Int? = null

    // --- Home popup ---
    private val _popup = MutableStateFlow<PromoItem?>(null)
    val popup: StateFlow<PromoItem?> = _popup.asStateFlow()
    private var popupFetchedFor: String? = null

    // --- Offer banner ---
    private val _banner = MutableStateFlow<PromoItem?>(null)
    val banner: StateFlow<PromoItem?> = _banner.asStateFlow()

    /** serverNow - deviceNow in ms, taken from the last response that carried `server_time`. */
    @Volatile
    var skewMillis: Long = 0L
        private set

    init {
        refreshWhatsNew()
    }

    // ---------------------------------------------------------------- What's New

    /** Fetches the releases. Called on launch and whenever the Notifications screen opens. */
    fun refreshWhatsNew() {
        viewModelScope.launch {
            try {
                val response = repository.getWhatsNew()
                _releases.value = response.releases.orEmpty()
                announceVersion = response.announce_version
                val version = response.announce_version
                _hasUnseenWhatsNew.value =
                    version != null && _releases.value.isNotEmpty() && version != prefs.seenVersion()
            } catch (e: Exception) {
                // Keep whatever was shown before; nothing is stored, so the badge retries next time.
                Logger.e(message = "What's New fetch failed: ${e.message}")
            }
        }
    }

    /** The user opened (expanded) a release: store `announce_version` so the badge clears. */
    fun markWhatsNewSeen() {
        val version = announceVersion ?: return
        if (!_hasUnseenWhatsNew.value) return
        _hasUnseenWhatsNew.value = false
        viewModelScope.launch {
            try { prefs.markSeen(version) } catch (e: Exception) { Logger.e(message = "markSeen failed: ${e.message}") }
        }
    }

    // ---------------------------------------------------------------- Home popup

    /**
     * Fetches the first eligible popup once per login. [bearer] is "Bearer <jwt>" or null when
     * logged out. The server stops returning a popup after it is tracked, so nothing is cached
     * across launches.
     */
    fun loadHomePopup(bearer: String?) {
        if (bearer == null) {
            popupFetchedFor = null
            _popup.value = null
            return
        }
        if (popupFetchedFor == bearer) return
        popupFetchedFor = bearer
        viewModelScope.launch {
            try {
                val first = repository.getEligiblePopups(bearer).popups.orEmpty().firstOrNull()
                _popup.value = first?.takeIf { it.isRenderable() }
            } catch (e: Exception) {
                // 401 or anything else: no popup. Never log the user out from here.
                Logger.e(message = "Popup fetch failed: ${e.message}")
                _popup.value = null
            }
        }
    }

    /** Called when the popup is actually on screen. Safe to repeat; failures are ignored (404 dismisses). */
    fun trackPopup(bearer: String?, id: Int) {
        if (bearer == null) return
        viewModelScope.launch {
            try {
                repository.trackPopup(bearer, id)
            } catch (e: ApiException) {
                if (e.httpCode == 404) _popup.value = null
            } catch (e: Exception) {
                Logger.e(message = "Popup track failed: ${e.message}")
            }
        }
    }

    fun dismissPopup() {
        _popup.value = null
    }

    // ---------------------------------------------------------------- Offer banner

    /** Refetch on every Pricing visit/resume. `{"banner": null}` or any failure means no banner. */
    fun refreshBanner() {
        viewModelScope.launch {
            try {
                val response = repository.getActiveBanner()
                response.server_time?.let { raw ->
                    runCatching { Instant.parse(raw).toEpochMilli() - System.currentTimeMillis() }
                        .getOrNull()?.let { skewMillis = it }
                }
                _banner.value = response.banner?.takeIf { it.isRenderable() }
            } catch (e: Exception) {
                Logger.e(message = "Banner fetch failed: ${e.message}")
                _banner.value = null
            }
        }
    }

    /** An `image` creative with no picture has nothing to show. */
    private fun PromoItem.isRenderable(): Boolean =
        if (display_mode == "image") !(mobile_image_url ?: image_url).isNullOrBlank() else true
}
