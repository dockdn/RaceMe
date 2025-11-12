package com.example.raceme

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.raceme.databinding.ActivityLeaderboardBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore

class LeaderboardActivity : BaseActivity() {

    private lateinit var binding: ActivityLeaderboardBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val usersList = mutableListOf<UserStats>()
    private lateinit var adapter: LeaderboardAdapter
    private var sortByDistance = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLeaderboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = LeaderboardAdapter(usersList, auth.currentUser?.uid ?: "")
        binding.rvLeaderboard.layoutManager = LinearLayoutManager(this)
        binding.rvLeaderboard.adapter = adapter

        binding.btnSortToggle.setOnClickListener {
            sortByDistance = !sortByDistance
            binding.btnSortToggle.text = if (sortByDistance) "Sort by Steps" else "Sort by Distance"
            loadLeaderboard() // reload with new sort
        }

        loadLeaderboard()
    }

    private fun loadLeaderboard() {
        val currentUid = auth.currentUser?.uid
        if (currentUid == null) {
            Toast.makeText(this, "Not signed in", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.tvLeaderboardTitle.text = "Loading..."

        db.collection("users").document(currentUid)
            .get()
            .addOnSuccessListener { doc ->
                val friends = doc.get("friends") as? List<*>
                val friendUids = friends?.filterIsInstance<String>()?.toMutableList() ?: mutableListOf()

                if (!friendUids.contains(currentUid)) friendUids.add(currentUid)
                if (friendUids.isEmpty()) {
                    binding.tvLeaderboardTitle.text = "No friends to show"
                    usersList.clear()
                    adapter.notifyDataSetChanged()
                    binding.progressBar.visibility = View.GONE
                    return@addOnSuccessListener
                }

                // Firestore whereIn max 10 items; batch if needed
                val chunks = friendUids.chunked(10)
                val combined = mutableListOf<UserStats>()
                var finishedChunks = 0

                for (chunk in chunks) {
                    db.collection("users")
                        .whereIn(FieldPath.documentId(), chunk)
                        .get()
                        .addOnSuccessListener { snap ->
                            val items = snap.documents.mapNotNull { d ->
                                val name = d.getString("displayName") ?: d.id
                                val steps = (d.getLong("steps") ?: 0L).toInt()
                                val distance = d.getDouble("distanceMeters") ?: 0.0
                                UserStats(name, steps, distance)
                            }
                            combined.addAll(items)
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Partial load error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                        .addOnCompleteListener {
                            finishedChunks++
                            if (finishedChunks == chunks.size) {
                                // Sort list
                                val sorted = if (sortByDistance) {
                                    combined.sortedByDescending { it.distance }
                                } else {
                                    combined.sortedByDescending { it.steps }
                                }
                                usersList.clear()
                                usersList.addAll(sorted)
                                adapter.notifyDataSetChanged()
                                binding.tvLeaderboardTitle.text = "Friends Leaderboard"
                                binding.progressBar.visibility = View.GONE
                            }
                        }
                }

            }
            .addOnFailureListener { e ->
                binding.tvLeaderboardTitle.text = "Failed to load user"
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Error fetching user doc: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}