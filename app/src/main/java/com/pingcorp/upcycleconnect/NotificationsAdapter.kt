package com.pingcorp.upcycleconnect

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NotificationsAdapter(
    private var notifications: List<Notification>,
    private val onMarkRead: (Notification) -> Unit,
    private val onDelete: (Notification) -> Unit
) : RecyclerView.Adapter<NotificationsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val message: TextView = view.findViewById(R.id.notificationMessage)
        val date: TextView = view.findViewById(R.id.notificationDate)
        val markReadBtn: Button = view.findViewById(R.id.markReadBtn)
        val deleteBtn: ImageButton = view.findViewById(R.id.deleteBtn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notification = notifications[position]
        holder.message.text = notification.message
        holder.date.text = notification.createdAt

        if (notification.read) {
            holder.markReadBtn.visibility = View.GONE
        } else {
            holder.markReadBtn.visibility = View.VISIBLE
            holder.markReadBtn.setOnClickListener { onMarkRead(notification) }
        }

        holder.deleteBtn.setOnClickListener { onDelete(notification) }
    }

    override fun getItemCount() = notifications.size

    fun updateData(newNotifications: List<Notification>) {
        notifications = newNotifications
        notifyDataSetChanged()
    }
}
