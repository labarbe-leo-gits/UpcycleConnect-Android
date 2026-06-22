package com.pingcorp.upcycleconnect

import android.content.Intent
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView

import androidx.activity.OnBackPressedCallback

open class BaseActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    protected lateinit var drawerLayout: DrawerLayout

    protected fun setupDrawer(toolbar: Toolbar) {
        drawerLayout = findViewById(R.id.drawer_layout)
        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)
        
        val selfId = getSelfNavDrawerItemId()
        if (selfId != -1) {
            navigationView.setCheckedItem(selfId)
        }

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.app_name, R.string.app_name
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (item.itemId == getSelfNavDrawerItemId()) {
            drawerLayout.closeDrawer(GravityCompat.START)
            return true
        }

        when (item.itemId) {
            R.id.nav_updoc -> {
                startActivity(Intent(this, UpdocActivity::class.java))
            }
//            R.id.nav_offers -> {
//                startActivity(Intent(this, OffersActivity::class.java))
//            }
            R.id.nav_profile -> {
                startActivity(Intent(this, ProfileActivity::class.java))
            }
            R.id.nav_containers -> {
                startActivity(Intent(this, ContainersActivity::class.java))
            }
            R.id.nav_notifications -> {
                startActivity(Intent(this, NotificationsActivity::class.java))
            }
        }

        if (shouldFinishOnNavigation()) {
            finish()
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    open fun getSelfNavDrawerItemId(): Int = -1
    open fun shouldFinishOnNavigation(): Boolean = false
}