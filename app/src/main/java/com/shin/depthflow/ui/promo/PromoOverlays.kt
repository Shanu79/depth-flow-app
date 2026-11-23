package com.shin.depthflow.ui.promo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.shin.depthflow.viewmodel.ContentViewModel
import kotlinx.coroutines.delay

/**
 * Hosts the home popup (the only admin overlay; What's New lives in the Notifications screen).
 * Shown on the Home screen only, about a second after the first content is up.
 *
 * @param bearer "Bearer <jwt>" or null when logged out (popups need a login)
 * @param onHome true while the Home route is visible
 * @param onInAppPath maps a web-app path such as "/pricing" to an app screen
 */
@Composable
fun PromoOverlays(
    viewModel: ContentViewModel,
    bearer: String?,
    onHome: Boolean,
    onInAppPath: (String) -> Unit
) {
    val context = LocalContext.current
    val popup by viewModel.popup.collectAsState()

    LaunchedEffect(bearer) { viewModel.loadHomePopup(bearer) }

    val current = popup
    var visibleId by remember { mutableStateOf<Int?>(null) }
    val canShow = current != null && onHome
    // Appear about a second after Home is on screen, then record the display (idempotent).
    LaunchedEffect(current?.id, canShow) {
        if (canShow && current != null) {
            delay(1000)
            visibleId = current.id
            viewModel.trackPopup(bearer, current.id)
        } else {
            visibleId = null
        }
    }
    if (current != null && canShow && visibleId == current.id) {
        HomePopupDialog(
            item = current,
            skewMillis = viewModel.skewMillis,
            onDismiss = viewModel::dismissPopup,
            onCta = {
                viewModel.dismissPopup()
                LinkRouter.handle(context, current.button_link, onInAppPath)
            }
        )
    }
}
