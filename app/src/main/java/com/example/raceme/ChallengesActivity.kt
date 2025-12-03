package com.example.raceme

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.raceme.databinding.ActivityChallengesBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.Date
import java.util.Calendar
import kotlin.math.min
import kotlin.math.roundToInt

class ChallengesActivity : BaseActivity() {

    // view + firebase + adapter

    private lateinit var b: ActivityChallengesBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val adapter by lazy { ChallengeAdapter() }

    // lifecycle: onCreate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityChallengesBinding.inflate(layoutInflater)
        setContentView(b.root)

        // header back button

        b.btnBackChallenges.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
        }

        // header labels

        b.tvChallengesHeaderTitle.text = "Challenges"
        b.tvChallengesTitle.text = "Challenges"
        b.tvChallengesSubtitle.text = "Track streaks, mileage, and weekend hustle."

        // recycler setup

        b.rvChallenges.layoutManager = LinearLayoutManager(this)
        b.rvChallenges.adapter = adapter

        // initial placeholder rows

        adapter.submit(
            ChallengeCatalog.defs.map { def ->
                ChallengeRow(
                    def = def,
                    progressLabel = when (def.id) {
                        "run5k"          -> "0.00/3.10 mi"
                        "streak7"        -> "0/7 days"
                        "weekendWarrior" -> "0/4 weekend days"
                        "miles30"        -> "0/20 mi"
                        "longRun6"       -> "0.00/6.00 mi"
                        "fifteenMiles7"  -> "0.0/15 mi"
                        "tenRuns30"      -> "0/10 runs"
                        "earlyBird3"     -> "0/3 early runs"
                        "nightOwl3"      -> "0/3 night runs"
                        else             -> "—"
                    },
                    subLabel = when (def.id) {
                        "streak7"        -> "Last 7 days"
                        "weekendWarrior" -> "Last 4 weekends"
                        "miles30"        -> "Last 30 days"
                        "longRun6"       -> "Single longest run"
                        "fifteenMiles7"  -> "Last 7 days"
                        "tenRuns30"      -> "Last 30 days"
                        "earlyBird3"     -> "Last 14 days"
                        "nightOwl3"      -> "Last 14 days"
                        else             -> ""
                    },
                    progressPercent = 0,
                    earned = false
                )
            }
        )

        // load real data

        loadAndCompute()
    }

    // load data from Firestore and compute challenge progress

    private fun loadAndCompute() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "Not signed in — showing placeholders.", Toast.LENGTH_LONG).show()
            updateSummary(emptyList())
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
                        "run5k"          -> computeRun5k(def, runs)
                        "streak7"        -> computeStreak7(def, runs)
                        "weekendWarrior" -> computeWeekendWarrior(def, runs)
                        "miles30"        -> computeMiles30(def, runs)
                        "longRun6"       -> computeLongRun6(def, runs)
                        "fifteenMiles7"  -> computeFifteenMiles7(def, runs)
                        "tenRuns30"      -> computeTenRuns30(def, runs)
                        "earlyBird3"     -> computeEarlyBird3(def, runs)
                        "nightOwl3"      -> computeNightOwl3(def, runs)
                        else             -> ChallengeRow(def, "—", "", 0, earned = false)
                    }
                }

                adapter.submit(rows)
                updateSummary(rows)
            }
            .addOnFailureListener { e ->
                Log.e("Challenges", "Load failed", e)
                Toast.makeText(this, e.message ?: "Failed to load challenges", Toast.LENGTH_LONG).show()
                updateSummary(emptyList())
            }
    }

    // update header summary ("X of Y challenges completed")

    private fun updateSummary(rows: List<ChallengeRow>) {
        val total = ChallengeCatalog.defs.size
        val earned = rows.count { it.earned }
        b.tvChallengesSummary.text = "You’ve completed $earned of $total challenges. Keep going!"
    }

    // compute: 5K challenge

    private fun computeRun5k(def: ChallengeDef, runs: List<RunRow>): ChallengeRow {
        val target = def.distanceMiles ?: 3.10
        val best = runs.maxOfOrNull { it.miles } ?: 0.0
        val pct = ((best / target) * 100.0).coerceIn(0.0, 100.0).roundToInt()
        val progress = String.format("%.2f/%.2f mi", min(best, target), target)
        return ChallengeRow(
            def = def,
            progressLabel = progress,
            subLabel = "Best single run",
            progressPercent = pct,
            earned = best >= target - 1e-6
        )
    }

    // compute: 7-day streak challenge

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
        val pct = ((uniqueDays / target.toDouble()) * 100.0).coerceIn(0.0, 100.0).roundToInt()
        val progress = "$uniqueDays/$target days"

        return ChallengeRow(
            def = def,
            progressLabel = progress,
            subLabel = "Last 7 days",
            progressPercent = pct,
            earned = uniqueDays >= target
        )
    }

    // compute: weekend warrior challenge

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
        val pct = ((weekendDays / target.toDouble()) * 100.0).coerceIn(0.0, 100.0).roundToInt()
        val progress = "$weekendDays/$target weekend days"

        return ChallengeRow(
            def = def,
            progressLabel = progress,
            subLabel = "Last 4 weekends",
            progressPercent = pct,
            earned = weekendDays >= target
        )
    }

    // compute: 30-day mileage challenge

    private fun computeMiles30(def: ChallengeDef, runs: List<RunRow>): ChallengeRow {
        val cal = Calendar.getInstance()
        val end = cal.time
        cal.add(Calendar.DAY_OF_YEAR, -29)
        val start = cal.time

        val totalMiles = runs.filter { it.date in start..end }.sumOf { it.miles }
        val target = def.distanceMiles ?: 20.0
        val pct = ((totalMiles / target) * 100.0).coerceIn(0.0, 100.0).roundToInt()
        val progress = String.format("%.1f/%.0f mi", min(totalMiles, target), target)

        return ChallengeRow(
            def = def,
            progressLabel = progress,
            subLabel = "Last 30 days",
            progressPercent = pct,
            earned = totalMiles >= target - 1e-6
        )
    }

    // compute: long run challenge (6 miles once)

    private fun computeLongRun6(def: ChallengeDef, runs: List<RunRow>): ChallengeRow {
        val target = def.distanceMiles ?: 6.0
        val best = runs.maxOfOrNull { it.miles } ?: 0.0
        val pct = ((best / target) * 100.0).coerceIn(0.0, 100.0).roundToInt()
        val progress = String.format("%.2f/%.2f mi", min(best, target), target)

        return ChallengeRow(
            def = def,
            progressLabel = progress,
            subLabel = "Single longest run",
            progressPercent = pct,
            earned = best >= target - 1e-6
        )
    }

    // compute: 15 miles in last 7 days

    private fun computeFifteenMiles7(def: ChallengeDef, runs: List<RunRow>): ChallengeRow {
        val cal = Calendar.getInstance()
        val end = cal.time
        cal.add(Calendar.DAY_OF_YEAR, -6)
        val start = cal.time

        val totalMiles = runs
            .filter { it.date in start..end }
            .sumOf { it.miles }

        val target = def.distanceMiles ?: 15.0
        val pct = ((totalMiles / target) * 100.0)
            .coerceIn(0.0, 100.0)
            .roundToInt()

        val progress = String.format("%.1f/%.0f mi", min(totalMiles, target), target)

        return ChallengeRow(
            def = def,
            progressLabel = progress,
            subLabel = "Last 7 days",
            progressPercent = pct,
            earned = totalMiles >= target - 1e-6
        )
    }

    // compute: 10 runs in last 30 days

    private fun computeTenRuns30(def: ChallengeDef, runs: List<RunRow>): ChallengeRow {
        val cal = Calendar.getInstance()
        val end = cal.time
        cal.add(Calendar.DAY_OF_YEAR, -29)
        val start = cal.time

        val runCount = runs
            .filter { it.date in start..end }
            .size

        val target = 10
        val pct = ((runCount / target.toDouble()) * 100.0)
            .coerceIn(0.0, 100.0)
            .roundToInt()

        val progress = "$runCount/$target runs"

        return ChallengeRow(
            def = def,
            progressLabel = progress,
            subLabel = "Last 30 days",
            progressPercent = pct,
            earned = runCount >= target
        )
    }

    // compute: 3 early runs (before 8am) in last 14 days

    private fun computeEarlyBird3(def: ChallengeDef, runs: List<RunRow>): ChallengeRow {
        val cal = Calendar.getInstance()
        val end = cal.time
        cal.add(Calendar.DAY_OF_YEAR, -13)
        val start = cal.time

        var earlyCount = 0

        runs.filter { it.date in start..end }.forEach { run ->
            val c = Calendar.getInstance().apply { time = run.date }
            val hour = c.get(Calendar.HOUR_OF_DAY)
            if (hour < 8) {
                earlyCount++
            }
        }

        val target = 3
        val pct = ((earlyCount / target.toDouble()) * 100.0)
            .coerceIn(0.0, 100.0)
            .roundToInt()

        val progress = "$earlyCount/$target early runs"

        return ChallengeRow(
            def = def,
            progressLabel = progress,
            subLabel = "Last 14 days",
            progressPercent = pct,
            earned = earlyCount >= target
        )
    }

    // compute: 3 night runs (after 8pm) in last 14 days

    private fun computeNightOwl3(def: ChallengeDef, runs: List<RunRow>): ChallengeRow {
        val cal = Calendar.getInstance()
        val end = cal.time
        cal.add(Calendar.DAY_OF_YEAR, -13)
        val start = cal.time

        var nightCount = 0

        runs.filter { it.date in start..end }.forEach { run ->
            val c = Calendar.getInstance().apply { time = run.date }
            val hour = c.get(Calendar.HOUR_OF_DAY)
            if (hour >= 20) {
                nightCount++
            }
        }

        val target = 3
        val pct = ((nightCount / target.toDouble()) * 100.0)
            .coerceIn(0.0, 100.0)
            .roundToInt()

        val progress = "$nightCount/$target night runs"

        return ChallengeRow(
            def = def,
            progressLabel = progress,
            subLabel = "Last 14 days",
            progressPercent = pct,
            earned = nightCount >= target
        )
    }

    // helpers: weekend + day key

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

    // local run row model

    private data class RunRow(val miles: Double, val elapsedMs: Long, val date: Date)
}
