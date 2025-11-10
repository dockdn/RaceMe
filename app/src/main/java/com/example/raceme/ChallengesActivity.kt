package com.example.raceme

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.raceme.databinding.ActivityChallengesBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.*
import kotlin.math.min
import kotlin.math.roundToInt

class ChallengesActivity : BaseActivity() {
    private lateinit var b: ActivityChallengesBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val adapter by lazy { ChallengeAdapter() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityChallengesBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.rvChallenges.layoutManager = LinearLayoutManager(this)
        b.rvChallenges.adapter = adapter

        adapter.submit(ChallengeCatalog.defs.map { def ->
            ChallengeRow(
                def = def,
                progressLabel = when (def.id) {
                    "run5k" -> "0.00/3.10 mi"
                    "streak7" -> "0/7 days"
                    "weekendWarrior" -> "0/4 weekend days"
                    "miles30" -> "0/20 mi"
                    else -> "—"
                },
                subLabel = when (def.id) {
                    "streak7" -> "Last 7 days"
                    "weekendWarrior" -> "Last 4 weekends"
                    "miles30" -> "Last 30 days"
                    else -> ""
                },
                progressPercent = 0,
                earned = false
            )
        })

        loadAndCompute()
    }

    private fun loadAndCompute() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "Not signed in — showing placeholders.", Toast.LENGTH_LONG).show()
            return
        }

        val endCal = Calendar.getInstance()
        val startCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -60) }
        val startTs = Timestamp(startCal.timeInMillis / 1000, 0)
        val endTs   = Timestamp(endCal.timeInMillis / 1000, 0)

        db.collection("users").document(uid).collection("runs")
            .whereGreaterThanOrEqualTo("startedAt", startTs)
            .whereLessThanOrEqualTo("startedAt", endTs)
            .orderBy("startedAt", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { snap ->
                val runs = snap.documents.mapNotNull { d ->
                    val miles = d.getDouble("distanceMiles") ?: 0.0
                    val elapsedMs = d.getLong("elapsedMs") ?: 0L
                    val ts = d.getTimestamp("startedAt") ?: return@mapNotNull null
                    RunRow(miles, elapsedMs, ts.toDate())
                }

                val rows = ChallengeCatalog.defs.map { def ->
                    when (def.id) {
                        "run5k"           -> computeRun5k(def, runs)
                        "streak7"         -> computeStreak7(def, runs)
                        "weekendWarrior"  -> computeWeekendWarrior(def, runs)
                        "miles30"         -> computeMiles30(def, runs)
                        else              -> ChallengeRow(def, "—", "", 0, earned = false)
                    }
                }
                adapter.submit(rows)
            }
            .addOnFailureListener { e ->
                Log.e("Challenges", "Load failed", e)
                Toast.makeText(this, e.message ?: "Failed to load challenges", Toast.LENGTH_LONG).show()
            }
    }

    private fun computeRun5k(def: ChallengeDef, runs: List<RunRow>): ChallengeRow {
        val target = def.distanceMiles ?: 3.10
        val best = runs.maxOfOrNull { it.miles } ?: 0.0
        val pct = ((best / target) * 100.0).coerceIn(0.0, 100.0).roundToInt()
        val progress = String.format("%.2f/%.2f mi", min(best, target), target)
        return ChallengeRow(def, progress, "Best single run", pct, earned = best >= target - 1e-6)
    }

    private fun computeStreak7(def: ChallengeDef, runs: List<RunRow>): ChallengeRow {
        val cal = Calendar.getInstance()
        val end = cal.time
        cal.add(Calendar.DAY_OF_YEAR, -6)
        val start = cal.time

        val uniqueDays = runs.filter { it.date in start..end }
            .map { dayKey(it.date) }
            .toSet()
            .size

        val target = def.days ?: 7
        val pct = ((uniqueDays / target.toDouble()) * 100).coerceIn(0.0, 100.0).roundToInt()
        val progress = "$uniqueDays/$target days"
        return ChallengeRow(def, progress, "Last 7 days", pct, earned = uniqueDays >= target)
    }

    private fun computeWeekendWarrior(def: ChallengeDef, runs: List<RunRow>): ChallengeRow {
        val cal = Calendar.getInstance()
        val end = cal.time
        cal.add(Calendar.WEEK_OF_YEAR, -4)
        val start = cal.time

        val weekendDays = runs.filter { it.date in start..end }
            .filter { isWeekend(it.date) }
            .map { dayKey(it.date) }
            .toSet()
            .size

        val target = def.weekends ?: 4
        val pct = ((weekendDays / target.toDouble()) * 100).coerceIn(0.0, 100.0).roundToInt()
        val progress = "$weekendDays/$target weekend days"
        return ChallengeRow(def, progress, "Last 4 weekends", pct, earned = weekendDays >= target)
    }

    private fun computeMiles30(def: ChallengeDef, runs: List<RunRow>): ChallengeRow {
        val cal = Calendar.getInstance()
        val end = cal.time
        cal.add(Calendar.DAY_OF_YEAR, -29)
        val start = cal.time

        val totalMiles = runs.filter { it.date in start..end }.sumOf { it.miles }
        val target = def.distanceMiles ?: 20.0
        val pct = ((totalMiles / target) * 100.0).coerceIn(0.0, 100.0).roundToInt()
        val progress = String.format("%.1f/%.0f mi", min(totalMiles, target), target)
        return ChallengeRow(def, progress, "Last 30 days", pct, earned = totalMiles >= target - 1e-6)
    }

    private fun isWeekend(date: Date): Boolean {
        val c = Calendar.getInstance().apply { time = date }
        val dow = c.get(Calendar.DAY_OF_WEEK)
        return dow == Calendar.SATURDAY || dow == Calendar.SUNDAY
    }

    private fun dayKey(date: Date): String {
        val c = Calendar.getInstance().apply { time = date }
        return "%04d-%02d-%02d".format(
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH) + 1,
            c.get(Calendar.DAY_OF_MONTH)
        )
    }

    private data class RunRow(val miles: Double, val elapsedMs: Long, val date: Date)
}
