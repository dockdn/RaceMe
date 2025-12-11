package com.example.raceme

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TracksAdapter(
    private val onClick: (Track) -> Unit
) : RecyclerView.Adapter<TracksAdapter.TrackViewHolder>() {

    private val allItems = mutableListOf<Track>()
    private val visibleItems = mutableListOf<Track>()

    fun setItems(list: List<Track>) {
        allItems.clear()
        allItems.addAll(list)
        visibleItems.clear()
        visibleItems.addAll(list)
        notifyDataSetChanged()
    }

    fun filter(query: String) {
        val q = query.trim().lowercase()
        visibleItems.clear()
        if (q.isEmpty()) {
            visibleItems.addAll(allItems)
        } else {
            visibleItems.addAll(
                allItems.filter { track ->
                    track.name.lowercase().contains(q) ||
                            track.type.lowercase().contains(q) ||
                            (track.addressText ?: "").lowercase().contains(q)
                }
            )
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_track_browse, parent, false)
        return TrackViewHolder(view)
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        val track = visibleItems[position]
        holder.bind(track, onClick)
    }

    override fun getItemCount(): Int = visibleItems.size

    class TrackViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvTrackName)
        private val tvType: TextView = itemView.findViewById(R.id.tvTrackType)
        private val tvAddress: TextView = itemView.findViewById(R.id.tvTrackAddress)

        fun bind(track: Track, onClick: (Track) -> Unit) {
            tvName.text = track.name

            val typeDisplay = if (track.type.isNotBlank()) track.type else "Open run"
            val distanceDisplay =
                if (track.distanceMiles > 0.0) String.format("%.2f mi", track.distanceMiles) else ""

            tvType.text = if (distanceDisplay.isNotEmpty()) {
                "$typeDisplay • $distanceDisplay"
            } else {
                typeDisplay
            }

            tvAddress.text = track.addressText ?: "Location: not specified"

            itemView.setOnClickListener { onClick(track) }
        }
    }
}
