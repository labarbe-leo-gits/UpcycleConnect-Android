package com.pingcorp.upcycleconnect

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load

class ConteneurItemAdapter(private val items: MutableList<ConteneurItem>) :
    RecyclerView.Adapter<ConteneurItemAdapter.ItemViewHolder>() {

    class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageViewItem)
        val title: TextView = view.findViewById(R.id.textViewItemName)
        val description: TextView = view.findViewById(R.id.textViewItemDescription)
        val status: TextView = view.findViewById(R.id.textViewItemStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_conteneur_item, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = items[position]
        holder.title.text = item.objectName
        holder.description.text = item.objectDescription
        holder.status.text = mapStatus(item.status)

        val firstFile = item.files.firstOrNull()
        if (firstFile != null) {
            val imageUrl = "http://10.0.2.2:8081/uploads/deposits/${firstFile.filename}"
            holder.imageView.load(imageUrl) {
                placeholder(android.R.color.darker_gray)
                error(android.R.color.darker_gray)
            }
        } else {
            holder.imageView.setImageResource(android.R.color.darker_gray)
        }
    }

    override fun getItemCount() = items.size

    private fun mapStatus(status: Int): String {
        return when (status) {
            0 -> "Pending"
            1 -> "Accepted"
            2 -> "Rejected"
            else -> "Unknown"
        }
    }

    fun addItems(newItems: List<ConteneurItem>) {
        val startPos = items.size
        items.addAll(newItems)
        notifyItemRangeInserted(startPos, newItems.size)
    }
}
