package com.example.raceme

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

// Adapter for RecyclerView to display incoming friend requests
class FriendRequestsAdapter(
    private val requests: MutableList<FriendRequest>,  // List of requests
    private val onAccept: (FriendRequest) -> Unit,     // Callback for accept button
    private val onReject: (FriendRequest) -> Unit     // Callback for reject button
) : RecyclerView.Adapter<FriendRequestsAdapter.RequestViewHolder>() {

    inner class RequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvFrom: TextView = itemView.findViewById(R.id.tvFromUid)
        val btnAccept: Button = itemView.findViewById(R.id.btnAccept)
        val btnReject: Button = itemView.findViewById(R.id.btnReject)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend_request, parent, false)
        return RequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        val request = requests[position]

        // Display sender email if available, fallback to UID
        val senderDisplay = request.fromEmail ?: request.fromUid
        holder.tvFrom.text = "Request from: $senderDisplay"

        // Enable/disable buttons based on status
        val isPending = request.status == "pending"
        holder.btnAccept.isEnabled = isPending
        holder.btnReject.isEnabled = isPending

        holder.btnAccept.setOnClickListener { if (isPending) onAccept(request) }
        holder.btnReject.setOnClickListener { if (isPending) onReject(request) }

        // Visually indicate accepted/rejected by disabling buttons and changing background
        if (!isPending) {
            holder.btnAccept.isEnabled = false
            holder.btnReject.isEnabled = false
            holder.itemView.setBackgroundColor(
                ContextCompat.getColor(holder.itemView.context, android.R.color.darker_gray)
            )
        } else {
            holder.itemView.setBackgroundColor(
                ContextCompat.getColor(holder.itemView.context, android.R.color.white)
            )
        }
    }

    override fun getItemCount(): Int = requests.size

    // Helper function to update the list
    fun setData(newList: List<FriendRequest>) {
        requests.clear()
        requests.addAll(newList)
        notifyDataSetChanged()
    }
}