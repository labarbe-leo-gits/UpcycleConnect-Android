package com.pingcorp.upcycleconnect

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import io.noties.markwon.Markwon
import kotlinx.coroutines.launch

class UpdocDetailActivity : BaseActivity() {
    private lateinit var sessionManager: SessionManager
    private lateinit var tvTitle: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvAiBadge: TextView
    private lateinit var stepsContainer: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var markwon: Markwon
    private var projectId: String? = null
    private var project: Project? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_updoc_detail)

        sessionManager = SessionManager(this)
        markwon = Markwon.create(this)

        tvTitle = findViewById(R.id.tvTitle)
        tvDescription = findViewById(R.id.tvDescription)
        tvAiBadge = findViewById(R.id.tvAiBadge)
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

        projectId = intent.getStringExtra("PROJECT_ID")
        val projectJson = intent.getStringExtra("PROJECT_JSON")
        
        if (projectJson != null) {
            try {
                project = RetrofitClient.json.decodeFromString<Project>(projectJson)
                project?.let {
                    projectId = it.id
                    displayProject(it)
                    loadSteps(it.id)
                }
            } catch (e: Exception) {
                Log.e("UpdocDetail", "Error decoding project", e)
            }
        } else if (projectId != null) {
            loadProject(projectId!!)
        } else {
            Toast.makeText(this, "Error: Project data missing", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadProject(id: String) {
        val token = sessionManager.getToken() ?: return
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.getProject(id, "Bearer $token")
                if (response.isSuccessful) {
                    project = response.body()
                    project?.let {
                        displayProject(it)
                        loadSteps(id)
                    }
                } else {
                    Toast.makeText(this@UpdocDetailActivity, "Failed to load project", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("UpdocDetail", "Error loading project", e)
                Toast.makeText(this@UpdocDetailActivity, "Error loading project", Toast.LENGTH_SHORT).show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun displayProject(project: Project) {
        tvTitle.text = project.title
        markwon.setMarkdown(tvDescription, project.description)
        tvAiBadge.visibility = if (project.aiGenerated == 1) View.VISIBLE else View.GONE
        supportActionBar?.title = project.title
        invalidateOptionsMenu()
    }

    private fun loadSteps(id: String) {
        val token = sessionManager.getToken() ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.getProjectSteps(id, "Bearer $token")
                if (response.isSuccessful) {
                    val steps = response.body() ?: emptyList()
                    displaySteps(steps)
                }
            } catch (e: Exception) {
                Log.e("UpdocDetail", "Error loading steps", e)
            }
        }
    }

    private fun displaySteps(steps: List<ProjectStep>) {
        stepsContainer.removeAllViews()
        if (steps.isEmpty()) {
            val tvNoSteps = TextView(this)
            tvNoSteps.text = getString(R.string.no_items_found)
            tvNoSteps.setPadding(0, 32, 0, 0)
            tvNoSteps.gravity = android.view.Gravity.CENTER
            tvNoSteps.alpha = 0.6f
            stepsContainer.addView(tvNoSteps)
            return
        }
        steps.sortedBy { it.stepOrder }.forEach { step ->
            val view = LayoutInflater.from(this).inflate(R.layout.item_step_detail, stepsContainer, false)
            view.findViewById<TextView>(R.id.tvStepOrder).text = step.stepOrder.toString()
            view.findViewById<TextView>(R.id.tvStepTitle).text = step.title
            
            val tvDesc = view.findViewById<TextView>(R.id.tvStepDescription)
            markwon.setMarkdown(tvDesc, step.description)

            val tvDuration = view.findViewById<TextView>(R.id.tvStepDuration)
            val duration = step.durationMin ?: 0
            if (duration > 0) {
                tvDuration.text = "${getString(R.string.step_duration_hint)}: $duration min"
                tvDuration.visibility = View.VISIBLE
            } else {
                tvDuration.visibility = View.GONE
            }
            stepsContainer.addView(view)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val userId = sessionManager.getUserId()
        if (project?.userId == userId) {
            menuInflater.inflate(R.menu.menu_updoc_detail, menu)
            return true
        }
        return false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_edit -> {
                val intent = Intent(this, UpdocEditorActivity::class.java)
                intent.putExtra("PROJECT_ID", projectId)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun getSelfNavDrawerItemId(): Int = -1
}