package com.pingcorp.upcycleconnect

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import coil.ImageLoader
import coil.load
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.decodeFromJsonElement
import java.io.OutputStream

class ConteneurItemDetailActivity : AppCompatActivity() {
    private lateinit var sessionManager: SessionManager
    private var item: ConteneurItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_conteneur_item_detail)

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

        val itemJson = intent.getStringExtra("ITEM_JSON")
        if (itemJson != null) {
            try {
                item = RetrofitClient.json.decodeFromString<ConteneurItem>(itemJson)
                item?.let {
                    displayItem(it)
                    fetchUserDetails(it.userId)
                    fetchContainerDetails(it.conteneurId)
                }
            } catch (e: Exception) {
                Log.e("ItemDetail", "Error decoding item", e)
                Toast.makeText(this, "Error loading item details", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            Toast.makeText(this, "No item data found", Toast.LENGTH_SHORT).show()
            finish()
        }

        setupButtons()
    }

    private fun displayItem(item: ConteneurItem) {
        findViewById<TextView>(R.id.tvItemNameTitle).text = item.objectName
        findViewById<TextView>(R.id.tvItemName).text = item.objectName
        findViewById<TextView>(R.id.tvDescription).text = item.objectDescription
        findViewById<TextView>(R.id.tvStatus).text = mapStatus(item.status)
        findViewById<TextView>(R.id.tvCreatedAt).text = item.createdAt

        val barcode = if (!item.barcode.isNullOrEmpty()) {
            item.barcode
        } else {
            val cleanId = item.id.replace("-", "")
            if (cleanId.length >= 16) {
                "UPC-${cleanId.substring(0, 16).uppercase()}"
            } else {
                item.barcode ?: ""
            }
        }
        
        findViewById<TextView>(R.id.tvBarcode).text = barcode
        findViewById<TextView>(R.id.tvBarcodeUnder).text = barcode
        findViewById<TextView>(R.id.tvRetrievalCode).text = item.retrievalCode

        val ivBarcode = findViewById<ImageView>(R.id.ivBarcode)
        if (barcode.isNotEmpty()) {
            val barcodeUrl = "https://bwipjs-api.metafloor.com/?bcid=code128&text=$barcode&includetext=false"
            ivBarcode.load(barcodeUrl) {
                placeholder(android.R.color.darker_gray)
                error(android.R.color.darker_gray)
            }
        }
        
        if (item.files.isNotEmpty()) {
            findViewById<TextView>(R.id.tvNoFiles).visibility = View.GONE
            val rvAttachments = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvAttachments)
            rvAttachments.visibility = View.VISIBLE
            findViewById<TextView>(R.id.tvNoFiles).visibility = View.VISIBLE
            findViewById<TextView>(R.id.tvNoFiles).text = item.files.joinToString { it.originalName }
        }
    }

    private fun fetchUserDetails(userId: String) {
        val token = sessionManager.getToken() ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.getUserProfile(userId, "Bearer $token")
                if (response.isSuccessful) {
                    val user = response.body()
                    user?.let {
                        findViewById<TextView>(R.id.tvUserName).text = "${it.firstName ?: ""} ${it.lastName ?: ""}".trim()
                        findViewById<TextView>(R.id.tvUserUsername).text = it.username
                        findViewById<TextView>(R.id.tvUserEmail).text = it.email
                    }
                }
            } catch (e: Exception) {
                Log.e("ItemDetail", "Error fetching user", e)
            }
        }
    }

    private fun fetchContainerDetails(containerId: String) {
        val token = sessionManager.getToken() ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.getContainer(containerId, "Bearer $token")
                if (response.isSuccessful) {
                    val element = response.body()
                    if (element != null) {
                        val container = RetrofitClient.json.decodeFromJsonElement<Container>(element)
                        findViewById<TextView>(R.id.tvContainerName).text = container.name
                        val address = "${container.number} ${container.road}, ${container.postalCode} ${container.city}"
                        findViewById<TextView>(R.id.tvAddress).text = address
                    }
                }
            } catch (e: Exception) {
                Log.e("ItemDetail", "Error fetching container", e)
            }
        }
    }

    private fun setupButtons() {
        findViewById<Button>(R.id.btnDownloadPng).setOnClickListener {
            downloadBarcode("png")
        }
        findViewById<Button>(R.id.btnDownloadPdf).setOnClickListener {
            downloadBarcode("pdf")
        }
        findViewById<Button>(R.id.btnCopy).setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Barcode", item?.barcode)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Barcode copied to clipboard", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btnDownloadZip).setOnClickListener {
            Toast.makeText(this, "Downloading ZIP...", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btnMarkRecovered).setOnClickListener {
            val itemId = item?.id ?: return@setOnClickListener
            val token = sessionManager.getToken() ?: return@setOnClickListener
            
            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.api.updateDepositStatus(
                        itemId,
                        StatusUpdate(status = 5),
                        "Bearer $token"
                    )
                    if (response.isSuccessful) {
                        setResult(RESULT_OK)
                        Toast.makeText(this@ConteneurItemDetailActivity, "Marked as recovered", Toast.LENGTH_SHORT).show()
                        
                        item?.id?.let { id ->
                            val token = sessionManager.getToken() ?: return@let
                            lifecycleScope.launch {
                                try {
                                    val itemResponse = RetrofitClient.api.getDeposit(id, "Bearer $token")
                                    if (itemResponse.isSuccessful) {
                                        val updatedItem = itemResponse.body()
                                        if (updatedItem != null) {
                                            item = updatedItem
                                            displayItem(updatedItem)
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("ItemDetail", "Error refetching item", e)
                                }
                            }
                        }
                    } else {
                        Toast.makeText(this@ConteneurItemDetailActivity, "Failed to update status", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("ItemDetail", "Error updating status", e)
                    Toast.makeText(this@ConteneurItemDetailActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        findViewById<Button>(R.id.btnClose).setOnClickListener {
            finish()
        }
    }

    private fun downloadBarcode(format: String) {
        val barcode = findViewById<TextView>(R.id.tvBarcode).text.toString()
        if (barcode.isEmpty()) return

        val barcodeUrl = "https://bwipjs-api.metafloor.com/?bcid=code128&text=$barcode&includetext=false"
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val loader = ImageLoader(this@ConteneurItemDetailActivity)
                val request = ImageRequest.Builder(this@ConteneurItemDetailActivity)
                    .data(barcodeUrl)
                    .allowHardware(false)
                    .build()
                
                val result = (loader.execute(request) as? SuccessResult)?.drawable
                val bitmap = (result as? android.graphics.drawable.BitmapDrawable)?.bitmap
                
                if (bitmap != null) {
                    if (format == "png") {
                        saveBitmapToGallery(bitmap, "barcode_$barcode.png")
                    } else {
                        saveBitmapToPdf(bitmap, "barcode_$barcode.pdf", barcode)
                    }
                }
            } catch (e: Exception) {
                Log.e("ItemDetail", "Error downloading barcode", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ConteneurItemDetailActivity, "Failed to download barcode", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun saveBitmapToGallery(bitmap: Bitmap, filename: String) {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val resolver = contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            resolver.openOutputStream(it).use { outputStream ->
                if (outputStream != null) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(it, contentValues, null, null)
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(this@ConteneurItemDetailActivity, "Barcode saved to Pictures", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun saveBitmapToPdf(bitmap: Bitmap, filename: String, barcode: String) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()

        paint.color = Color.BLACK
        paint.textSize = 24f
        paint.isFakeBoldText = true
        canvas.drawText("UpcycleConnect Barcode", 50f, 50f, paint)

        paint.textSize = 18f
        paint.isFakeBoldText = false
        canvas.drawText("Item: ${findViewById<TextView>(R.id.tvItemName).text}", 50f, 100f, paint)
        canvas.drawText("Barcode: $barcode", 50f, 130f, paint)

        val margin = 50f
        val imageWidth = 500f
        val imageHeight = (bitmap.height.toFloat() / bitmap.width.toFloat()) * imageWidth
        val destRect = android.graphics.RectF(margin, 180f, margin + imageWidth, 180f + imageHeight)
        canvas.drawBitmap(bitmap, null, destRect, null)

        pdfDocument.finishPage(page)

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
        }

        val resolver = contentResolver
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        } else {
            null
        }

        uri?.let {
            resolver.openOutputStream(it).use { outputStream ->
                if (outputStream != null) {
                    pdfDocument.writeTo(outputStream)
                }
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(this@ConteneurItemDetailActivity, "Barcode saved to Downloads", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@ConteneurItemDetailActivity, "PDF saving not fully supported on this Android version", Toast.LENGTH_SHORT).show()
            }
        }
        
        pdfDocument.close()
    }

    private fun mapStatus(status: Int): String {
        return when (status) {
            0, 1 -> "Pending"
            2 -> "Accepted"
            3 -> "Rejected"
            4 -> "Deposited"
            5 -> "Completed"
            else -> "Unknown"
        }
    }
}

@Serializable
data class StatusUpdate(val status: Int)
