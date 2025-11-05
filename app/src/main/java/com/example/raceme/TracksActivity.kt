package com.example.raceme

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
            // OPTIONAL: on click, pre-fill StartRun with selected name and open it
            Toast.makeText(this, "Selected: ${track.name}", Toast.LENGTH_SHORT).show()
            // Example: pass the chosen name to StartRun
            // val i = Intent(this, StartRunActivity::class.java)
            // i.putExtra("prefill_name", track.name)
            // startActivity(i)
        }

        b.rvTracks.layoutManager = LinearLayoutManager(this)
        b.rvTracks.adapter = adapter

        // Search box
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
        // Firestore: tracks where public == true
        db.collection("tracks")
            .whereEqualTo("public", true)
            .orderBy("name", Query.Direction.ASCENDING) // if 'name' field is present
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Toast.makeText(this, err.message ?: "Failed to load tracks", Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }
                val list = snap?.documents?.map { doc ->
                    Track(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        distanceMiles = (doc.getDouble("distanceMiles") ?: 0.0),
                        public = doc.getBoolean("public") ?: true,
                        createdBy = doc.getString("createdBy"),
                        createdAt = doc.getTimestamp("createdAt")
                    )
                }.orEmpty()
                adapter.setItems(list)
            }
    }
}
