package com.example.raceme

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.raceme.databinding.ActivityRunsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RunsActivity : AppCompatActivity() {
    private lateinit var b: ActivityRunsBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val adapter = RunsAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityRunsBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.rvRuns.layoutManager = LinearLayoutManager(this)
        b.rvRuns.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        val uid = auth.currentUser?.uid ?: run { finish(); return }
        db.collection("users").document(uid).collection("runs")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snap ->
                val rows = snap.documents.map { d ->
                    val name = d.getString("name").orEmpty().ifBlank { "Run" }
                    val miles = d.getDouble("distanceMiles") ?: ((d.getDouble("distanceMeters") ?: 0.0) / 1609.344)
                    val pace = d.getString("paceMinPerMile").orEmpty()
                    val rating = (d.getLong("rating") ?: 0L).toInt()
                    val ts = d.getTimestamp("startedAt")?.toDate()
                    val whenStr = if (ts != null) android.text.format.DateFormat.format("MMM d, h:mma", ts).toString() else ""
                    val quote = d.getString("quote").orEmpty()
                    RunRow(name = name, whenText = whenStr, miles = miles, pace = pace, rating = rating, quote = quote)
                }
                adapter.submit(rows)
                b.tvEmpty.visibility = if (rows.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            }
            .addOnFailureListener {
                adapter.submit(emptyList())
                b.tvEmpty.visibility = android.view.View.VISIBLE
            }
    }
}

data class RunRow(
    val name: String,
    val whenText: String,
    val miles: Double,
    val pace: String,
    val rating: Int,
    val quote: String
)
