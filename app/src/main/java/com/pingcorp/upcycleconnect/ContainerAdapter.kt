package com.pingcorp.upcycleconnect

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ContainerAdapter(private val containers: List<Container>):
        RecyclerView.Adapter<ContainerAdapter.ContainerViewHolder>(){
    class ContainerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.containerName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContainerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_container, parent, false)
        return ContainerViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContainerViewHolder, position: Int){
        val container = containers[position]
        holder.nameTextView.text = container.name
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, ContainerActivity::class.java)
            intent.putExtra("CONTAINER_ID", container.id)
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = containers.size
        }