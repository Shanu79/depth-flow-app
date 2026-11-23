package com.shin.depthflow.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// DTOs for the admin-managed content surfaces (see ANDROID_ADMIN_CONTENT.md):
// home popup, pricing offer banner and What's New. Everything is nullable/defaulted and unknown
// fields are ignored, so backend additions or omissions never break parsing.

/** One popup or banner. The popup and the banner share most fields, so one model serves both. */
data class PromoItem(
    val id: Int = 0,
    val title: String? = null,
    val description: String? = null,
    val image_url: String? = null,
    val button_text: String? = null,
    val button_link: String? = null,
    val coupon_code: String? = null,
    val button_color: String? = null,
    val text_color: String? = null,
    val bg_color: String? = null,
    val bg_color2: String? = null,
    val accent_color: String? = null,
    val header_text: String? = null,
    val footnote: String? = null,
    val badge_text: String? = null, // banner only
    val layout: String? = null, // popup only: "split" | "stack"
    val theme: String? = null, // none | snow | lights | confetti | sparkles | rain
    val emoji: String? = null,
    val font_style: String? = null, // modern | elegant | bold
    val bg_image_url: String? = null,
    val mobile_image_url: String? = null, // banner only
    val display_mode: String? = null, // "designed" | "image"
    val show_countdown: Boolean? = null,
    val end_at: String? = null,
    val priority: Int? = null
)

data class PopupsResponse(val popups: List<PromoItem>? = null)

data class TrackResponse(val status: String? = null)

data class BannerResponse(
    val server_time: String? = null,
    val banner: PromoItem? = null
)

data class WhatsNewFeature(
    val icon: String? = null,
    val title: String? = null,
    val description: String? = null
)

data class WhatsNewRelease(
    val id: Int = 0,
    val tab_label: String? = null,
    val badge_text: String? = null,
    val title: String? = null,
    val subtitle: String? = null,
    val features: List<WhatsNewFeature>? = null,
    val button1_text: String? = null,
    val button1_link: String? = null,
    val button1_style: String? = null, // google_play | light | dark
    val button2_text: String? = null,
    val button2_link: String? = null
)

data class WhatsNewResponse(
    val releases: List<WhatsNewRelease>? = null,
    val announce_version: Int? = null
)

private val Context.contentPrefsStore by preferencesDataStore(name = "content_prefs")

/** Stores the last What's New `announce_version` the user has dismissed. */
class WhatsNewPrefs(private val context: Context) {
    private val key = intPreferencesKey("whats_new_seen_version")

    suspend fun seenVersion(): Int? = context.contentPrefsStore.data.map { it[key] }.first()

    suspend fun markSeen(version: Int) {
        context.contentPrefsStore.edit { it[key] = version }
    }
}
