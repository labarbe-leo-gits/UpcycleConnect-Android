package com.pingcorp.upcycleconnect

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class UpdocAdapter(
    private val updocs: List<Project>,
    private val onItemClick: (Project) -> Unit
) : RecyclerView.Adapter<UpdocAdapter.UpdocViewHolder>() {

    class UpdocViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.textViewTitle)
        val description: TextView = view.findViewById(R.id.textViewDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UpdocViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_updoc, parent, false)
        return UpdocViewHolder(view)
    }

    override fun onBindViewHolder(holder: UpdocViewHolder, position: Int) {
        val updoc = updocs[position]
        holder.title.text = updoc.title
        holder.description.text = updoc.description
        holder.itemView.setOnClickListener { onItemClick(updoc) }
    }

    override fun getItemCount() = updocs.size
}
