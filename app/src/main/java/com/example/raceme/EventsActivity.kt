package com.example.raceme

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.raceme.databinding.ActivityEventsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class EventsActivity : BaseActivity() {

    private lateinit var b: ActivityEventsBinding
    private val db = FirebaseFirestore.getInstance()
    private val eventsRef = db.collection("events")

    private var allEventsListener: ListenerRegistration? = null
    private var myEventsListener: ListenerRegistration? = null

    private val currentUserId: String?
        get() = FirebaseAuth.getInstance().currentUser?.uid

    private lateinit var eventsAdapter: EventsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityEventsBinding.inflate(layoutInflater)
        setContentView(b.root)

        // 🔙 Back button
        b.btnBackEvents.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
        }

        // Recycler + adapter
        eventsAdapter = EventsAdapter { event ->
            // (Optional: tap behavior later)
        }
        b.recyclerEvents.layoutManager = LinearLayoutManager(this)
        b.recyclerEvents.adapter = eventsAdapter

        // Default = ALL events
        b.radioAll.isChecked = true
        startAllEventsListener()

        // Toggle between ALL and MY
        b.radioGroupFilter.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radioAll -> {
                    stopMyEventsListener()
                    startAllEventsListener()
                }
                R.id.radioMy -> {
                    stopAllEventsListener()
                    startMyEventsListener()
                }
            }
        }

        // Create new event
        b.btnCreateEvent.setOnClickListener {
            startActivity(Intent(this, CreateEventActivity::class.java))
        }
    }

    // ==========================
    //   LISTEN: ALL EVENTS
    // ==========================
    private fun startAllEventsListener() {
        if (allEventsListener != null) return

        allEventsListener = eventsRef
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(
                        this,
                        error.message ?: "Failed to load events",
                        Toast.LENGTH_LONG
                    ).show()
                    updateList(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    updateList(emptyList())
                    return@addSnapshotListener
                }

                val rawList = snapshot.toObjects(RaceEvent::class.java)
                updateList(rawList)
            }
    }

    // ==========================
    //   LISTEN: MY EVENTS ONLY
    // ==========================
    private fun startMyEventsListener() {
        val uid = currentUserId ?: return
        if (myEventsListener != null) return

        myEventsListener = eventsRef
            .whereArrayContains("interestedUserIds", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(
                        this,
                        error.message ?: "Failed to load my events",
                        Toast.LENGTH_LONG
                    ).show()
                    updateList(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    updateList(emptyList())
                    return@addSnapshotListener
                }

                val rawList = snapshot.toObjects(RaceEvent::class.java)
                updateList(rawList)
            }
    }

    // Stop listeners
    private fun stopAllEventsListener() {
        allEventsListener?.remove()
        allEventsListener = null
    }

    private fun stopMyEventsListener() {
        myEventsListener?.remove()
        myEventsListener = null
    }

    // Update list + empty label
    private fun updateList(events: List<RaceEvent>) {
        eventsAdapter.submitList(events)
        b.tvEventsEmpty.visibility = if (events.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAllEventsListener()
        stopMyEventsListener()
    }
}
