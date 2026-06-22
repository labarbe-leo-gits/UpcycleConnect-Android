package com.pingcorp.upcycleconnect

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch

class NotificationsActivity : BaseActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var sessionManager: SessionManager
    private val fragments = mutableMapOf<Int, NotificationListFragment>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notifications)

        sessionManager = SessionManager(this)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        setupDrawer(toolbar)
        supportActionBar?.title = getString(R.string.notifications_title)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)

        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 2
            override fun createFragment(position: Int): androidx.fragment.app.Fragment {
                val fragment = NotificationListFragment.newInstance(position == 1)
                fragments[position] = fragment
                return fragment
            }
        }

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = if (position == 0) getString(R.string.unread_tab) else getString(R.string.read_tab)
        }.attach()

        findViewById<ExtendedFloatingActionButton>(R.id.markAllReadFab).setOnClickListener {
            markAllAsRead()
        }

        findViewById<ExtendedFloatingActionButton>(R.id.clearTabFab).setOnClickListener {
            clearCurrentTab()
        }

        fetchNotifications()
    }

    private var allNotifications: List<Notification> = emptyList()

    private fun fetchNotifications() {
        val token = sessionManager.getToken() ?: return
        val userId = sessionManager.getUserId() ?: return

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.getUserNotifications(userId, "Bearer $token")
                if (response.isSuccessful) {
                    allNotifications = response.body() ?: emptyList()
                    updateFragments()
                } else {
                    Toast.makeText(this@NotificationsActivity, R.string.error_fetching_notifications, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("Notifications", "Error", e)
                Toast.makeText(this@NotificationsActivity, R.string.network_error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun refreshUI() {
        updateFragments()
    }

    private fun updateFragments() {
        fragments.values.forEach { it.updateNotifications(allNotifications) }
    }

    fun markAsRead(notification: Notification) {
        val token = sessionManager.getToken() ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.markNotificationAsRead(notification.id, "Bearer $token")
                if (response.isSuccessful) {
                    fetchNotifications()
                }
            } catch (e: Exception) {
                Log.e("Notifications", "Error marking as read", e)
            }
        }
    }

    fun deleteNotification(notification: Notification) {
        val token = sessionManager.getToken() ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.deleteNotification(notification.id, "Bearer $token")
                if (response.isSuccessful) {
                    fetchNotifications()
                }
            } catch (e: Exception) {
                Log.e("Notifications", "Error deleting", e)
            }
        }
    }

    private fun markAllAsRead() {
        val token = sessionManager.getToken() ?: return
        val userId = sessionManager.getUserId() ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.markAllNotificationsAsRead(userId, "Bearer $token")
                if (response.isSuccessful) {
                    fetchNotifications()
                }
            } catch (e: Exception) {
                Log.e("Notifications", "Error marking all as read", e)
            }
        }
    }

    private fun clearCurrentTab() {
        val isReadTab = viewPager.currentItem == 1
        val toDelete = allNotifications.filter { it.read == isReadTab }
        if (toDelete.isEmpty()) return

        val token = sessionManager.getToken() ?: return
        lifecycleScope.launch {
            try {
                toDelete.forEach {
                    RetrofitClient.api.deleteNotification(it.id, "Bearer $token")
                }
                fetchNotifications()
            } catch (e: Exception) {
                Log.e("Notifications", "Error clearing tab", e)
            }
        }
    }

    override fun getSelfNavDrawerItemId(): Int = R.id.nav_notifications
}
