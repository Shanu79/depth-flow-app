package com.shin.depthflow.util

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.MutableStateFlow

class BillingManager(
    private val context: Context,
    private val onPurchaseVerified: (purchaseToken: String, productId: String) -> Unit
) {

    private val _isBillingClientReady = MutableStateFlow(false)

    // 1. Initialize the Client with the required listener and pending purchase params
    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            handlePurchases(purchases)
        }
    }

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        // Required by Play Billing to support pending transactions (like cash or delayed bank transfers)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    init {
        connectToPlayBilling()
    }

    // 2. Connect to Google Play Services
    private fun connectToPlayBilling() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _isBillingClientReady.value = true
                }
            }
            override fun onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
            }
        })
    }

    // 3. Launch the Purchase Flow
    fun launchPurchaseFlow(activity: Activity, productId: String) {

        Logger.d("BillingTest", "3. Inside BillingManager querying: $productId")

        if (!billingClient.isReady) {
            Logger.e("BillingTest", "ERROR: BillingClient is NOT ready.")
            activity.runOnUiThread {
                android.widget.Toast.makeText(activity, "Google Play Services not ready. Try again.", android.widget.Toast.LENGTH_SHORT).show()
            }
            connectToPlayBilling()
            return
        }

        val queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            ).build()

        billingClient.queryProductDetailsAsync(queryProductDetailsParams) { billingResult, queryProductDetailsResult ->

            Logger.d("BillingTest", "4. Query Response Code: ${billingResult.responseCode}")

            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val productDetailsList = queryProductDetailsResult.productDetailsList

                if (productDetailsList != null && productDetailsList.isNotEmpty()) {
                    val productDetails = productDetailsList[0]
                    
                    // Safely extract the first available offer token
                    val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: ""

                    Logger.d("BillingTest", "5. Product found: ${productDetails.name}. OfferToken length: ${offerToken.length}")

                    val productDetailsParamsList = listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(productDetails)
                            .setOfferToken(offerToken)
                            .build()
                    )

                    val billingFlowParams = BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(productDetailsParamsList)
                        .build()

                    billingClient.launchBillingFlow(activity, billingFlowParams)
                } else {
                    Logger.e("BillingTest", "ERROR: Product list is empty for $productId. This usually means the ID is incorrect in Play Console or the product is not 'Active'.")
                    activity.runOnUiThread {
                        android.widget.Toast.makeText(activity, "Error: Product '$productId' not found in Play Console.", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                Logger.e("BillingTest", "ERROR: Query failed. Code: ${billingResult.responseCode}, Msg: ${billingResult.debugMessage}")
                activity.runOnUiThread {
                    android.widget.Toast.makeText(activity, "Billing Error: ${billingResult.debugMessage}", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // 4. Handle the Success & Acknowledge
    private fun handlePurchases(purchases: List<Purchase>) {
        for (purchase in purchases) {
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                // All purchases must be acknowledged within 3 days, or Google refunds the user automatically
                if (!purchase.isAcknowledged) {
                    val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()

                    billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            // Purchase is fully complete!
                            // Notify the backend to verify and grant credits
                            val productId = purchase.products.firstOrNull() ?: ""
                            onPurchaseVerified(purchase.purchaseToken, productId)
                        }
                    }
                }
            }
        }
    }
}








