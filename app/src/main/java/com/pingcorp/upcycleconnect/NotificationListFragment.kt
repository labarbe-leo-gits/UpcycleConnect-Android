package com.pingcorp.upcycleconnect

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView

class NotificationListFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NotificationsAdapter
    private var isReadTab: Boolean = false

    companion object {
        fun newInstance(isRead: Boolean): NotificationListFragment {
            val fragment = NotificationListFragment()
            val args = Bundle()
            args.putBoolean("IS_READ", isRead)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isReadTab = arguments?.getBoolean("IS_READ") ?: false
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notification_list, container, false)
        recyclerView = view.findViewById(R.id.recyclerView)
        
        adapter = NotificationsAdapter(emptyList(), 
            onMarkRead = { (activity as? NotificationsActivity)?.markAsRead(it) },
            onDelete = { (activity as? NotificationsActivity)?.deleteNotification(it) }
        )
        recyclerView.adapter = adapter
        
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? NotificationsActivity)?.refreshUI()
    }

    fun updateNotifications(notifications: List<Notification>) {
        if (!isAdded) return
        val filtered = notifications.filter { it.read == isReadTab }
        adapter.updateData(filtered)
        val emptyState = view?.findViewById<View>(R.id.emptyState)
        emptyState?.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        if (filtered.isEmpty()) {
            emptyState?.findViewById<TextView>(R.id.emptyStateText)?.text = getString(R.string.no_notifications)
            emptyState?.findViewById<Button>(R.id.emptyStateButton)?.visibility = View.GONE
        }
    }
}
