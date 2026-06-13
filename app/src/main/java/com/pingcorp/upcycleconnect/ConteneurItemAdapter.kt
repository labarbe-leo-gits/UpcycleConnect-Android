package com.pingcorp.upcycleconnect

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ConteneurItemAdapter(
    private val items: MutableList<ConteneurItem>,
    private val onItemClick: (ConteneurItem) -> Unit
) : RecyclerView.Adapter<ConteneurItemAdapter.ItemViewHolder>() {

    class ItemViewHolder(view: View, onItemClick: (Int) -> Unit) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.textViewItemName)
        val description: TextView = view.findViewById(R.id.textViewItemDescription)
        val status: TextView = view.findViewById(R.id.textViewItemStatus)

        init {
            view.setOnClickListener {
                onItemClick(adapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_conteneur_item, parent, false)
        return ItemViewHolder(view) { position ->
            if (position != RecyclerView.NO_POSITION) {
                onItemClick(items[position])
            }
        }
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = items[position]
        holder.title.text = item.objectName
        holder.description.text = item.objectDescription
        holder.status.text = mapStatus(item.status)
    }

    override fun getItemCount() = items.size

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

    fun addItems(newItems: List<ConteneurItem>) {
        val startPos = items.size
        items.addAll(newItems)
        notifyItemRangeInserted(startPos, newItems.size)
    }
}
