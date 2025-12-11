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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityLeaderboardBinding.inflate(layoutInflater)
        setContentView(b.root)

        // 🔙 Back arrow
        b.btnBackLeaderboard.setOnClickListener {
            finish()
        }

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

                                // BASELINE: use whatever is stored on the user doc right now
                                val meters = d.getDouble("distanceMeters") ?: 0.0
                                val miles = meters / 1609.344
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
                                // Once all user docs loaded, recompute any user who HAS runs
                                recomputeAllFromRuns(combined)
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

    // recompute lifetime miles + steps from runs for every user who has runs
    private fun recomputeAllFromRuns(baseRows: List<LeaderboardUserRow>) {
        if (baseRows.isEmpty()) {
            allRows.clear()
            adapter.submit(emptyList())
            b.tvLeaderboardTitle.text = "Friends Leaderboard"
            b.progressBar.visibility = View.GONE
            return
        }

        val mutableRows = baseRows.toMutableList()
        var remaining = mutableRows.size

        for (row in mutableRows) {
            val uid = row.uid
            db.collection("users").document(uid)
                .collection("runs")
                .get()
                .addOnSuccessListener { runsSnap ->
                    // If no runs for this user → keep their existing steps/miles
                    if (runsSnap.isEmpty) {
                        return@addOnSuccessListener
                    }

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
                    // 🔢 use 1400 steps per mile now
                    val stepsApprox = (milesTotal * 1400.0).roundToInt()

                    val idx = mutableRows.indexOfFirst { it.uid == uid }
                    if (idx != -1) {
                        val old = mutableRows[idx]
                        mutableRows[idx] = old.copy(
                            steps = stepsApprox,
                            miles = milesTotal
                        )
                    }
                }
                .addOnFailureListener {
                    // ignore, leave baseline values
                }
                .addOnCompleteListener {
                    remaining--
                    if (remaining == 0) {
                        // All friends processed → update UI
                        allRows.clear()
                        allRows.addAll(mutableRows)
                        applySorting()

                        b.tvLeaderboardTitle.text = "Friends Leaderboard"
                        b.progressBar.visibility = View.GONE
                    }
                }
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
