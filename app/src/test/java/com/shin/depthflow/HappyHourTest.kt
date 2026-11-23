package com.shin.depthflow

import com.shin.depthflow.data.ApiClient
import com.shin.depthflow.data.HappyHourStatus
import com.shin.depthflow.data.InboxResponse
import com.shin.depthflow.data.toApiException
import com.shin.depthflow.util.HappyHourTime
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.time.Duration
import java.time.Instant

class HappyHourTest {

    private val json = "application/json".toMediaType()

    @Test
    fun skew_isServerMinusDevice() {
        val device = Instant.parse("2026-11-15T12:00:00Z")
        val skew = HappyHourTime.skew("2026-11-15T12:00:30.123456Z", device)
        assertEquals(30L, skew.seconds)
    }

    @Test
    fun secondsLeft_usesServerClock_andNeverGoesNegative() {
        val device = Instant.parse("2026-11-15T12:00:00Z")
        // device clock is 60 s behind the server
        val skew = Duration.ofSeconds(60)
        assertEquals(540L, HappyHourTime.secondsLeft("2026-11-15T12:10:00Z", skew, device))
        assertEquals(0L, HappyHourTime.secondsLeft("2026-11-15T12:00:30Z", skew, device))
        assertEquals(0L, HappyHourTime.secondsLeft(null, skew, device))
        assertEquals(0L, HappyHourTime.secondsLeft("garbage", skew, device))
    }

    @Test
    fun formatCountdown() {
        assertEquals("59:59", HappyHourTime.formatCountdown(3599))
        assertEquals("00:05", HappyHourTime.formatCountdown(5))
        assertEquals("00:00", HappyHourTime.formatCountdown(-3))
    }

    @Test
    fun status_parsesYearlyBlockedResponse() {
        val raw = """
            {"server_time":"2026-11-15T11:04:23.059617Z","eligible":true,"subscription_type":"yearly",
             "total_allocated_tokens":4,"available_tokens":3,"total_claimed_tokens":1,
             "last_token_activation_month":"2026-11","can_activate_this_month":false,
             "blocked_reason":"MONTHLY_LIMIT","next_activation_available_at":"2026-12-01T00:00:00Z",
             "entitlement_expires_at":"2027-11-16T11:04:12Z","session_duration_seconds":3600,
             "active":false,"session":null,"remaining_video_allowance":0,"some_new_field":123}
        """.trimIndent()
        val st = ApiClient.moshi.adapter(HappyHourStatus::class.java).fromJson(raw)!!
        assertTrue(st.eligible)
        assertFalse(st.can_activate_this_month)
        assertEquals("MONTHLY_LIMIT", st.blocked_reason)
        assertEquals(3, st.available_tokens)
        assertNull(st.session)
    }

    @Test
    fun status_parsesActiveSession() {
        val raw = """
            {"server_time":"2026-11-15T11:04:23Z","eligible":true,"active":true,
             "session":{"id":1,"status":"ACTIVE","expires_at":"2026-11-15T12:04:23Z",
             "remaining_seconds":3599,"max_video_generations":10,
             "successful_video_generations":2,"remaining_video_allowance":8},
             "remaining_video_allowance":8,"idempotent_replay":false}
        """.trimIndent()
        val st = ApiClient.moshi.adapter(HappyHourStatus::class.java).fromJson(raw)!!
        assertTrue(st.active)
        assertEquals(8, st.remaining_video_allowance)
        assertEquals("2026-11-15T12:04:23Z", st.session?.expires_at)
        assertEquals(false, st.idempotent_replay)
    }

    @Test
    fun inbox_parsesMixedDataTypes() {
        val raw = """
            {"server_time":"2026-11-15T11:04:23Z","unread_count":2,"items":[
              {"id":5,"type":"HAPPY_HOUR_STARTED","title":"Live","body":"Go",
               "data":{"route":"happy_hour","session_id":1,"expires_at":"2026-11-15T12:04:23Z"},
               "created_at":"2026-11-15T11:04:23Z","read_at":null}]}
        """.trimIndent()
        val inbox = ApiClient.moshi.adapter(InboxResponse::class.java).fromJson(raw)!!
        assertEquals(2, inbox.unread_count)
        assertEquals("happy_hour", inbox.items.first().data?.get("route"))
        assertNull(inbox.items.first().read_at)
    }

    @Test
    fun httpError_mapsCodeAndDetail() {
        val body = """{"detail":"Already used this month","code":"MONTHLY_LIMIT"}""".toResponseBody(json)
        val ex = HttpException(Response.error<Any>(403, body)).toApiException()
        assertEquals(403, ex.httpCode)
        assertEquals("MONTHLY_LIMIT", ex.code)
        assertEquals("Already used this month", ex.detail)
    }

    @Test
    fun httpError_validationDetailListIsJoined() {
        val body = """{"detail":[{"loc":["body","token"],"msg":"too short","type":"x"}]}""".toResponseBody(json)
        val ex = HttpException(Response.error<Any>(422, body)).toApiException()
        assertEquals(422, ex.httpCode)
        assertNull(ex.code)
        assertEquals("too short", ex.detail)
    }

    @Test
    fun httpError_nonJsonBodyFallsBackToMessage() {
        val body = "oops".toResponseBody("text/plain".toMediaType())
        val ex = HttpException(Response.error<Any>(500, body)).toApiException()
        assertEquals(500, ex.httpCode)
        assertNull(ex.code)
    }
}
