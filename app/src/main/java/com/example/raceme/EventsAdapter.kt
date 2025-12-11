package com.example.raceme

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

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

        private val db = FirebaseFirestore.getInstance()
        private val auth = FirebaseAuth.getInstance()

        fun bind(event: RaceEvent) {
            titleText.text = event.title.ifBlank { "Untitled event" }

            // Address/location
            val loc = event.locationName
            locationText.text = if (loc.isBlank()) "Location: not set" else loc

            // Date/time formatting
            val dateStr = event.startTime?.toDate()?.let { date ->
                val fmt = SimpleDateFormat("EEE, MMM d • h:mm a", Locale.US)
                fmt.format(date)
            } ?: "No date set"
            dateText.text = dateStr

            // Interested count
            var interestedList = event.interestedUserIds ?: emptyList()
            var interestedCount = interestedList.size

            val currentUid = auth.currentUser?.uid
            var isInterested = currentUid != null && interestedList.contains(currentUid)

            updateInterestedUI(isInterested, interestedCount)

            // Click to toggle interested
            interestedButton.setOnClickListener {
                if (currentUid == null) {
                    Toast.makeText(
                        itemView.context,
                        "You must be logged in to mark interest",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                val eventId = event.id
                if (eventId.isBlank()) {
                    Toast.makeText(
                        itemView.context,
                        "Event ID missing; try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                // Local optimistic update
                if (isInterested) {
                    isInterested = false
                    interestedCount = (interestedCount - 1).coerceAtLeast(0)
                } else {
                    isInterested = true
                    interestedCount += 1
                }
                updateInterestedUI(isInterested, interestedCount)

                // Firestore transaction toggling array
                toggleInterestedInFirestore(eventId, currentUid)
            }

            // Whole card click
            itemView.setOnClickListener {
                onEventClicked(event)
            }
        }

        private fun updateInterestedUI(isInterested: Boolean, count: Int) {
            interestedButton.text = if (isInterested) "Not interested" else "I'm interested"
            interestedCountText.text = "$count person(s) interested"
        }

        private fun toggleInterestedInFirestore(eventId: String, uid: String) {
            val eventDoc = db.collection("events").document(eventId)

            db.runTransaction { transaction ->
                val snapshot = transaction.get(eventDoc)
                val list = snapshot.get("interestedUserIds") as? List<String> ?: emptyList()

                val newList = if (list.contains(uid)) {
                    list - uid
                } else {
                    list + uid
                }

                transaction.update(eventDoc, "interestedUserIds", newList)
            }.addOnFailureListener {
                // We already did an optimistic UI update; Firestore listener will correct
            }
        }
    }
}
