package com.shin.depthflow.data

import retrofit2.HttpException

// DTOs for the Happy Hour + notifications endpoints (see ANDROID_INTEGRATION.md).
// Field names mirror the backend JSON (snake_case). Everything is nullable/defaulted
// so new backend fields or omissions never crash parsing.

data class HappyHourSession(
    val id: Long? = null,
    val status: String? = null, // ACTIVE | EXPIRED | LIMIT_REACHED | INVALIDATED
    val subscription_id: String? = null,
    val started_at: String? = null,
    val expires_at: String? = null,
    val remaining_seconds: Long? = null,
    val max_video_generations: Int? = null,
    val successful_video_generations: Int? = null,
    val remaining_video_allowance: Int? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)

data class HappyHourStatus(
    val server_time: String? = null,
    val eligible: Boolean = false,
    val subscription_type: String? = null, // "monthly" | "yearly"
    val total_allocated_tokens: Int = 0,
    val available_tokens: Int = 0,
    val total_claimed_tokens: Int = 0,
    val last_token_activation_month: String? = null,
    val can_activate_this_month: Boolean = false,
    val blocked_reason: String? = null, // null | NOT_SUBSCRIBED | SESSION_ACTIVE | NO_TOKENS | MONTHLY_LIMIT
    val next_activation_available_at: String? = null,
    val entitlement_expires_at: String? = null,
    val session_duration_seconds: Int = 3600,
    val active: Boolean = false,
    val session: HappyHourSession? = null,
    val remaining_video_allowance: Int = 0,
    val idempotent_replay: Boolean? = null // only present on /activate
)

data class HappyHourSessionResponse(
    val server_time: String? = null,
    val active: Boolean = false,
    val session: HappyHourSession? = null
)

data class InboxItem(
    val id: Long,
    val type: String? = null,
    val title: String? = null,
    val body: String? = null,
    val data: Map<String, Any?>? = null,
    val created_at: String? = null,
    val read_at: String? = null
)

data class InboxResponse(
    val server_time: String? = null,
    val unread_count: Int = 0,
    val items: List<InboxItem> = emptyList()
)

data class UnreadCountResponse(
    val server_time: String? = null,
    val unread_count: Int = 0
)

data class ReadResponse(
    val status: String? = null,
    val unread_count: Int = 0,
    val marked: Int? = null
)

data class DeviceRequest(
    val token: String,
    val platform: String = "android",
    val app_version: String? = null
)

data class DeviceUnregisterRequest(val token: String)

data class DeviceStatusResponse(val status: String? = null)

/** Backend error: `{"detail": "...", "code": "MONTHLY_LIMIT"}`. */
class ApiException(
    val httpCode: Int,
    val code: String?,
    val detail: String
) : Exception(detail)

private val errorAdapter by lazy {
    ApiClient.moshi.adapter(Map::class.java)
}

/** Converts a Retrofit failure into an [ApiException] carrying the server's `code` and `detail`. */
fun HttpException.toApiException(): ApiException {
    val raw = try { response()?.errorBody()?.string() } catch (e: Exception) { null }
    var code: String? = null
    var detail: String? = null
    if (!raw.isNullOrBlank()) {
        try {
            val map = errorAdapter.fromJson(raw)
            code = map?.get("code") as? String
            detail = when (val d = map?.get("detail")) {
                is String -> d
                is List<*> -> d.joinToString("; ") { (it as? Map<*, *>)?.get("msg")?.toString() ?: it.toString() }
                else -> null
            }
        } catch (e: Exception) { /* not JSON: fall through */ }
    }
    return ApiException(code(), code, detail ?: message())
}
