package com.maycode.neonrush.billing

import android.app.Activity
import android.util.Log
import com.android.billingclient.api.*

/**
 * Gestor de Google Play Billing.
 *
 * Productos que debes crear en Play Console (exactamente con estos IDs):
 * - coins_1000 (consumible)
 * - coins_5000 (consumible)
 * - gems_100 (consumible)
 * - remove_ads (no consumible)
 * - premium_monthly (suscripción)
 */
class BillingManager(
    private val activity: Activity,
    private val onPurchaseResult: (productId: String, success: Boolean) -> Unit
) : PurchasesUpdatedListener {

    companion object {
        private const val TAG = "BillingManager"

        val PRODUCT_IDS = listOf(
            "coins_1000",
            "coins_5000",
            "gems_100",
            "remove_ads",
            "premium_monthly"
        )
    }

    private var billingClient: BillingClient = BillingClient.newBuilder(activity)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    private val productDetailsMap = mutableMapOf<String, ProductDetails>()

    fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing connected")
                    queryProducts()
                    queryPurchases()
                } else {
                    Log.e(TAG, "Billing setup failed: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing disconnected")
            }
        })
    }

    private fun queryProducts() {
        val productList = PRODUCT_IDS.map { productId ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(
                    if (productId == "premium_monthly") BillingClient.ProductType.SUBS
                    else BillingClient.ProductType.INAPP
                )
                .build()
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                productDetailsList.forEach { details ->
                    productDetailsMap[details.productId] = details
                    Log.d(TAG, "Product found: ${details.productId}")
                }
            }
        }
    }

    private fun queryPurchases() {
        // Consultar compras existentes (para restaurar remove_ads / premium)
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                purchases.forEach { handlePurchase(it) }
            }
        }

        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                purchases.forEach { handlePurchase(it) }
            }
        }
    }

    fun launchPurchase(productId: String) {
        val productDetails = productDetailsMap[productId]
        if (productDetails == null) {
            Log.e(TAG, "Product not found: $productId. ¿Lo creaste en Play Console?")
            onPurchaseResult(productId, false)
            return
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .apply {
                    // Para suscripciones hay que elegir la oferta
                    productDetails.subscriptionOfferDetails?.firstOrNull()?.let { offer ->
                        setOfferToken(offer.offerToken)
                    }
                }
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            purchases.forEach { handlePurchase(it) }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.d(TAG, "User canceled")
        } else {
            Log.e(TAG, "Purchase error: ${billingResult.debugMessage}")
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            // Consumir los consumibles
            if (purchase.products.any { it.startsWith("coins_") || it.startsWith("gems_") }) {
                val consumeParams = ConsumeParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.consumeAsync(consumeParams) { _, _ -> }
            } else {
                // Acknowledge non-consumables / subscriptions
                if (!purchase.isAcknowledged) {
                    val acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    billingClient.acknowledgePurchase(acknowledgeParams) { }
                }
            }

            purchase.products.forEach { productId ->
                onPurchaseResult(productId, true)
            }
        }
    }

    fun endConnection() {
        billingClient.endConnection()
    }
}
