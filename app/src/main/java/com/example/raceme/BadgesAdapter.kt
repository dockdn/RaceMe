package com.example.raceme

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.raceme.databinding.ItemBadgeBinding
import com.example.raceme.models.BadgeRow

class BadgesAdapter : RecyclerView.Adapter<BadgesAdapter.VH>() {
    private val items = mutableListOf<BadgeRow>()

    class VH(val b: ItemBadgeBinding) : RecyclerView.ViewHolder(b.root)

    fun submit(list: List<BadgeRow>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(p: ViewGroup, v: Int): VH {
        val b = ItemBadgeBinding.inflate(LayoutInflater.from(p.context), p, false)
        return VH(b)
    }

    override fun onBindViewHolder(h: VH, i: Int) {
        val row = items[i]
        h.b.tvEmoji.text = row.def.emoji
        h.b.tvTitle.text = row.def.title
        h.b.tvDesc.text  = row.def.desc
        h.b.tvState.text = if (row.earned) "Earned" else "Locked"

        val alpha = if (row.earned) 1f else 0.35f
        h.b.tvEmoji.alpha = alpha
        h.b.tvTitle.alpha = if (row.earned) 1f else 0.5f
        h.b.tvDesc.alpha  = if (row.earned) 1f else 0.5f
    }

    override fun getItemCount() = items.size
}
