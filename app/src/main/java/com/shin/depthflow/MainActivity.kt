package com.shin.depthflow

import android.content.Intent
import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.shin.depthflow.push.PushTarget
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.shin.depthflow.ui.theme.DepthflowTheme
import com.shin.depthflow.util.BillingManager
import com.shin.depthflow.viewmodel.DepthFlowViewModel
import com.shin.depthflow.viewmodel.HappyHourViewModel
import com.shin.depthflow.viewmodel.ContentViewModel
import com.shin.depthflow.ui.navigation.Routes
import com.shin.depthflow.util.NotificationHelper
import com.shin.depthflow.util.GenerationNotifier

class MainActivity : ComponentActivity() {
    private lateinit var billingManager: BillingManager
    private val viewModel: DepthFlowViewModel by viewModels()
    private val happyHourViewModel: HappyHourViewModel by viewModels()
    private val contentViewModel: ContentViewModel by viewModels()

    // Set when the user taps a push (or our foreground notification); consumed by DepthFlowApp.
    private var pendingPush by mutableStateOf<PushTarget?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        billingManager = BillingManager(this) { purchaseToken, productId ->
            viewModel.verifyPurchaseOnBackend(purchaseToken, productId)
        }
        NotificationHelper.createNotificationChannel(this)
        pendingPush = PushTarget.from(intent)
        val startRoute = Routes.HOME

        // 3. Render the UI
        setContent {

            DepthflowTheme {
                DepthFlowApp(
                    viewModel = viewModel,
                    happyHourViewModel = happyHourViewModel,
                    contentViewModel = contentViewModel,
                    pendingPush = pendingPush,
                    onPushHandled = { pendingPush = null },
                    startDestination = startRoute, // <-- Pass the determined start route here
                    onInitiatePurchase = { planId, isYearly ->

                        // Map the UI selection to your specific Google Play Product IDs
                        val googlePlayProductId = when {
                            planId == "Pro Plan" && isYearly -> "sub_pro_yr"
                            planId == "Pro Plan" && !isYearly -> "sub_pro_mo"
                            planId == "Basic Plan" && isYearly -> "sub_basic_yr"
                            planId == "Basic Plan" && !isYearly -> "sub_basic_mo"
                            else -> return@DepthFlowApp // Failsafe
                        }

                        billingManager.launchPurchaseFlow(this@MainActivity, googlePlayProductId)
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        PushTarget.from(intent)?.let { pendingPush = it }
    }

    override fun onStart() {
        super.onStart()
        // In the app, Workspace shows the result itself; result notifications are only for background.
        GenerationNotifier.appInForeground = true
    }

    override fun onStop() {
        super.onStop()
        GenerationNotifier.appInForeground = false
    }

    override fun onResume() {
        super.onResume()
        // The server is the authority: refetch Happy Hour state and the unread badge on resume
        // (both are no-ops while logged out).
        happyHourViewModel.refresh()
        happyHourViewModel.refreshUnreadCount()
    }
}
