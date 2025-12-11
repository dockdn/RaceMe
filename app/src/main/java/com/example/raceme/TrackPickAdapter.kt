package com.example.raceme

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * Adapter for picking a public track (uses item_track.xml).
 */
class TrackPickAdapter(
    private val onClick: (Track) -> Unit
) : RecyclerView.Adapter<TrackPickAdapter.TrackViewHolder>() {

    private val items = mutableListOf<Track>()

    fun setItems(list: List<Track>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_track, parent, false)
        return TrackViewHolder(view)
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        val track = items[position]
        holder.bind(track, onClick)
    }

    override fun getItemCount(): Int = items.size

    class TrackViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val tvType: TextView = itemView.findViewById(R.id.tvType)
        private val tvDistance: TextView = itemView.findViewById(R.id.tvDistance)
        private val tvAddress: TextView = itemView.findViewById(R.id.tvAddress)

        fun bind(track: Track, onClick: (Track) -> Unit) {
            tvName.text = track.name

            val typeDisplay = if (track.type.isNotBlank()) track.type else "Open run"
            tvType.text = "Type: $typeDisplay"

            val distanceDisplay =
                if (track.distanceMiles > 0.0) String.format("%.2f mi", track.distanceMiles)
                else ""
            tvDistance.text = distanceDisplay

            tvAddress.text = track.addressText ?: "Location: not specified"

            itemView.setOnClickListener { onClick(track) }
        }
    }
}
