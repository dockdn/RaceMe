package com.example.raceme

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.raceme.databinding.ItemTrackBinding
import kotlin.math.round

class TracksAdapter(
    private val onClick: (Track) -> Unit
) : RecyclerView.Adapter<TracksAdapter.VH>() {

    // The full list of all tracks
    private val all = mutableListOf<Track>()
    private val shown = mutableListOf<Track>()


    class VH(val b: ItemTrackBinding) : RecyclerView.ViewHolder(b.root)

    //Full list of items
    fun setItems(items: List<Track>) {
        all.clear(); all.addAll(items)
        filter("")
    }

    //Trakcs by name using a  lowercase text match
    fun filter(query: String) {
        val q = query.trim().lowercase()
        shown.clear()
        if (q.isEmpty()) shown.addAll(all)
        else shown.addAll(all.filter { it.name.lowercase().contains(q) })
        notifyDataSetChanged()
    }

    // Inflate item view
    override fun onCreateViewHolder(p: ViewGroup, v: Int): VH {
        val b = ItemTrackBinding.inflate(LayoutInflater.from(p.context), p, false)
        return VH(b)
    }

    override fun getItemCount() = shown.size


    override fun onBindViewHolder(h: VH, i: Int) {
        val t = shown[i]

        // Set track name with fallback text for blank names
        h.b.tvName.text = t.name.ifBlank { "Untitled Track" }

        val miles = round(t.distanceMiles * 100.0) / 100.0
        h.b.tvMeta.text = "${miles} mi"

        h.b.root.setOnClickListener { onClick(t) }
    }
}
