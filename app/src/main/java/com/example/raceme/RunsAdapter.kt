package com.example.raceme

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.raceme.databinding.ItemRunBinding
import kotlin.math.round

class RunsAdapter : RecyclerView.Adapter<RunsAdapter.VH>() {

    private val items = mutableListOf<RunRow>()

    class VH(val binding: ItemRunBinding) : RecyclerView.ViewHolder(binding.root)

    fun submit(list: List<RunRow>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemRunBinding.inflate(inflater, parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val row = items[position]
        val b = holder.binding

        b.tvTitle.text = "🏃 ${row.name}"
        b.tvSub.text = row.whenText
        b.tvMiles.text = String.format("%.2f mi", round(row.miles * 100.0) / 100.0)
        b.tvPace.text = if (row.pace.isNotBlank()) row.pace else "--:-- / mi"
        b.ratingBar.rating = row.rating.toFloat()
        b.ratingBar.setIsIndicator(true)

        if (row.quote.isBlank()) {
            b.tvQuote.text = ""
            b.tvQuote.visibility = View.GONE
        } else {
            b.tvQuote.text = "“${row.quote}”"
            b.tvQuote.visibility = View.VISIBLE
        }
    }

    override fun getItemCount(): Int = items.size
}
