package com.shin.depthflow.util

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Pure helpers for Happy Hour countdowns. All server timestamps are UTC ISO-8601 ("...Z"). */
object HappyHourTime {

    fun parse(iso: String?): Instant? =
        try { if (iso.isNullOrBlank()) null else Instant.parse(iso) } catch (e: Exception) { null }

    /** serverTime - deviceNow. Add it to the device clock to approximate the server clock. */
    fun skew(serverTime: String?, deviceNow: Instant = Instant.now()): Duration =
        parse(serverTime)?.let { Duration.between(deviceNow, it) } ?: Duration.ZERO

    /** Whole seconds until [expiresAt] on the server clock, never negative. */
    fun secondsLeft(expiresAt: String?, skew: Duration, deviceNow: Instant = Instant.now()): Long {
        val end = parse(expiresAt) ?: return 0L
        return Duration.between(deviceNow.plus(skew), end).seconds.coerceAtLeast(0L)
    }

    fun formatCountdown(totalSeconds: Long): String {
        val s = totalSeconds.coerceAtLeast(0L)
        return "%02d:%02d".format(Locale.US, s / 60, s % 60)
    }

    /** Date like "Dec 1, 2026" in the local timezone. */
    fun formatDate(iso: String?): String {
        val t = parse(iso) ?: return ""
        return DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
            .withZone(ZoneId.systemDefault()).format(t)
    }
}
