package com.example.raceme

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.raceme.databinding.ActivityLeaderboardBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.roundToInt

class LeaderboardActivity : BaseActivity() {

    // view + firebase

    private lateinit var b: ActivityLeaderboardBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    // data + adapter

    private val allRows = mutableListOf<LeaderboardUserRow>()
    private lateinit var adapter: LeaderboardAdapter
    private var sortByDistance = true   // distance first, steps when toggled

    // lifecycle: onCreate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityLeaderboardBinding.inflate(layoutInflater)
        setContentView(b.root)

        // header labels
        b.tvLeaderboardTitle.text = "Friends Leaderboard"
        b.tvLeaderboardSubtitle.text = "Compare steps and miles with your friends."

        // recycler setup
        adapter = LeaderboardAdapter(mutableListOf())
        b.rvLeaderboard.layoutManager = LinearLayoutManager(this)
        b.rvLeaderboard.adapter = adapter

        // radio buttons control sort (distance vs steps)
        b.rbSortDistance.isChecked = true
        b.rbSortSteps.isChecked = false

        b.rbSortDistance.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                sortByDistance = true
                applySorting()
            }
        }

        b.rbSortSteps.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                sortByDistance = false
                applySorting()
            }
        }

        // load data
        loadLeaderboard()
    }

    // load leaderboard using current user's "friends" array

    private fun loadLeaderboard() {
        val currentUid = auth.currentUser?.uid
        if (currentUid == null) {
            Toast.makeText(this, "Not signed in", Toast.LENGTH_SHORT).show()
            return
        }

        b.progressBar.visibility = View.VISIBLE
        b.tvLeaderboardTitle.text = "Loading leaderboard…"

        db.collection("users").document(currentUid)
            .get()
            .addOnSuccessListener { doc ->
                val friends = doc.get("friends") as? List<*>
                val friendUids = friends?.filterIsInstance<String>()?.toMutableList()
                    ?: mutableListOf()

                // always include me
                if (!friendUids.contains(currentUid)) {
                    friendUids.add(currentUid)
                }

                if (friendUids.isEmpty()) {
                    b.tvLeaderboardTitle.text = "Friends Leaderboard"
                    b.progressBar.visibility = View.GONE
                    allRows.clear()
                    adapter.submit(emptyList())
                    return@addOnSuccessListener
                }

                // Firestore whereIn max 10 items → chunk friend IDs
                val chunks = friendUids.chunked(10)
                val combined = mutableListOf<LeaderboardUserRow>()
                var finishedChunks = 0

                for (chunk in chunks) {
                    db.collection("users")
                        .whereIn(FieldPath.documentId(), chunk)
                        .get()
                        .addOnSuccessListener { snap ->
                            val rows = snap.documents.map { d ->
                                val uid = d.id
                                val name =
                                    d.getString("displayName")
                                        ?: d.getString("email")
                                        ?: uid.take(6)

                                // lifetime meters → miles (may be off for some users)
                                val meters = d.getDouble("distanceMeters") ?: 0.0
                                val miles = meters / 1609.344

                                // steps (already stored on user doc)
                                val steps = (d.getLong("steps") ?: 0L).toInt()

                                LeaderboardUserRow(
                                    uid = uid,
                                    name = name,
                                    steps = steps,
                                    miles = miles
                                )
                            }
                            combined.addAll(rows)
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                this,
                                "Partial leaderboard error: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        .addOnCompleteListener {
                            finishedChunks++
                            if (finishedChunks == chunks.size) {
                                // after all user docs loaded, fix *my* row using runs
                                recomputeCurrentUserFromRuns(currentUid, combined)
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                b.tvLeaderboardTitle.text = "Friends Leaderboard"
                b.progressBar.visibility = View.GONE
                Toast.makeText(
                    this,
                    "Error loading friends: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    // recompute *my* lifetime miles + steps from runs (so they are accurate)

    private fun recomputeCurrentUserFromRuns(
        currentUid: String,
        baseRows: MutableList<LeaderboardUserRow>
    ) {
        db.collection("users").document(currentUid)
            .collection("runs")
            .get()
            .addOnSuccessListener { runsSnap ->
                var totalMeters = 0.0

                for (d in runsSnap.documents) {
                    val meters = d.getDouble("distanceMeters")
                    val miles = d.getDouble("distanceMiles")

                    totalMeters += when {
                        meters != null -> meters
                        miles != null -> miles * 1609.344
                        else -> 0.0
                    }
                }

                val milesTotal = totalMeters / 1609.344
                val stepsApprox = (milesTotal * 2100.0).roundToInt()

                // update my row if present
                val idx = baseRows.indexOfFirst { it.uid == currentUid }
                if (idx != -1) {
                    val old = baseRows[idx]
                    baseRows[idx] = old.copy(
                        steps = stepsApprox,
                        miles = milesTotal
                    )
                }

                // now apply sorting + show in UI
                allRows.clear()
                allRows.addAll(baseRows)
                applySorting()

                b.tvLeaderboardTitle.text = "Friends Leaderboard"
                b.progressBar.visibility = View.GONE
            }
            .addOnFailureListener {
                // if run query fails, just use baseRows as-is
                allRows.clear()
                allRows.addAll(baseRows)
                applySorting()

                b.tvLeaderboardTitle.text = "Friends Leaderboard"
                b.progressBar.visibility = View.GONE
            }
    }

    // sort + update adapter based on current mode

    private fun applySorting() {
        if (allRows.isEmpty()) {
            adapter.submit(emptyList())
            return
        }

        val sorted = if (sortByDistance) {
            allRows.sortedByDescending { it.miles }
        } else {
            allRows.sortedByDescending { it.steps }
        }

        adapter.submit(sorted)
    }
}
