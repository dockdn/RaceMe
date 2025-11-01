package com.example.raceme

import android.app.AlertDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import com.example.raceme.databinding.ActivityTracksBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class TracksActivity : BaseActivity() {
    private lateinit var b: ActivityTracksBinding
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private val items = mutableListOf<String>()
    private val docIds = mutableListOf<String>()
    private val owners = mutableListOf<String>()
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityTracksBinding.inflate(layoutInflater)
        setContentView(b.root)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
        b.list.adapter = adapter

        b.btnRefresh.setOnClickListener { load() }
        b.btnBack.setOnClickListener { finish() }

        // Optional: long-press to toggle public/private if user owns it
        b.list.setOnItemLongClickListener { _, _, position, _ ->
            val currentUser = auth.currentUser?.uid
            val owner = owners.getOrNull(position)
            val id = docIds.getOrNull(position)
            if (currentUser != null && owner == currentUser && id != null) {
                AlertDialog.Builder(this)
                    .setTitle("Privacy")
                    .setMessage("Toggle public/private for this race?")
                    .setPositiveButton("Toggle") { _, _ ->
                        togglePrivacy(id)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
                true
            } else {
                Toast.makeText(this, "Only the owner can change privacy", Toast.LENGTH_SHORT).show()
                true
            }
        }

        load()
    }

    private fun load() {
        items.clear()
        docIds.clear()
        owners.clear()

        db.collection("races")
            .whereEqualTo("public", true)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)
            .get()
            .addOnSuccessListener { snap ->
                for (doc in snap.documents) {
                    val race = doc.toObject(Race::class.java) ?: continue
                    items.add("${race.title} — ${race.type} — ${"%.2f".format(race.lapsPerMile)} laps/mi")
                    docIds.add(doc.id)
                    owners.add(race.userId)
                }
                adapter.notifyDataSetChanged()
            }
    }

    private fun togglePrivacy(docId: String) {
        val uid = auth.currentUser?.uid ?: return
        // Verify ownership client-side (rules still protect server-side)
        db.collection("races").document(docId).get().addOnSuccessListener { d ->
            val owner = d.getString("userId")
            if (owner == uid) {
                val current = d.getBoolean("public") ?: true
                db.collection("races").document(docId)
                    .update("public", !current)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Updated privacy", Toast.LENGTH_SHORT).show()
                        load()
                    }
            } else {
                Toast.makeText(this, "Only owner can update privacy", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
