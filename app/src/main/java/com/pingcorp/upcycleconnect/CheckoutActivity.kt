package com.pingcorp.upcycleconnect

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import kotlinx.coroutines.launch

class CheckoutActivity : BaseActivity() {
    private lateinit var paymentSheet: PaymentSheet
    private lateinit var sessionManager: SessionManager
    private var productUuid: String? = null
    private var currentAnnonce: Annonce? = null
    private var paymentIntentId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_checkout)

        PaymentConfiguration.init(applicationContext, BuildConfig.STRIPE_PUBLISHABLE_KEY)
        paymentSheet = PaymentSheet(this, ::onPaymentSheetResult)
        sessionManager = SessionManager(this)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        productUuid = intent.getStringExtra("PRODUCT_UUID")
        if (productUuid == null) {
            Toast.makeText(this, "No product selected", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadProductDetails()

        findViewById<Button>(R.id.payButton).setOnClickListener {
            startPaymentFlow()
        }
    }

    private fun loadProductDetails() {
        val token = sessionManager.getToken() ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.getAnnonce(productUuid!!, "Bearer $token")
                if (response.isSuccessful) {
                    val annonce = response.body()
                    if (annonce != null) {
                        currentAnnonce = annonce
                        findViewById<TextView>(R.id.productName).text = annonce.title
                        findViewById<TextView>(R.id.productPrice).text = if (annonce.price != null) "${annonce.price} €" else "Free"
                    }
                } else {
                    Toast.makeText(this@CheckoutActivity, "Failed to load product", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("CheckoutActivity", "Error loading product", e)
            }
        }
    }

    private fun startPaymentFlow() {
        val token = sessionManager.getToken() ?: return
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.createPaymentIntent(PaymentIntentRequest(productUuid!!), "Bearer $token")
                progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        // Extract payment intent ID from client secret (it's the prefix before _secret_)
                        paymentIntentId = body.clientSecret.substringBefore("_secret")
                        paymentSheet.presentWithPaymentIntent(body.clientSecret)
                    }
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Failed to initiate payment"
                    Toast.makeText(this@CheckoutActivity, errorMsg, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                Log.e("CheckoutActivity", "Error starting payment flow", e)
                Toast.makeText(this@CheckoutActivity, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun onPaymentSheetResult(paymentSheetResult: PaymentSheetResult) {
        when (paymentSheetResult) {
            is PaymentSheetResult.Canceled -> {
                Log.d("CheckoutActivity", "Payment Canceled")
            }
            is PaymentSheetResult.Failed -> {
                Log.e("CheckoutActivity", "Payment Failed", paymentSheetResult.error)
                Toast.makeText(this, "Payment Failed: ${paymentSheetResult.error.message}", Toast.LENGTH_LONG).show()
            }
            is PaymentSheetResult.Completed -> {
                Log.d("CheckoutActivity", "Payment Completed")
                finalizePurchase()
            }
        }
    }

    private fun finalizePurchase() {
        val token = sessionManager.getToken() ?: return
        val userId = sessionManager.getUserId() ?: return
        val product = currentAnnonce ?: return
        val piId = paymentIntentId ?: "unknown"

        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                // 1. Verify payment
                val verifyResponse = RetrofitClient.api.verifyPayment(
                    VerifyPaymentRequest(piId, product.id),
                    "Bearer $token"
                )

                if (verifyResponse.isSuccessful && verifyResponse.body()?.success == true) {
                    // 2. Create order
                    val order = Order(
                        userId = userId,
                        productId = product.id,
                        amount = product.price ?: 0.0,
                        transactionId = piId,
                        status = 1
                    )
                    val orderResponse = RetrofitClient.api.createOrder(order, "Bearer $token")

                    if (orderResponse.isSuccessful) {
                        // 3. Mark offer sold
                        RetrofitClient.api.updateAnnonceStatus(
                            product.id,
                            UpdateAnnonceStatusDto(status = 1),
                            "Bearer $token"
                        )
                        
                        Toast.makeText(this@CheckoutActivity, "Purchase successful!", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        Toast.makeText(this@CheckoutActivity, "Failed to create order record", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@CheckoutActivity, "Payment verification failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("CheckoutActivity", "Error finalizing purchase", e)
                Toast.makeText(this@CheckoutActivity, "Error finalizing purchase", Toast.LENGTH_SHORT).show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    override fun getSelfNavDrawerItemId(): Int = -1
}
