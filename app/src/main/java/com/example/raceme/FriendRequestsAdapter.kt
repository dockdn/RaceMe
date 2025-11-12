package com.example.raceme

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Adapter for RecyclerView to display incoming friend requests
class FriendRequestsAdapter(
    private val requests: MutableList<FriendRequest>,  // List of requests
    private val onAccept: (FriendRequest) -> Unit,     // Callback for accept button
    private val onReject: (FriendRequest) -> Unit     // Callback for reject button
) : RecyclerView.Adapter<FriendRequestsAdapter.RequestViewHolder>() {

    inner class RequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvFromUid: TextView = itemView.findViewById(R.id.tvFromUid)
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
        holder.tvFromUid.text = "Request from: ${request.fromUid}"
        holder.btnAccept.setOnClickListener { onAccept(request) }
        holder.btnReject.setOnClickListener { onReject(request) }
    }

    override fun getItemCount(): Int = requests.size

    // Optional helper function to update the list
    fun setData(newList: List<FriendRequest>) {
        requests.clear()
        requests.addAll(newList)
        notifyDataSetChanged()
    }
}