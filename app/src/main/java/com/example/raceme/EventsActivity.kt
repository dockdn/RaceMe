package com.example.raceme

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.ImageView
import android.widget.RadioGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class EventsActivity : AppCompatActivity() {

    // adapter for event rows
    private lateinit var eventsAdapter: EventsAdapter

    // firestore
    private val db = FirebaseFirestore.getInstance()
    private val eventsRef = db.collection("events")

    // listeners
    private var allEventsListener: ListenerRegistration? = null
    private var myEventsListener: ListenerRegistration? = null

    private val currentUserId: String?
        get() = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_events)

        // ui refs
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val radioGroup = findViewById<RadioGroup>(R.id.radioGroupFilter)
        val recycler = findViewById<RecyclerView>(R.id.recyclerEvents)

        // back button
        btnBack.setOnClickListener {
            finish()
        }

        // adapter + list setup
        eventsAdapter = EventsAdapter {}

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = eventsAdapter

        // show upcoming events
        startAllEventsListener()

        // filter toggle
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
    }

    // load upcoming all events
    private fun startAllEventsListener() {
        if (allEventsListener != null) return

        val nowMs = System.currentTimeMillis()

        allEventsListener = eventsRef
            .orderBy("startTime")
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener

                val events = snap.toObjects(RaceEvent::class.java)
                    .filter { it.startTime?.toDate()?.time ?: 0 >= nowMs }

                eventsAdapter.submitList(events)
            }
    }

    // load upcoming events you're attending
    private fun startMyEventsListener() {
        val uid = currentUserId ?: return
        if (myEventsListener != null) return

        val nowMs = System.currentTimeMillis()

        myEventsListener = eventsRef
            .whereArrayContains("interestedUserIds", uid)
            .orderBy("startTime")
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener

                val events = snap.toObjects(RaceEvent::class.java)
                    .filter { it.startTime?.toDate()?.time ?: 0 >= nowMs }

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
