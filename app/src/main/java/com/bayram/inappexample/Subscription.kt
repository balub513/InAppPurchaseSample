package com.bayram.inappexample

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingFlowParams.ProductDetailsParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetailsResponseListener
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryProductDetailsParams.Product
import com.android.billingclient.api.QueryPurchasesParams

class Subscription : AppCompatActivity() {
    private var billingClient: BillingClient? = null

    var btn_premium: Button? = null
    var btn_restore: Button? = null
    var tv_status: TextView? = null
    private val purchaseItemIDs: ArrayList<String?> = object : ArrayList<String?>() {
        init {
            add(PRODUCT_PREMIUM)
            add(PRODUCT_MONTHLY)
        }
    }
    private val TAG = "iapSample"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subscription)
        billingClient = BillingClient.newBuilder(this)
            .enablePendingPurchases()
            .setListener { billingResult: BillingResult, list: List<Purchase>? ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && list != null) {
                    for (purchase in list) {
                        verifySubPurchase(purchase)
                    }
                }
            }.build()

        //start the connection after initializing the billing client
        establishConnection()
        init()
    }

    override fun onResume() {
        super.onResume()
        billingClient!!.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        ) { billingResult: BillingResult, list: List<Purchase> ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                for (purchase in list) {
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
                        verifySubPurchase(purchase)
                    }
                }
            }
        }
    }

    fun init() {
        btn_premium = this.findViewById(R.id.btn_premium)
        btn_restore = this.findViewById(R.id.btn_restore)
        tv_status = this.findViewById(R.id.tv_status)

        (btn_premium as Button).setOnClickListener(View.OnClickListener { GetSubPurchases() })

        (btn_restore as Button).setOnClickListener(View.OnClickListener { restorePurchases() })
    }

    fun establishConnection() {
        billingClient!!.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    // The BillingClient is ready. You can query purchases here.
                    //Use any of function below to get details upon successful connection
                    Log.d("TAG", "Connection Established")
                }
            }

            override fun onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                Log.d("TAG", "Connection NOT Established")
                establishConnection()
            }
        })
    }

    fun GetSubPurchases() {
        val productList = ArrayList<Product>()

        productList.add(
            Product.newBuilder()
                .setProductId(PRODUCT_PREMIUM)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()


        billingClient!!.queryProductDetailsAsync(params) { billingResult, list ->
            LaunchSubPurchase(list[0])
            Log.d(
                "TAG",
                "Product Price" + list[0].subscriptionOfferDetails!![0].pricingPhases.pricingPhaseList[0].formattedPrice
            )
        }
    }

    //Call this function using PRODUCT_PREMIUM or PRODUCT_MONTHLY as parameters.
    fun GetListsSubDetail(SKU: String) {
        val productList = ArrayList<Product>()

        //Set your In App Product ID in setProductId()
        for (ids in purchaseItemIDs) {
            ids?.let {
                Product.newBuilder()
                    .setProductId(it)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            }?.let {
                productList.add(
                    it
                )
            }
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient!!.queryProductDetailsAsync(
            params,
            ProductDetailsResponseListener { billingResult, list ->
                //                Log.d(TAG, "Total size is: " + list);
                for (li in list) {
                    if (li.productId.equals(SKU, ignoreCase = true) && SKU.equals(
                            PRODUCT_MONTHLY,
                            ignoreCase = true
                        )
                    ) {
                        LaunchSubPurchase(li)
                        Log.d(
                            TAG,
                            "Monthly Price is " + li.subscriptionOfferDetails!![0].pricingPhases.pricingPhaseList[0].formattedPrice
                        )
                        return@ProductDetailsResponseListener
                    } else if (li.productId.equals(SKU, ignoreCase = true) && SKU.equals(
                            PRODUCT_PREMIUM,
                            ignoreCase = true
                        )
                    ) {
                        LaunchSubPurchase(li)
                        Log.d(
                            TAG,
                            "Yearly Price is " + li.subscriptionOfferDetails!![0].pricingPhases.pricingPhaseList[0].formattedPrice
                        )
                        return@ProductDetailsResponseListener
                    }
                }
                //Do Anything that you want with requested product details
            })
    }

    fun LaunchSubPurchase(productDetails: ProductDetails) {
        assert(productDetails.subscriptionOfferDetails != null)
        val productList = ArrayList<ProductDetailsParams>()

        productList.add(
            ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(productDetails.subscriptionOfferDetails!![0].offerToken)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productList)
            .build()

        billingClient!!.launchBillingFlow(this, billingFlowParams)
    }

    fun verifySubPurchase(purchases: Purchase) {
        if (!purchases.isAcknowledged) {
            billingClient!!.acknowledgePurchase(
                AcknowledgePurchaseParams
                    .newBuilder()
                    .setPurchaseToken(purchases.purchaseToken)
                    .build()
            ) { billingResult: BillingResult ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    for (pur in purchases.products) {
                        if (pur.equals(PRODUCT_PREMIUM, ignoreCase = true)) {
                            Log.d("TAG", "Purchase is successful$pur")
                            tv_status!!.text = "Yay! Purchased"
                        }
                    }
                }
            }
        }
    }

    fun restorePurchases() {
        billingClient = BillingClient.newBuilder(this).enablePendingPurchases()
            .setListener { billingResult: BillingResult?, list: List<Purchase?>? -> }
            .build()
        val finalBillingClient = billingClient!!
        billingClient!!.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() {
            }

            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    finalBillingClient.queryPurchasesAsync(
                        QueryPurchasesParams.newBuilder()
                            .setProductType(BillingClient.ProductType.SUBS).build()
                    ) { billingResult1: BillingResult, list: List<Purchase> ->
                        if (billingResult1.responseCode == BillingClient.BillingResponseCode.OK) {
                            if (list.size > 0) {
                                for (i in list.indices) {
                                    if (list[i].products.contains(PRODUCT_PREMIUM)) {
                                        tv_status!!.text = "Premium Restored"
                                        Log.d(
                                            "TAG",
                                            "Product id " + PRODUCT_PREMIUM + " will restore here"
                                        )
                                    }
                                }
                            } else {
                                tv_status!!.text = "Nothing found to Restored"
                            }
                        }
                    }
                }
            }
        })
    }

    companion object {
        private const val PRODUCT_PREMIUM = "yearly"
        private const val PRODUCT_MONTHLY = "monthly"
    }
}