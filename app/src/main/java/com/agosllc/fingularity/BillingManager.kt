package com.agosllc.fingularity

import android.app.Activity
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams

class BillingManager(
    private val activity: Activity,
    private val onPrice: (String) -> Unit,
    private val onEntitlement: (Boolean) -> Unit,
    private val onPurchaseResult: (Boolean, String?) -> Unit
) : PurchasesUpdatedListener {

    companion object {
        const val PRODUCT_ID = "com.agosllc.fingularity.endless"
    }

    private var productDetails: ProductDetails? = null
    private var ready = false
    private var connecting = false
    private var queuedPurchase = false
    private var queuedRestore = false

    private val billingClient = BillingClient.newBuilder(activity)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .enableAutoServiceReconnection()
        .build()

    fun start() {
        connect()
    }

    fun destroy() {
        if (billingClient.isReady) billingClient.endConnection()
    }

    fun refreshEntitlement() {
        if (!ready) {
            connect()
            return
        }
        queryOwnedPurchases(reportRestoreResult = false)
    }

    fun purchase() {
        if (!ready) {
            queuedPurchase = true
            connect()
            return
        }
        val details = productDetails
        if (details == null) {
            queryProductDetails(after = { launchPurchaseIfReady() })
        } else {
            launchPurchase(details)
        }
    }

    fun restore() {
        if (!ready) {
            queuedRestore = true
            connect()
            return
        }
        queryOwnedPurchases(reportRestoreResult = true)
    }

    private fun connect() {
        if (ready || connecting) return
        connecting = true
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                connecting = false
                ready = result.responseCode == BillingResponseCode.OK
                if (!ready) {
                    onEntitlement(false)
                    if (queuedPurchase || queuedRestore) {
                        onPurchaseResult(false, friendlyBillingError(result))
                    }
                    queuedPurchase = false
                    queuedRestore = false
                    return
                }

                queryProductDetails()
                queryOwnedPurchases(reportRestoreResult = queuedRestore)
                if (queuedPurchase) {
                    queuedPurchase = false
                    queryProductDetails(after = { launchPurchaseIfReady() })
                }
                queuedRestore = false
            }

            override fun onBillingServiceDisconnected() {
                ready = false
            }
        })
    }

    private fun queryProductDetails(after: (() -> Unit)? = null) {
        if (!ready) {
            after?.invoke()
            return
        }
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PRODUCT_ID)
            .setProductType(ProductType.INAPP)
            .build()
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(product))
            .build()

        billingClient.queryProductDetailsAsync(params) { result, queryResult ->
            if (result.responseCode == BillingResponseCode.OK) {
                productDetails = queryResult.productDetailsList.firstOrNull { it.productId == PRODUCT_ID }
                productDetails?.let { details ->
                    val offer = details.oneTimePurchaseOfferDetailsList?.firstOrNull()
                        ?: details.oneTimePurchaseOfferDetails
                    offer?.formattedPrice?.takeIf { it.isNotBlank() }?.let(onPrice)
                }
            }
            after?.invoke()
        }
    }

    private fun launchPurchaseIfReady() {
        val details = productDetails
        if (details == null) {
            onPurchaseResult(
                false,
                "The Endless Collapse is not available from Google Play yet. Check the Play Console product setup and try again."
            )
            return
        }
        launchPurchase(details)
    }

    private fun launchPurchase(details: ProductDetails) {
        val offer = details.oneTimePurchaseOfferDetailsList?.firstOrNull()
            ?: details.oneTimePurchaseOfferDetails

        val productParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
        offer?.offerToken?.takeIf { it.isNotBlank() }?.let {
            productParamsBuilder.setOfferToken(it)
        }

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParamsBuilder.build()))
            .build()

        val result = billingClient.launchBillingFlow(activity, flowParams)
        if (result.responseCode != BillingResponseCode.OK) {
            onPurchaseResult(false, friendlyBillingError(result))
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingResponseCode.OK -> {
                val relevant = purchases.orEmpty().filter { PRODUCT_ID in it.products }
                if (relevant.isEmpty()) {
                    onPurchaseResult(false, "Purchase completed but the unlock item was not returned by Google Play.")
                    return
                }
                relevant.forEach { processPurchase(it, reportResult = true) }
            }
            BillingResponseCode.USER_CANCELED -> onPurchaseResult(false, "")
            BillingResponseCode.ITEM_ALREADY_OWNED -> queryOwnedPurchases(reportRestoreResult = true)
            else -> onPurchaseResult(false, friendlyBillingError(result))
        }
    }

    private fun queryOwnedPurchases(reportRestoreResult: Boolean) {
        if (!ready) return
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(ProductType.INAPP)
            .build()
        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode != BillingResponseCode.OK) {
                if (reportRestoreResult) onPurchaseResult(false, friendlyBillingError(result))
                return@queryPurchasesAsync
            }

            val relevant = purchases.filter { PRODUCT_ID in it.products }
            val purchased = relevant.firstOrNull { it.purchaseState == Purchase.PurchaseState.PURCHASED }
            val pending = relevant.firstOrNull { it.purchaseState == Purchase.PurchaseState.PENDING }

            when {
                purchased != null -> processPurchase(purchased, reportResult = reportRestoreResult)
                pending != null -> {
                    onEntitlement(false)
                    if (reportRestoreResult) {
                        onPurchaseResult(false, "Your purchase is still pending. The unlock will activate after Google Play confirms payment.")
                    }
                }
                else -> {
                    onEntitlement(false)
                    if (reportRestoreResult) {
                        onPurchaseResult(false, "No previous purchase was found for this Google Play account.")
                    }
                }
            }
        }
    }

    private fun processPurchase(purchase: Purchase, reportResult: Boolean) {
        if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            if (reportResult) {
                onPurchaseResult(false, "Waiting for Google Play to confirm payment. The unlock will activate automatically once it clears.")
            }
            return
        }
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return

        if (purchase.isAcknowledged) {
            onEntitlement(true)
            if (reportResult) onPurchaseResult(true, null)
            return
        }

        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.acknowledgePurchase(params) { result ->
            if (result.responseCode == BillingResponseCode.OK) {
                onEntitlement(true)
                if (reportResult) onPurchaseResult(true, null)
            } else if (reportResult) {
                onPurchaseResult(false, "Google Play could not finish confirming the purchase. Please use Restore Purchases in a moment.")
            }
        }
    }

    private fun friendlyBillingError(result: BillingResult): String {
        return when (result.responseCode) {
            BillingResponseCode.BILLING_UNAVAILABLE -> "Google Play Billing is unavailable on this device or account."
            BillingResponseCode.ITEM_UNAVAILABLE -> "The Endless Collapse is not available for this account or region yet."
            BillingResponseCode.NETWORK_ERROR, BillingResponseCode.SERVICE_UNAVAILABLE -> "Google Play could not be reached. Check your connection and try again."
            BillingResponseCode.SERVICE_DISCONNECTED -> "Google Play disconnected. Try again in a moment."
            BillingResponseCode.DEVELOPER_ERROR -> "Google Play rejected the product configuration. Check the Play Console product and test-track setup."
            else -> result.debugMessage.takeIf { it.isNotBlank() }
                ?: "Google Play could not complete the purchase."
        }
    }
}
