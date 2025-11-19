package com.example.raceme

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EventsAdapter(
    private val onEventClicked: (RaceEvent) -> Unit
) : ListAdapter<RaceEvent, EventsAdapter.EventViewHolder>(DiffCallback) {

    object DiffCallback : DiffUtil.ItemCallback<RaceEvent>() {
        override fun areItemsTheSame(oldItem: RaceEvent, newItem: RaceEvent) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: RaceEvent, newItem: RaceEvent) =
            oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.event_item, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val titleText: TextView = itemView.findViewById(R.id.textTitle)
        private val dateText: TextView = itemView.findViewById(R.id.textDateTime)
        private val locationText: TextView = itemView.findViewById(R.id.textLocation)
        private val interestedCountText: TextView = itemView.findViewById(R.id.textInterestedCount)
        private val interestedButton: Button = itemView.findViewById(R.id.buttonInterested)

        fun bind(event: RaceEvent) {
            titleText.text = event.title
            locationText.text = event.locationName

            val dateStr = event.startTime?.toDate()?.toString() ?: "No date set"
            dateText.text = dateStr

            val interestedCount = event.interestedUserIds.size
            interestedCountText.text = "$interestedCount person(s) interested"

            val currentUid = FirebaseAuth.getInstance().currentUser?.uid
            val isInterested = currentUid != null && event.interestedUserIds.contains(currentUid)

            // Change button text based on state
            interestedButton.text = if (isInterested) "Not Interested" else "Interested"

            // Toggle when clicked
            interestedButton.setOnClickListener {
                toggleInterested(event.id)
            }

            // Click entire card
            itemView.setOnClickListener {
                onEventClicked(event)
            }
        }

        private fun toggleInterested(eventId: String) {
            val db = FirebaseFirestore.getInstance()
            val eventDoc = db.collection("events").document(eventId)
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

            db.runTransaction { transaction ->
                val snapshot = transaction.get(eventDoc)
                val list = snapshot.get("interestedUserIds") as? List<String> ?: emptyList()

                val newList = if (list.contains(uid)) {
                    list - uid
                } else {
                    list + uid
                }

                transaction.update(eventDoc, "interestedUserIds", newList)
            }
        }
    }
}
