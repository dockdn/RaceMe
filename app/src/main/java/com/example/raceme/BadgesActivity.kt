package com.example.raceme

import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import com.example.raceme.databinding.ActivityBadgesBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import com.example.raceme.models.RunPoint
import com.example.raceme.models.BadgeDef
import com.example.raceme.models.BadgeRow

class BadgesActivity : BaseActivity() {

    // view binding and adapters
    private lateinit var b: ActivityBadgesBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val tz by lazy { ZoneId.systemDefault() }
    private val adapter = BadgesAdapter()

    // lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val user = auth.currentUser ?: run { finish(); return }
        b = ActivityBadgesBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.rvBadges.layoutManager = GridLayoutManager(this, 2)
        b.rvBadges.adapter = adapter

        b.btnBackBadges.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
        }

        b.tvBadgesHeaderTitle.text = "Badges"
        b.tvBadgesTitle.text = "Badges"
        b.tvBadgesSubtitle.text = "Collect milestones as you run, race, and stay consistent."
    }

    // loading badges
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
                    RunPoint(
                        startedAtMs = ts.seconds * 1000 + ts.nanoseconds / 1_000_000,
                        distanceMiles = distMiles,
                        elapsedMs = d.getLong("elapsedMs") ?: 0L
                    )
                }
                val badges = evaluateBadges(runs)
                adapter.submit(badges)
                updateSummary(badges)
            }
            .addOnFailureListener {
                val badges = evaluateBadges(emptyList())
                adapter.submit(badges)
                updateSummary(badges)
            }
    }

    // summary text
    private fun updateSummary(badges: List<BadgeRow>) {
        val total = badges.size
        val earned = badges.count { it.earned }
        b.tvBadgesSummary.text = "You’ve unlocked $earned of $total badges so far. Keep going!"
    }

    // badge definitions
    private fun badgeDefs(): List<BadgeDef> = listOf(
        BadgeDef(
            id = "first_run",
            title = "First Run",
            desc = "Save your very first run in RaceMe.",
            emoji = "🏁",
            condition = "first_run"
        ),
        BadgeDef(
            id = "mile_one",
            title = "One Miler",
            desc = "Run at least 1.0 mile in a single run.",
            emoji = "1️⃣",
            condition = "distance_once",
            distanceMiles = 1.0
        ),
        BadgeDef(
            id = "run_5k",
            title = "5K Finisher",
            desc = "Crush a 3.1+ mile run.",
            emoji = "🥇",
            condition = "distance_once",
            distanceMiles = 3.1
        ),
        BadgeDef(
            id = "run_10k",
            title = "10K Hero",
            desc = "Run 6.2+ miles in one go.",
            emoji = "🏅",
            condition = "distance_once",
            distanceMiles = 6.2
        ),
        BadgeDef(
            id = "long_run_6",
            title = "Long Run Lover",
            desc = "Go the distance with a 6.0+ mile run.",
            emoji = "💪",
            condition = "distance_once",
            distanceMiles = 6.0
        ),
        BadgeDef(
            id = "half_hour_hero",
            title = "Half-Hour Hero",
            desc = "Finish a run of at least 30 minutes.",
            emoji = "⏱️",
            condition = "duration_once_30"
        ),
        BadgeDef(
            id = "hour_of_power",
            title = "Hour of Power",
            desc = "Finish a run of at least 60 minutes.",
            emoji = "🕒",
            condition = "duration_once_60"
        ),
        BadgeDef(
            id = "steady_20x5",
            title = "Steady Strider",
            desc = "Do 5 runs of 20+ minutes in the last 14 days.",
            emoji = "🎧",
            condition = "duration_20min_5runs"
        ),
        BadgeDef(
            id = "weekly_15",
            title = "Mileage Beast",
            desc = "Log 15+ miles in the last 7 days.",
            emoji = "💨",
            condition = "miles_last7",
            distanceMiles = 15.0
        ),
        BadgeDef(
            id = "ten_runs_30",
            title = "Ten-Run Titan",
            desc = "Complete 10 runs in the last 30 days.",
            emoji = "🔟",
            condition = "runs_last30"
        ),
        BadgeDef(
            id = "weekend_warrior",
            title = "Weekend Warrior",
            desc = "Run on 4 different weekend days in the last 4 weeks.",
            emoji = "🎉",
            condition = "weekend_warrior"
        ),
        BadgeDef(
            id = "early_bird",
            title = "Ultra Early Bird",
            desc = "Start any run before 6:00 AM.",
            emoji = "🌅",
            condition = "early_bird",
            beforeHour = 6
        ),
        BadgeDef(
            id = "early_bird_3",
            title = "Morning Momentum",
            desc = "Start 3 runs before 8:00 AM in the last 14 days.",
            emoji = "☕",
            condition = "early_bird_3",
            beforeHour = 8
        ),
        BadgeDef(
            id = "night_owl_3",
            title = "Night Owl Hustler",
            desc = "Start 3 runs after 8:00 PM in the last 14 days.",
            emoji = "🌙",
            condition = "night_owl_3"
        ),
        BadgeDef(
            id = "streak7",
            title = "Streak Queen",
            desc = "Run on 7 different days in the last 10.",
            emoji = "🔥",
            condition = "streak7"
        )
    )

    // evaluation logic
    private fun evaluateBadges(runs: List<RunPoint>): List<BadgeRow> {
        data class DecoratedRun(
            val miles: Double,
            val elapsedMs: Long,
            val date: LocalDate,
            val hour: Int
        )

        val decorated = runs.map { rp ->
            val zdt = Instant.ofEpochMilli(rp.startedAtMs).atZone(tz)
            DecoratedRun(
                miles = rp.distanceMiles,
                elapsedMs = rp.elapsedMs,
                date = zdt.toLocalDate(),
                hour = zdt.hour
            )
        }

        val dates = decorated.map { it.date }.toSet()
        val today = LocalDate.now(tz)

        val earnedFirst      = decorated.isNotEmpty()
        val earned1Mile      = decorated.any { it.miles >= 1.0 - 1e-6 }
        val earned5k         = decorated.any { it.miles >= 3.1 - 1e-6 }
        val earned10k        = decorated.any { it.miles >= 6.2 - 1e-6 }
        val earnedLong6      = decorated.any { it.miles >= 6.0 - 1e-6 }
        val earnedUltraEarly = decorated.any { it.hour < 6 }

        val earned30min = decorated.any { it.elapsedMs >= 30L * 60L * 1000L }
        val earned60min = decorated.any { it.elapsedMs >= 60L * 60L * 1000L }

        val cutoff14_forDuration = today.minusDays(13)
        val recentDuration14 = decorated.filter {
            !it.date.isBefore(cutoff14_forDuration) && !it.date.isAfter(today)
        }
        val long20minRuns14 = recentDuration14.count { it.elapsedMs >= 20L * 60L * 1000L }
        val earned20x5 = long20minRuns14 >= 5

        val cutoff7 = today.minusDays(6)
        val miles7 = decorated
            .filter { !it.date.isBefore(cutoff7) && !it.date.isAfter(today) }
            .sumOf { it.miles }
        val earnedWeekly15 = miles7 >= 15.0 - 1e-6

        val cutoff30 = today.minusDays(29)
        val runs30 = decorated
            .count { !it.date.isBefore(cutoff30) && !it.date.isAfter(today) }
        val earnedTenRuns30 = runs30 >= 10

        val cutoff28 = today.minusDays(27)
        val weekendDays = decorated
            .filter { !it.date.isBefore(cutoff28) && !it.date.isAfter(today) }
            .filter {
                it.date.dayOfWeek == DayOfWeek.SATURDAY ||
                        it.date.dayOfWeek == DayOfWeek.SUNDAY
            }
            .map { it.date }
            .toSet()
            .size
        val earnedWeekendWarrior = weekendDays >= 4

        val cutoff14 = today.minusDays(13)
        val recent14 = decorated.filter {
            !it.date.isBefore(cutoff14) && !it.date.isAfter(today)
        }

        val earlyRuns14 = recent14.count { it.hour < 8 }
        val nightRuns14 = recent14.count { it.hour >= 20 }

        val earnedEarly3 = earlyRuns14 >= 3
        val earnedNight3 = nightRuns14 >= 3

        val cutoff10 = today.minusDays(9)
        val distinctRecent = dates.count {
            !it.isBefore(cutoff10) && !it.isAfter(today)
        }
        val earnedStreak7 = distinctRecent >= 7

        val earnedMap = mapOf(
            "first_run"            to earnedFirst,
            "distance_once:1.0"    to earned1Mile,
            "distance_once:3.1"    to earned5k,
            "distance_once:6.2"    to earned10k,
            "distance_once:6.0"    to earnedLong6,
            "early_bird"           to earnedUltraEarly,
            "duration_once_30"     to earned30min,
            "duration_once_60"     to earned60min,
            "duration_20min_5runs" to earned20x5,
            "miles_last7:15.0"     to earnedWeekly15,
            "runs_last30"          to earnedTenRuns30,
            "weekend_warrior"      to earnedWeekendWarrior,
            "early_bird_3"         to earnedEarly3,
            "night_owl_3"          to earnedNight3,
            "streak7"              to earnedStreak7
        )

        // return badges to user

        return badgeDefs().map { def ->
            val key = when (def.condition) {
                "distance_once" -> "distance_once:${def.distanceMiles ?: 0.0}"
                "miles_last7"   -> "miles_last7:${def.distanceMiles ?: 0.0}"
                else            -> def.condition
            }
            BadgeRow(def, earnedMap[key] == true)
        }
    }
}
