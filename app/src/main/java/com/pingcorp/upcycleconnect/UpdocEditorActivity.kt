package com.pingcorp.upcycleconnect

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonPrimitive
import java.util.*

class UpdocEditorActivity : BaseActivity() {
    private lateinit var sessionManager: SessionManager
    private lateinit var stepsContainer: LinearLayout
    private val stepsList = mutableListOf<CreateProjectStepDto>()
    private lateinit var etTitle: TextInputEditText
    private lateinit var etDescription: TextInputEditText
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_updoc_editor)

        sessionManager = SessionManager(this)
        etTitle = findViewById(R.id.etTitle)
        etDescription = findViewById(R.id.etDescription)
        stepsContainer = findViewById(R.id.stepsContainer)
        progressBar = findViewById(R.id.progressBar)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<Button>(R.id.btnGenerateDescription).setOnClickListener { generateAi("generate_description") }
        findViewById<Button>(R.id.btnSuggestSteps).setOnClickListener { generateAi("suggest_steps") }
        findViewById<Button>(R.id.btnGenerateAll).setOnClickListener { generateAi("generate_all") }
        findViewById<Button>(R.id.btnAddStep).setOnClickListener { showStepDialog() }
        findViewById<Button>(R.id.btnSave).setOnClickListener { saveProject() }
    }

    private fun showStepDialog(step: CreateProjectStepDto? = null, index: Int = -1) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_step, null)
        val etStepTitle = dialogView.findViewById<TextInputEditText>(R.id.etStepTitle)
        val etStepDescription = dialogView.findViewById<TextInputEditText>(R.id.etStepDescription)
        val etStepDuration = dialogView.findViewById<TextInputEditText>(R.id.etStepDuration)

        step?.let {
            etStepTitle.setText(it.title)
            etStepDescription.setText(it.description)
            etStepDuration.setText(it.durationMin?.toString())
        }

        AlertDialog.Builder(this)
            .setTitle(if (step == null) getString(R.string.add_step_btn) else getString(R.string.edit_step_title))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.save_btn)) { _, _ ->
                val title = etStepTitle.text.toString()
                val description = etStepDescription.text.toString()
                val duration = etStepDuration.text.toString().toIntOrNull()

                if (title.isNotEmpty() && description.isNotEmpty()) {
                    val newStep = CreateProjectStepDto(
                        stepOrder = if (index != -1) index + 1 else stepsList.size + 1,
                        title = title,
                        description = description,
                        durationMin = duration
                    )
                    if (index != -1) {
                        stepsList[index] = newStep
                    } else {
                        stepsList.add(newStep)
                    }
                    refreshStepsUi()
                }
            }
            .setNegativeButton(getString(R.string.cancel_btn), null)
            .show()
    }

    private fun refreshStepsUi() {
        stepsContainer.removeAllViews()
        stepsList.forEachIndexed { index, step ->
            val view = LayoutInflater.from(this).inflate(R.layout.item_step_editor, stepsContainer, false)
            view.findViewById<TextView>(R.id.tvStepOrder).text = (index + 1).toString()
            view.findViewById<TextView>(R.id.tvStepTitle).text = step.title
            view.findViewById<TextView>(R.id.tvStepDescription).text = step.description
            view.findViewById<ImageButton>(R.id.btnRemoveStep).setOnClickListener {
                stepsList.removeAt(index)
                refreshStepsUi()
            }
            view.setOnClickListener { showStepDialog(step, index) }
            stepsContainer.addView(view)
        }
    }

    private fun generateAi(type: String) {
        val context = etTitle.text.toString()
        if (context.isEmpty()) {
            Toast.makeText(this, getString(R.string.enter_title_first), Toast.LENGTH_SHORT).show()
            return
        }

        val prompt = when (type) {
            "generate_all" -> """
                You are a helpful assistant for an upcycling platform.
                Given this project idea, generate two things:
                1. A clear and engaging project description (3-5 sentences).
                2. A numbered list of practical steps to complete the project.

                Use exactly this format and nothing else:
                DESCRIPTION:
                <your description here>

                STEPS:
                1. Step title - Step description
                2. Step title - Step description
                (etc.)

                Do not use emojis. Do not include comments or extra text.
                Project idea: $context
            """.trimIndent()
            "generate_description" -> """
                You are a helpful assistant for an upcycling platform.
                Given this brief idea for an upcycling project, write a clear and engaging project description (3-5 sentences). Respond with only the description text, no extra commentary.
                Do not use emojis. Do not include comments or extra text.

                Project idea: $context
            """.trimIndent()
            "suggest_steps" -> """
                You are a helpful assistant for an upcycling platform.
                Given this upcycling project description, suggest a list of clear, practical steps to complete the project.
                Format your response as a numbered list. Each step should have a short title followed by a dash and a brief description.
                Example format:
                1. Gather materials - Collect all items you will need.
                2. Prepare the surface - Clean and sand the object.

                Do not use emojis. Do not include comments or extra text.
                Project: ${etDescription.text.toString().ifEmpty { context }}
            """.trimIndent()
            else -> return
        }

        val fullPrompt = "$prompt\n\nReply with ONLY a JSON object: {\"response\": \"Your message here\"}"

        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.geminiApi.generateContent(
                    BuildConfig.GEMINI_API_KEY,
                    GeminiRequest(listOf(GeminiContent(listOf(GeminiPart(fullPrompt)))), GeminiConfig(temperature = 0.7f, maxOutputTokens = 1024))
                )

                if (response.isSuccessful) {
                    val candidate = response.body()?.candidates?.getOrNull(0)
                    val parts = candidate?.content?.parts ?: emptyList()
                    val rawText = parts.find { it.thought != true }?.text ?: parts.getOrNull(0)?.text ?: ""
                    val cleanText = parseGeminiJsonResponse(rawText)
                    
                    runOnUiThread {
                        when (type) {
                            "generate_description" -> etDescription.setText(cleanText)
                            "suggest_steps" -> parseAndAddSteps(cleanText)
                            "generate_all" -> {
                                val splitParts = cleanText.split("STEPS:")
                                if (splitParts.size == 2) {
                                    etDescription.setText(splitParts[0].replace("DESCRIPTION:", "").trim())
                                    parseAndAddSteps(splitParts[1].trim())
                                } else {
                                    etDescription.setText(cleanText)
                                }
                            }
                        }
                    }
                } else {
                    Toast.makeText(this@UpdocEditorActivity, "AI Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("UpdocEditor", "Gemini error", e)
                Toast.makeText(this@UpdocEditorActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun parseGeminiJsonResponse(text: String): String {
        return try {
            val startIndex = text.indexOf("{\"response\":")
            val result = if (startIndex != -1) {
                val lastIndex = text.lastIndexOf("}")
                val jsonStr = text.substring(startIndex, lastIndex + 1)
                val json = Json { ignoreUnknownKeys = true }.decodeFromString<JsonObject>(jsonStr)
                json["response"]?.jsonPrimitive?.content ?: text
            } else {
                text
            }
            cleanFinalText(result)
        } catch (e: Exception) {
            cleanFinalText(text)
        }
    }

    private fun cleanFinalText(text: String): String {
        return text.replace(Regex("[\\x{1F600}-\\x{1F64F}\\x{1F300}-\\x{1F5FF}\\x{1F680}-\\x{1F6FF}\\x{2600}-\\x{26FF}\\x{2700}-\\x{27BF}]"), "")
            .replace(Regex("//.*|/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")
            .trim()
    }

    private fun parseAndAddSteps(text: String) {
        val lines = text.split("\n")
        stepsList.clear()
        lines.forEach { line ->
            val match = Regex("""\d+\.\s*(.*?)\s*-\s*(.*)""").find(line)
            if (match != null) {
                val title = match.groupValues[1].trim()
                val description = match.groupValues[2].trim()
                stepsList.add(CreateProjectStepDto(stepsList.size + 1, title, description))
            }
        }
        refreshStepsUi()
    }

    private fun saveProject() {
        val title = etTitle.text.toString()
        val description = etDescription.text.toString()
        val token = sessionManager.getToken() ?: return
        val userId = sessionManager.getUserId() ?: return

        if (title.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, getString(R.string.title_desc_required), Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val isAi = detectAiContent(description)
                val projectDto = CreateProjectDto(
                    userId = userId,
                    title = title,
                    description = description,
                    aiGenerated = if (isAi) 1 else 0
                )
                val response = RetrofitClient.api.createProject(projectDto, "Bearer $token")

                if (response.isSuccessful) {
                    val project = response.body()
                    if (project != null) {
                        stepsList.forEach { step ->
                            RetrofitClient.api.createProjectStep(project.id, step, "Bearer $token")
                        }
                        Toast.makeText(this@UpdocEditorActivity, getString(R.string.project_saved_success), Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } else {
                    Toast.makeText(this@UpdocEditorActivity, getString(R.string.failed_save_project), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("UpdocEditor", "Save error", e)
                Toast.makeText(this@UpdocEditorActivity, "Error saving project", Toast.LENGTH_SHORT).show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private suspend fun detectAiContent(text: String): Boolean {
        val prompt = """
            You are an AI content detector. Analyze the following text and determine whether it was likely written by an AI (such as ChatGPT, Gemini, Claude, etc.) or by a human.
            Reply with ONLY a JSON object: {"ai_generated": true} or {"ai_generated": false}
            Include nothing else.

            Text:
            ${text.take(1024)}
        """.trimIndent()

        return try {
            val response = RetrofitClient.geminiApi.generateContent(
                BuildConfig.GEMINI_API_KEY,
                GeminiRequest(listOf(GeminiContent(listOf(GeminiPart(prompt)))), GeminiConfig(temperature = 0.0f, maxOutputTokens = 10))
            )
            if (response.isSuccessful) {
                val candidate = response.body()?.candidates?.getOrNull(0)
                val parts = candidate?.content?.parts ?: emptyList()
                val rawText = parts.find { it.thought != true }?.text ?: parts.getOrNull(0)?.text ?: ""

                val startIndex = rawText.indexOf("{")
                val lastIndex = rawText.lastIndexOf("}")
                if (startIndex != -1 && lastIndex != -1) {
                    val jsonStr = rawText.substring(startIndex, lastIndex + 1)
                    val json = Json { ignoreUnknownKeys = true }.decodeFromString<JsonObject>(jsonStr)
                    json["ai_generated"]?.jsonPrimitive?.boolean ?: false
                } else false
            } else false
        } catch (e: Exception) {
            false
        }
    }

    override fun getSelfNavDrawerItemId(): Int = -1
}
