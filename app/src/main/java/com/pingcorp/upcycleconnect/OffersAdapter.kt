package com.pingcorp.upcycleconnect

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class OffersAdapter(
    private val offers: List<Annonce>,
    private val onOfferClick: (Annonce) -> Unit
) : RecyclerView.Adapter<OffersAdapter.OfferViewHolder>() {

    class OfferViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleTextView: TextView = view.findViewById(R.id.offerTitle)
        val priceTextView: TextView = view.findViewById(R.id.offerPrice)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OfferViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_offer, parent, false)
        return OfferViewHolder(view)
    }

    override fun onBindViewHolder(holder: OfferViewHolder, position: Int) {
        val offer = offers[position]
        holder.titleTextView.text = offer.title
        holder.priceTextView.text = if (offer.price != null) "${offer.price} €" else "Free"
        holder.itemView.setOnClickListener { onOfferClick(offer) }
    }

    override fun getItemCount() = offers.size
}
