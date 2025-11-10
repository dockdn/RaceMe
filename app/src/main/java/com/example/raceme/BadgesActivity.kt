package com.example.raceme

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.raceme.databinding.ActivityBadgesBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import com.example.raceme.models.RunPoint
import com.example.raceme.models.BadgeDef
import com.example.raceme.models.BadgeRow

class BadgesActivity : AppCompatActivity() {
    private lateinit var b: ActivityBadgesBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val tz by lazy { ZoneId.systemDefault() }
    private val adapter = BadgesAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val user = auth.currentUser ?: run { finish(); return }
        b = ActivityBadgesBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.rvBadges.layoutManager = GridLayoutManager(this, 2)
        b.rvBadges.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        val uid = auth.currentUser?.uid ?: run { finish(); return }
        db.collection("users").document(uid).collection("runs")
            .get()
            .addOnSuccessListener { snap ->
                val runs = snap.documents.mapNotNull { d ->
                    val ts = d.getTimestamp("startedAt") ?: return@mapNotNull null
                    val distMiles = d.getDouble("distanceMiles")
                        ?: (d.getDouble("distanceMeters")?.let { it / 1609.344 } ?: 0.0)
                    RunPoint(ts.seconds * 1000 + ts.nanoseconds / 1_000_000, distMiles, d.getLong("elapsedMs") ?: 0L)
                }
                adapter.submit(evaluateBadges(runs))
            }
            .addOnFailureListener { adapter.submit(evaluateBadges(emptyList())) }
    }

    private fun badgeDefs(): List<BadgeDef> = listOf(
        BadgeDef("first_run", "First Run", "Save your first run", "🏁", "first_run"),
        BadgeDef("mile_one", "One Miler", "Run ≥ 1 mile once", "1️⃣", "distance_once", distanceMiles = 1.0),
        BadgeDef("run_5k", "5K Finisher", "Run ≥ 3.1 miles once", "🥇", "distance_once", distanceMiles = 3.1),
        BadgeDef("run_10k", "10K Finisher", "Run ≥ 6.2 miles once", "🏅", "distance_once", distanceMiles = 6.2),
        BadgeDef("early_bird", "Early Bird", "Start a run before 6am", "🌅", "early_bird", beforeHour = 6),
        BadgeDef("streak7", "7-Day Streak", "Run on 7 different days in last 10", "🔥", "streak7")
    )

    private fun evaluateBadges(runs: List<RunPoint>): List<BadgeRow> {
        val dates = runs.map { LocalDate.ofInstant(Instant.ofEpochMilli(it.startedAtMs), tz) }.toSet()

        val earnedFirst = runs.isNotEmpty()
        val earned1     = runs.any { it.distanceMiles >= 1.0 - 1e-6 }
        val earned5k    = runs.any { it.distanceMiles >= 3.1 - 1e-6 }
        val earned10k   = runs.any { it.distanceMiles >= 6.2 - 1e-6 }
        val earnedEarly = runs.any { Instant.ofEpochMilli(it.startedAtMs).atZone(tz).hour < 6 }

        val today = LocalDate.now(tz)
        val cutoff = today.minusDays(9)
        val distinctRecent = dates.count { !it.isBefore(cutoff) }
        val earnedStreak = distinctRecent >= 7

        val earnedMap = mapOf(
            "first_run" to earnedFirst,
            "distance_once:1.0" to earned1,
            "distance_once:3.1" to earned5k,
            "distance_once:6.2" to earned10k,
            "early_bird" to earnedEarly,
            "streak7" to earnedStreak
        )

        return badgeDefs().map { def ->
            val key = if (def.condition == "distance_once")
                "distance_once:${def.distanceMiles ?: 0.0}"
            else
                def.condition
            BadgeRow(def, earnedMap[key] == true)
        }
    }
}
