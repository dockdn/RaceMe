package com.example.raceme

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.raceme.databinding.ItemChallengeBinding

class ChallengeAdapter : RecyclerView.Adapter<ChallengeAdapter.VH>() {

    private val items = mutableListOf<ChallengeRow>()

    class VH(val b: ItemChallengeBinding) : RecyclerView.ViewHolder(b.root)

    fun submit(list: List<ChallengeRow>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemChallengeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val row = items[position]
        holder.b.tvTitle.text = row.def.title
        holder.b.tvDesc.text  = row.def.desc
        holder.b.tvProgress.text = row.progressLabel
        holder.b.tvSub.text = row.subLabel
        holder.b.progressBar.progress = row.progressPercent
        holder.b.tvPct.text = "${row.progressPercent}%"
        holder.b.badgeEarned.alpha = if (row.earned) 1f else 0.2f
    }

    override fun getItemCount(): Int = items.size
}
