package com.example.raceme

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.raceme.databinding.ActivityTracksBinding
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore

class TracksActivity : BaseActivity() {
    private lateinit var b: ActivityTracksBinding
    private val db by lazy { FirebaseFirestore.getInstance() }
    private lateinit var adapter: TracksAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityTracksBinding.inflate(layoutInflater)
        setContentView(b.root)

        adapter = TracksAdapter { track ->
            Toast.makeText(this, "Selected: ${track.name}", Toast.LENGTH_SHORT).show()
        }

        b.rvTracks.layoutManager = LinearLayoutManager(this)
        b.rvTracks.adapter = adapter

        // Search box filters the in-memory list
        b.inputSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b2: Int, c: Int) {
                adapter.filter(s?.toString().orEmpty())
            }
        })

        fetchPublicTracks()
    }

    private fun fetchPublicTracks() {
        db.collection("public_races")
            .whereEqualTo("visibility", "public")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Toast.makeText(this, err.message ?: "Failed to load tracks", Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }
                val list = snap?.documents?.map { doc ->
                    Track(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        distanceMiles = when {
                            doc.getDouble("distanceMiles") != null -> doc.getDouble("distanceMiles")!!
                            doc.getDouble("distanceMeters") != null -> (doc.getDouble("distanceMeters")!! / 1609.344)
                            else -> 0.0
                        },
                        public = (doc.getString("visibility") ?: "public") == "public",
                        createdBy = doc.getString("ownerId"),
                        createdAt = doc.getTimestamp("createdAt")
                    )
                }.orEmpty()
                    .sortedBy { it.name.lowercase() }

                adapter.setItems(list)
            }
    }
}
