package com.example.raceme

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.raceme.databinding.ActivityLeaderboardBinding
import com.google.firebase.auth.FirebaseAuth
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

    /**
     * Load leaderboard based on the current user's "friends" array.
     * For each UID (friends + me), we recompute lifetime miles/steps from runs.
     */
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

                // if somehow empty, just reset UI
                if (friendUids.isEmpty()) {
                    b.tvLeaderboardTitle.text = "Friends Leaderboard"
                    b.progressBar.visibility = View.GONE
                    allRows.clear()
                    adapter.submit(emptyList())
                    return@addOnSuccessListener
                }

                fetchAllUserRows(friendUids)
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

    /**
     * For each UID, fetch:
     *  - displayName/email (from users doc)
     *  - ALL runs under users/{uid}/runs
     * Then compute lifetime miles + steps from runs.
     */
    private fun fetchAllUserRows(uids: List<String>) {
        val results = mutableListOf<LeaderboardUserRow>()
        var completed = 0
        val total = uids.size

        fun doneOne() {
            completed++
            if (completed == total) {
                // all users processed, update UI
                allRows.clear()
                allRows.addAll(results)
                applySorting()

                b.tvLeaderboardTitle.text = "Friends Leaderboard"
                b.progressBar.visibility = View.GONE
            }
        }

        for (uid in uids) {
            db.collection("users").document(uid)
                .get()
                .addOnSuccessListener { userDoc ->
                    val name = userDoc.getString("displayName")
                        ?: userDoc.getString("email")
                        ?: uid.take(6)

                    // now get this user's runs
                    db.collection("users").document(uid)
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
                            val stepsApprox = (milesTotal * 1400.0).roundToInt()

                            results.add(
                                LeaderboardUserRow(
                                    uid = uid,
                                    name = name,
                                    steps = stepsApprox,
                                    miles = milesTotal
                                )
                            )
                            doneOne()
                        }
                        .addOnFailureListener {
                            // if runs query fails, still add row with 0s
                            results.add(
                                LeaderboardUserRow(
                                    uid = uid,
                                    name = name,
                                    steps = 0,
                                    miles = 0.0
                                )
                            )
                            doneOne()
                        }
                }
                .addOnFailureListener {
                    // total failure for this user → add minimal row
                    results.add(
                        LeaderboardUserRow(
                            uid = uid,
                            name = uid.take(6),
                            steps = 0,
                            miles = 0.0
                        )
                    )
                    doneOne()
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
