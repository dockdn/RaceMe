package com.example.raceme

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.raceme.databinding.ItemTrackBinding
import kotlin.math.round

class TrackPickAdapter(
    private val onClick: (Track) -> Unit
) : RecyclerView.Adapter<TrackPickAdapter.VH>() {

    private val all = mutableListOf<Track>()
    private val shown = mutableListOf<Track>()

    class VH(val b: ItemTrackBinding) : RecyclerView.ViewHolder(b.root)

    fun setItems(items: List<Track>) {
        all.clear(); all.addAll(items)
        filter("") // show all initially
    }

    fun filter(query: String) {
        val q = query.trim().lowercase()
        shown.clear()
        if (q.isEmpty()) shown.addAll(all)
        else shown.addAll(all.filter { it.name.lowercase().contains(q) })
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(p: ViewGroup, v: Int): VH {
        val b = ItemTrackBinding.inflate(LayoutInflater.from(p.context), p, false)
        return VH(b)
    }

    override fun getItemCount() = shown.size

    override fun onBindViewHolder(h: VH, i: Int) {
        val t = shown[i]
        h.b.tvName.text = t.name.ifBlank { "Untitled Track" }
        val miles = round(t.distanceMiles * 100.0) / 100.0
        h.b.tvMeta.text = "${miles} mi • Public"
        h.b.root.setOnClickListener { onClick(t) }
    }
}
