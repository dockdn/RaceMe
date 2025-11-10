package com.example.raceme

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.raceme.databinding.ActivityTracksBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class TracksActivity : BaseActivity() {
    private lateinit var b: ActivityTracksBinding
    private val db by lazy { FirebaseFirestore.getInstance() }
    private lateinit var adapter: TracksAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityTracksBinding.inflate(layoutInflater)
        setContentView(b.root)

        adapter = TracksAdapter { track ->
            // Return selection to the caller (StartRunActivity)
            val data = Intent()
                .putExtra("selected_track_name", track.name)
                .putExtra("selected_public_race_id", track.id)
            setResult(Activity.RESULT_OK, data)
            finish()
        }

        b.rvTracks.layoutManager = LinearLayoutManager(this)
        b.rvTracks.adapter = adapter

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
        db.collection("publicRaces")
            .orderBy("name", Query.Direction.ASCENDING)
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
                        public = true,
                        createdBy = doc.getString("ownerUid"),
                        createdAt = doc.getTimestamp("createdAt")
                    )
                }.orEmpty()

                adapter.setItems(list)
            }
    }
}
