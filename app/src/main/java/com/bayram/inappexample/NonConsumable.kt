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
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryProductDetailsParams.Product
import com.android.billingclient.api.QueryPurchasesParams

class NonConsumable : AppCompatActivity() {
    private val PRODUCT_PREMIUM = "lifetime"
    private val NoAds = "NoAds"
    private val purchaseItemIDs: ArrayList<String?> = object : ArrayList<String?>() {
        init {
            add(PRODUCT_PREMIUM)
            add(NoAds)
        }
    }

    private val TAG = "iapSample"

    private var billingClient: BillingClient? = null

    var btn_premium: Button? = null
    var btn_restore: Button? = null
    var tv_status: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_non_consumable)

        billingClient = BillingClient.newBuilder(this)
            .enablePendingPurchases()
            .setListener { billingResult: BillingResult, list: List<Purchase>? ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && list != null) {
                    for (purchase in list) {
                        Log.d(TAG, "Response is OK")
                        handlePurchase(purchase)
                    }
                } else {
                    Log.d(TAG, "Response NOT OK")
                }
            }.build()

        //start the connection after initializing the billing client
        establishConnection()
        init()
    }

    fun init() {
        btn_premium = this.findViewById(R.id.btn_premium)
        btn_restore = this.findViewById(R.id.btn_restore)
        tv_status = this.findViewById(R.id.tv_status)

        (btn_premium as Button).setOnClickListener(View.OnClickListener { GetSingleInAppDetail() })

        (btn_restore as Button).setOnClickListener(View.OnClickListener { restorePurchases() })
    }


    fun establishConnection() {
        billingClient!!.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    // The BillingClient is ready. You can query purchases here.

                    //Use any of function below to get details upon successful connection

                    // GetSingleInAppDetail();
                    //GetListsInAppDetail();

                    Log.d(TAG, "Connection Established")
                }
            }

            override fun onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                Log.d(TAG, "Connection NOT Established")
                establishConnection()
            }
        })
    }

    /*
     *
     * The official examples use an ImmutableList for some reason to build the query,
     * but you don't actually need to use that.
     * The setProductList method just takes List<Product> as its input, it does not require ImmutableList.
     *
     * */
    /*
     * If you have API < 24, you could just make an ArrayList instead.
     * */
    fun GetSingleInAppDetail() {
        val productList = ArrayList<Product>()

        //Set your In App Product ID in setProductId()
        productList.add(
            Product.newBuilder()
                .setProductId(PRODUCT_PREMIUM)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient!!.queryProductDetailsAsync(params) { billingResult, list -> //Do Anything that you want with requested product details
            //Calling this function here so that once products are verified we can start the purchase behavior.
            //You can save this detail in separate variable or list to call them from any other location
            //Create another function if you want to call this in establish connections' success state
            LaunchPurchaseFlow(list[0])
        }
    }


    fun GetListsInAppDetail() {
        val productList = ArrayList<Product>()

        //Set your In App Product ID in setProductId()
        for (ids in purchaseItemIDs) {
            ids?.let {
                Product.newBuilder()
                    .setProductId(it)
                    .setProductType(BillingClient.ProductType.INAPP)
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

        billingClient!!.queryProductDetailsAsync(params) { billingResult, list ->
            for (li in list) {
                Log.d(
                    TAG, "IN APP item Price" + li.oneTimePurchaseOfferDetails!!
                        .formattedPrice
                )
            }
            //Do Anything that you want with requested product details
        }
    }

    fun LaunchPurchaseFlow(productDetails: ProductDetails?) {
        val productList = ArrayList<ProductDetailsParams>()

        productList.add(
            ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails!!)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productList)
            .build()

        billingClient!!.launchBillingFlow(this, billingFlowParams)
    }

    fun handlePurchase(purchases: Purchase) {
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

    override fun onResume() {
        super.onResume()
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
                            .setProductType(BillingClient.ProductType.INAPP).build()
                    ) { billingResult1: BillingResult, list: List<Purchase> ->
                        if (billingResult1.responseCode == BillingClient.BillingResponseCode.OK) {
                            if (list.size > 0) {
                                Log.d("TAG", "IN APP SUCCESS RESTORE: $list")
                                for (i in list.indices) {
                                    if (list[i].products.contains(PRODUCT_PREMIUM)) {
                                        tv_status!!.text = "Premium Restored"
                                        Log.d(
                                            "TAG",
                                            "Product id $PRODUCT_PREMIUM will restore here"
                                        )
                                    }
                                }
                            } else {
                                tv_status!!.text = "Nothing found to Restored"
                                Log.d("TAG", "In APP Not Found To Restore")
                            }
                        }
                    }
                }
            }
        })
    }
}