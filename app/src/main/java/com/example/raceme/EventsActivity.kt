package com.example.raceme

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.raceme.databinding.ActivityEventsBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.Date

class EventsActivity : BaseActivity() {

    // view + firebase

    private lateinit var b: ActivityEventsBinding
    private val db = FirebaseFirestore.getInstance()
    private val eventsRef = db.collection("events")

    private var allEventsListener: ListenerRegistration? = null
    private var myEventsListener: ListenerRegistration? = null

    private val currentUserId: String?
        get() = FirebaseAuth.getInstance().currentUser?.uid

    private lateinit var eventsAdapter: EventsAdapter

    // lifecycle: onCreate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityEventsBinding.inflate(layoutInflater)
        setContentView(b.root)

        // header back button
        b.btnBackEvents.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
        }

        // recycler + adapter
        eventsAdapter = EventsAdapter { event ->
        }
        b.recyclerEvents.layoutManager = LinearLayoutManager(this)
        b.recyclerEvents.adapter = eventsAdapter

        // filter: default show all events
        b.radioAll.isChecked = true
        startAllEventsListener()

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

        // create event button
        b.btnCreateEvent.setOnClickListener {
            startActivity(Intent(this, CreateEventActivity::class.java))
        }
    }

    // start listener: all upcoming events

    private fun startAllEventsListener() {
        if (allEventsListener != null) return

        allEventsListener = eventsRef
            .orderBy("startTime")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val rawList = snapshot.toObjects(RaceEvent::class.java)
                val upcoming = filterUpcoming(rawList)

                updateList(upcoming)
            }
    }

    // start listener: my upcoming events (where user is interested)

    private fun startMyEventsListener() {
        val uid = currentUserId ?: return
        if (myEventsListener != null) return

        myEventsListener = eventsRef
            .whereArrayContains("interestedUserIds", uid)
            .orderBy("startTime")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val rawList = snapshot.toObjects(RaceEvent::class.java)
                val upcoming = filterUpcoming(rawList)

                updateList(upcoming)
            }
    }

    // stop listeners

    private fun stopAllEventsListener() {
        allEventsListener?.remove()
        allEventsListener = null
    }

    private fun stopMyEventsListener() {
        myEventsListener?.remove()
        myEventsListener = null
    }

    // filter helper: only future events

    private fun filterUpcoming(list: List<RaceEvent>): List<RaceEvent> {
        val now = Date()
        return list.filter { event ->
            val ts = event.startTime
            ts != null && ts.toDate().after(now)
        }
    }

    // update UI with list + empty state

    private fun updateList(events: List<RaceEvent>) {
        eventsAdapter.submitList(events)
        if (events.isEmpty()) {
            b.tvEventsEmpty.visibility = View.VISIBLE
        } else {
            b.tvEventsEmpty.visibility = View.GONE
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopAllEventsListener()
        stopMyEventsListener()
    }
}
