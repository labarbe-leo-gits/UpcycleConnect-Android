package com.pingcorp.upcycleconnect

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import coil.load
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

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
            Toast.makeText(this, getString(R.string.no_product_selected), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadProductDetails()

        findViewById<Button>(R.id.payButton).setOnClickListener {
            startPaymentFlow()
        }

        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnContact).setOnClickListener {
            Toast.makeText(this, "Contacting seller...", Toast.LENGTH_SHORT).show()
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
                        displayProduct(annonce)
                        fetchSellerUsername(annonce.userId)
                    }
                } else {
                    Toast.makeText(this@CheckoutActivity, getString(R.string.failed_load_product), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("CheckoutActivity", "Error loading product", e)
            }
        }
    }

    private fun fetchSellerUsername(userId: String) {
        val token = sessionManager.getToken() ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.getUserProfile(userId, "Bearer $token")
                if (response.isSuccessful) {
                    val user = response.body()
                    if (user != null) {
                        setupDetailCard(R.id.cardSeller, "SELLER", user.username, android.R.drawable.ic_menu_myplaces)
                    }
                }
            } catch (e: Exception) {
                Log.e("CheckoutActivity", "Error fetching seller profile", e)
            }
        }
    }

    private fun displayProduct(annonce: Annonce) {
        findViewById<TextView>(R.id.productName).text = annonce.title
        findViewById<TextView>(R.id.productDescription).text = annonce.description ?: "No description"
        
        val price = annonce.price ?: 0.0
        findViewById<TextView>(R.id.productPriceTTC).text = String.format(Locale.getDefault(), "€ %.2f", price)
        
        val imageUrl = "http://10.0.2.2:8081/assets/img/defaults/placeholder.png"
        findViewById<ImageView>(R.id.productImage).load(imageUrl)

        setupDetailCard(R.id.cardSeller, "SELLER", annonce.sellerUsername ?: "Loading...", android.R.drawable.ic_menu_myplaces)
        setupDetailCard(R.id.cardRating, "SELLER RATING", if (annonce.sellerRating != null) "${annonce.sellerRating}/5" else "No reviews yet", android.R.drawable.ic_menu_agenda)
        
        val formattedDate = formatIsoDate(annonce.createdAt)
        setupDetailCard(R.id.cardPosted, "POSTED", formattedDate, android.R.drawable.ic_menu_today)
        
        val condition = annonce.itemStateLabel ?: mapItemState(annonce.itemState)
        setupDetailCard(R.id.cardCondition, "CONDITION", condition, android.R.drawable.ic_menu_search)
        
        setupDetailCard(R.id.cardScore, "UPCYCLING SCORE", String.format(Locale.getDefault(), "%.2f", annonce.upcyclingScore ?: 0.0), android.R.drawable.ic_menu_send)
        setupDetailCard(R.id.cardViews, "VIEWS", annonce.viewCount.toString(), android.R.drawable.ic_menu_view)

        val commission = price * 0.05
        val stripeFee = (price * 0.029) + 0.3
        val sellerReceives = price - commission - stripeFee
        findViewById<TextView>(R.id.priceBreakdown).text = String.format(
            Locale.getDefault(),
            "Seller receives: € %.2f (HT) · Commission: € %.2f · Stripe: € %.2f",
            sellerReceives, commission, stripeFee
        )
    }

    private fun formatIsoDate(isoString: String?): String {
        if (isoString == null) return "Unknown"
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(isoString)
            val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            date?.let { outputFormat.format(it) } ?: "Unknown"
        } catch (unused: Exception) {
            isoString.substringBefore("T")
        }
    }

    private fun mapItemState(state: Int): String {
        return when (state) {
            0 -> "New"
            1 -> "Like New"
            2 -> "Good"
            3 -> "Fair"
            4 -> "Poor"
            else -> "Unknown"
        }
    }

    private fun setupDetailCard(includeId: Int, label: String, value: String, iconRes: Int) {
        val view = findViewById<View>(includeId)
        view.findViewById<TextView>(R.id.cardLabel).text = label
        view.findViewById<TextView>(R.id.cardValue).text = value
        view.findViewById<ImageView>(R.id.cardIcon).setImageResource(iconRes)
    }

    private fun startPaymentFlow() {
        val token = sessionManager.getToken() ?: return
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val request = PaymentIntentRequest(productUuid!!, token)
                val response = RetrofitClient.phpApi.createPaymentIntent(request, "Bearer $token")
                progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        paymentIntentId = body.clientSecret.substringBefore("_secret")
                        paymentSheet.presentWithPaymentIntent(body.clientSecret)
                    }
                } else {
                    val errorMsg = response.errorBody()?.string() ?: getString(R.string.failed_initiate_payment)
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
                Toast.makeText(this, getString(R.string.payment_failed_prefix, paymentSheetResult.error.message), Toast.LENGTH_LONG).show()
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
                val request = VerifyPaymentRequest(piId, product.id, token)
                val verifyResponse = RetrofitClient.phpApi.verifyPayment(
                    request, "Bearer $token"
                )

                if (verifyResponse.isSuccessful && verifyResponse.body()?.success == true) {
                    val order = Order(
                        userId = userId,
                        productId = product.id,
                        amount = product.price ?: 0.0,
                        transactionId = piId,
                        status = 1
                    )
                    val orderResponse = RetrofitClient.api.createOrder(order, "Bearer $token")

                    if (orderResponse.isSuccessful) {
                        RetrofitClient.api.updateAnnonceStatus(
                            product.id,
                            UpdateAnnonceStatusDto(status = 1),
                            "Bearer $token"
                        )
                        
                        Toast.makeText(this@CheckoutActivity, getString(R.string.purchase_successful), Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        Toast.makeText(this@CheckoutActivity, getString(R.string.failed_create_order), Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@CheckoutActivity, getString(R.string.payment_verification_failed), Toast.LENGTH_SHORT).show()
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
