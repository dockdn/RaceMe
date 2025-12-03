package com.example.raceme

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.raceme.databinding.ActivityRunsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot

class RunsActivity : AppCompatActivity() {

    private lateinit var b: ActivityRunsBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private lateinit var adapter: RunsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityRunsBinding.inflate(layoutInflater)
        setContentView(b.root)

        // toolbar back button
        b.btnBack.setOnClickListener { finish() }

        // set up RecyclerView and adapter
        adapter = RunsAdapter { row ->
            confirmDelete(row)
        }
        b.rvRuns.layoutManager = LinearLayoutManager(this)
        b.rvRuns.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        loadRuns()
    }

    // load all runs for current user and update list
    private fun loadRuns() {
        val uid = auth.currentUser?.uid ?: run { finish(); return }

        db.collection("users").document(uid).collection("runs")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snap: QuerySnapshot ->
                val rows = snap.documents.map { d ->
                    val name = d.getString("name").orEmpty().ifBlank { "Run" }
                    val miles = d.getDouble("distanceMiles")
                        ?: ((d.getDouble("distanceMeters") ?: 0.0) / 1609.344)
                    val pace = d.getString("paceMinPerMile").orEmpty()
                    val rating = (d.getLong("rating") ?: 0L).toInt()
                    val ts = d.getTimestamp("startedAt")?.toDate()
                    val whenStr = if (ts != null) {
                        android.text.format.DateFormat.format("MMM d, h:mma", ts).toString()
                    } else {
                        ""
                    }
                    val quote = d.getString("quote").orEmpty()

                    RunRow(
                        id = d.id,
                        name = name,
                        whenText = whenStr,
                        miles = miles,
                        pace = pace,
                        rating = rating,
                        quote = quote
                    )
                }

                adapter.submit(rows)
                b.tvEmpty.visibility =
                    if (rows.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            }
            .addOnFailureListener {
                adapter.submit(emptyList())
                b.tvEmpty.visibility = android.view.View.VISIBLE
                Toast.makeText(this, "Failed to load runs", Toast.LENGTH_SHORT).show()
            }
    }

    // show confirmation dialog before deleting a run
    private fun confirmDelete(row: RunRow) {
        AlertDialog.Builder(this)
            .setTitle("Delete run?")
            .setMessage("Remove \"${row.name}\" from your history?")
            .setPositiveButton("Delete") { _, _ -> deleteRun(row) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // delete selected run document from Firestore
    private fun deleteRun(row: RunRow) {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid).collection("runs")
            .document(row.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Run deleted", Toast.LENGTH_SHORT).show()
                loadRuns()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Delete failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}

// simple row model used by the adapter
data class RunRow(
    val id: String,
    val name: String,
    val whenText: String,
    val miles: Double,
    val pace: String,
    val rating: Int,
    val quote: String
)
