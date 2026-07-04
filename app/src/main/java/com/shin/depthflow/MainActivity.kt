package com.shin.depthflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.shin.depthflow.ui.theme.DepthflowTheme
import com.shin.depthflow.util.BillingManager
import com.shin.depthflow.viewmodel.DepthFlowViewModel
import com.shin.depthflow.ui.navigation.Routes
import com.shin.depthflow.util.NotificationHelper

class MainActivity : ComponentActivity() {
    private lateinit var billingManager: BillingManager
    private val viewModel: DepthFlowViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        billingManager = BillingManager(this) { purchaseToken, productId ->
            viewModel.verifyPurchaseOnBackend(purchaseToken, productId)
        }
        NotificationHelper.createNotificationChannel(this)
        val startRoute = Routes.HOME

        // 3. Render the UI
        setContent {

            DepthflowTheme {
                DepthFlowApp(
                    viewModel = viewModel,
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
}







