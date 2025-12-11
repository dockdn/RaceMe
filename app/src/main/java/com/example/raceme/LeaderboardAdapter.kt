package com.example.raceme

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// row model for leaderboard (includes uid so we can recompute per user)
data class LeaderboardUserRow(
    val uid: String,
    val name: String,
    val steps: Int,
    val miles: Double
)

class LeaderboardAdapter(
    private val items: MutableList<LeaderboardUserRow>
) : RecyclerView.Adapter<LeaderboardAdapter.ViewHolder>() {

    // replace current list with new rows
    fun submit(newItems: List<LeaderboardUserRow>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    // view holder for one row
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvRank: TextView = view.findViewById(R.id.tvRank)
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvSteps: TextView = view.findViewById(R.id.tvSteps)
        val tvDistance: TextView = view.findViewById(R.id.tvDistance)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_leaderboard_row, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        // rank number
        holder.tvRank.text = (position + 1).toString()

        // racer name
        holder.tvName.text = item.name

        // steps (approximate)
        holder.tvSteps.text = "Steps: ${item.steps}"

        // miles total
        holder.tvDistance.text = String.format("Miles: %.2f", item.miles)
    }

    override fun getItemCount(): Int = items.size
}
