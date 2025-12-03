package com.example.raceme

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.raceme.databinding.ItemRunBinding

class RunsAdapter(
    private val onDeleteClicked: (RunRow) -> Unit
) : RecyclerView.Adapter<RunsAdapter.VH>() {

    private val items = mutableListOf<RunRow>()

    // replace all rows with new list
    fun submit(list: List<RunRow>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    inner class VH(val b: ItemRunBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemRunBinding.inflate(inflater, parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val row = items[position]

        holder.b.tvTitle.text = "🏃 ${row.name}"
        holder.b.tvSub.text = row.whenText
        holder.b.tvMiles.text = String.format("%.2f mi", row.miles)
        holder.b.tvPace.text = row.pace.ifBlank { "--:-- / mi" }

        holder.b.ratingBar.rating = row.rating.toFloat()

        holder.b.tvQuote.text =
            if (row.quote.isBlank()) ""
            else "“${row.quote}”"

        holder.b.btnDeleteRun.setOnClickListener {
            onDeleteClicked(row)
        }
    }

    override fun getItemCount(): Int = items.size
}
