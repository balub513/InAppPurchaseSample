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
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryProductDetailsParams.Product
import com.android.billingclient.api.QueryPurchasesParams

class Consumable : AppCompatActivity() {
    private val PRODUCT_PREMIUM = "lifetime"
    private val ConsumeProductID = "consumabletest"
    private val purchaseItemIDs: ArrayList<String?> = object : ArrayList<String?>() {
        init {
            add(PRODUCT_PREMIUM)
            add(ConsumeProductID)
        }
    }

    private val TAG = "iapSample"

    private var billingClient: BillingClient? = null
    private var tries = 1
    private var maxTries = 3

    private val connectionAttempts = 0

    var isConnectionEstablished: Boolean = false
    var btn_premium: Button? = null
    var btn_restore: Button? = null
    var tv_status: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_consumable)

        billingClient = BillingClient.newBuilder(this)
            .enablePendingPurchases()
            .setListener { billingResult: BillingResult, list: List<Purchase>? ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && list != null) {
                    for (purchase in list) {
                        // TODO: we will send order id to the backend before handle purchase

                        val orderId = purchase.orderId
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


    private fun establishConnection() {
        billingClient!!.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.e("err", billingResult.responseCode.toString())
                    Log.e("err", "baglamtı başarılı")

                    // Bağlantı başarıyla kuruldu
                    // The BillingClient is ready. You can query purchases here.
                    //Use any of function below to get details upon successful connection
                    // GetSingleInAppDetail();
                    //GetListsInAppDetail();
                } else {
                    errorControl(billingResult.responseCode)
                }
            }

            override fun onBillingServiceDisconnected() {
                // Bağlantı kesildiğinde yeniden bağlanmak için yeniden bağlanmayı deneyin
                // TODO: 11.04.2023 alttaki yerine retryBillingServiceConnection() eklenecek ve 3 deneme olacak
                retryBillingServiceConnection()
            }
        })
    }

    fun errorControl(response: Int) {
        if (response == BillingClient.BillingResponseCode.BILLING_UNAVAILABLE || response == BillingClient.BillingResponseCode.DEVELOPER_ERROR || response == BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED || response == BillingClient.BillingResponseCode.USER_CANCELED) {
            // non retriable responses
            handleBillingError(response)
        } else {
            // Bağlantı başarısız oldu 3 kere tekrar dene
            retryBillingServiceConnection()
        }
    }

    fun retryBillingServiceConnection() {
        tries = 1
        maxTries = 3
        isConnectionEstablished = false
        do {
            try {
                billingClient!!.startConnection(object : BillingClientStateListener {
                    override fun onBillingServiceDisconnected() {
                        retryBillingServiceConnection()
                    }

                    override fun onBillingSetupFinished(billingResult: BillingResult) {
                        tries++
                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            isConnectionEstablished = true
                        } else if (tries == maxTries) {
                            handleBillingError(billingResult.responseCode)
                        }
                    }
                })
            } catch (e: Exception) {
                tries++
            }
        } while (tries <= maxTries && !isConnectionEstablished)

        if (isConnectionEstablished == false) {
            handleBillingError(-1)
        }
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
                .setProductId(ConsumeProductID)
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

    //This function will be called in handlepurchase() after success of any consumeable purchase
    fun ConsumePurchase(purchase: Purchase) {
        val params = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient!!.consumeAsync(params) { billingResult, s ->
            Log.d("TAG", "Consuming Successful: $s")
            tv_status!!.text = "Product Consumed"
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

                /*       ConsumeResponseListener listener = new ConsumeResponseListener() {
                    @Override
                    public void onConsumeResponse(@NonNull BillingResult billingResult, @NonNull String purchaseToken) {
                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                            // Handle the success of the consume operation.
                        }
                    }
                }; */
                /*      ConsumeParams consumeParams =
                        ConsumeParams.newBuilder()
                                .setPurchaseToken(purchases.getPurchaseToken())
                                .build();
                                *
           */
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    for (pur in purchases.products) {
                        if (pur.equals(ConsumeProductID, ignoreCase = true)) {
                            Log.d("TAG", "Purchase is successful")
                            tv_status!!.text = "Yay! Purchased"


                            //Calling Consume to consume the current purchase
                            // so user will be able to buy same product again
                            //   billingClient.consumeAsync(consumeParams, listener); // you should have listener method !


                            // TODO: 11.04.2023 bu kısım backend tarafında yapılacak
                            ConsumePurchase(purchases)
                        }
                    }
                } else {
                    handleBillingError(billingResult.responseCode)
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
                Log.e("erorr", "geldi")
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
                } else {
                    handleBillingError(billingResult.responseCode)
                }
            }
        })
    }


    private fun handleBillingError(responseCode: Int) {
        var errorMessage = ""
        errorMessage = when (responseCode) {
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> "Billing service is currently unavailable. Please try again later."
            BillingClient.BillingResponseCode.DEVELOPER_ERROR -> "An error occurred while processing the request. Please try again later."
            BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED -> "This feature is not supported on your device."
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> "You already own this item."
            BillingClient.BillingResponseCode.ITEM_NOT_OWNED -> "You do not own this item."
            BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> "This item is not available for purchase."
            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> "Billing service has been disconnected. Please try again later."
            BillingClient.BillingResponseCode.SERVICE_TIMEOUT -> "Billing service timed out. Please try again later."
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> "Billing service is currently unavailable. Please try again later."
            BillingClient.BillingResponseCode.USER_CANCELED -> "The purchase has been canceled."
            else -> "An unknown error occurred."
        }
        Log.e("BillingError", errorMessage)
    }
}