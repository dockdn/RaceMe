package com.example.raceme

import android.content.Intent
import android.os.Bundle
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class EventsActivity : AppCompatActivity() {

    private lateinit var eventsAdapter: EventsAdapter
    private val db = FirebaseFirestore.getInstance()
    private val eventsRef = db.collection("events")

    private var allEventsListener: ListenerRegistration? = null
    private var myEventsListener: ListenerRegistration? = null

    private val currentUserId: String?
        get() = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_events)

        val radioGroup = findViewById<RadioGroup>(R.id.radioGroupFilter)
        val recycler = findViewById<RecyclerView>(R.id.recyclerEvents)
        val fabAddEvent = findViewById<FloatingActionButton>(R.id.fabAddEvent)

        eventsAdapter = EventsAdapter { event ->
            // Optional: open details screen here
        }

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = eventsAdapter

        // Show all events
        startAllEventsListener()

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
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

        // FAB: open Create Event screen
        fabAddEvent.setOnClickListener {
            val intent = Intent(this, CreateEventActivity::class.java)
            startActivity(intent)
        }
    }

    private fun startAllEventsListener() {
        if (allEventsListener != null) return

        allEventsListener = eventsRef
            .orderBy("startTime")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val events = snapshot.toObjects(RaceEvent::class.java)
                eventsAdapter.submitList(events)
            }
    }

    private fun startMyEventsListener() {
        val uid = currentUserId ?: return

        if (myEventsListener != null) return

        myEventsListener = eventsRef
            .whereArrayContains("interestedUserIds", uid)
            .orderBy("startTime")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val events = snapshot.toObjects(RaceEvent::class.java)
                eventsAdapter.submitList(events)
            }
    }

    private fun stopAllEventsListener() {
        allEventsListener?.remove()
        allEventsListener = null
    }

    private fun stopMyEventsListener() {
        myEventsListener?.remove()
        myEventsListener = null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAllEventsListener()
        stopMyEventsListener()
    }
}
