package com.example.raceme

data class ChallengeDef(
    val id: String,
    val title: String,
    val desc: String,
    val distanceMiles: Double? = null,
    val days: Int? = null,
    val weekends: Int? = null
)

data class ChallengeRow(
    val def: ChallengeDef,
    val progressLabel: String,
    val subLabel: String,
    val progressPercent: Int,
    val earned: Boolean
)

/**
 * Catalog of all defined challenges.
 *
 * NOTE: The actual logic for each id lives in ChallengesActivity.
 */
object ChallengeCatalog {
    val defs = listOf(
        // --- Existing ones ---

        ChallengeDef(
            id = "run5k",
            title = "Run 5K",
            desc = "Finish a 5 km run",
            distanceMiles = 3.10
        ),
        ChallengeDef(
            id = "streak7",
            title = "7-Day Streak",
            desc = "Run every day for a week",
            days = 7
        ),
        ChallengeDef(
            id = "weekendWarrior",
            title = "Weekend Warrior",
            desc = "Run on weekends (last 4)",
            weekends = 4
        ),
        ChallengeDef(
            id = "miles30",
            title = "30-Day Miles",
            desc = "Accumulate 20 miles in last 30 days",
            distanceMiles = 20.0,
            days = 30
        ),

        // --- NEW challenges ---

        // Long single run
        ChallengeDef(
            id = "longRun6",
            title = "Long Run – 6 Miles",
            desc = "Complete one run of at least 6.0 miles",
            distanceMiles = 6.0
        ),

        // Total miles in a shorter window
        ChallengeDef(
            id = "fifteenMiles7",
            title = "15 Miles / 7 Days",
            desc = "Log 15 miles in the last 7 days",
            distanceMiles = 15.0,
            days = 7
        ),

        // Activity count challenge
        ChallengeDef(
            id = "tenRuns30",
            title = "10 Runs / 30 Days",
            desc = "Complete 10 runs in the last 30 days",
            days = 30
        ),

        // Time-of-day challenges
        ChallengeDef(
            id = "earlyBird3",
            title = "Early Bird",
            desc = "Finish 3 runs that start before 8:00 AM (last 14 days)",
            days = 14
        ),
        ChallengeDef(
            id = "nightOwl3",
            title = "Night Owl",
            desc = "Finish 3 runs that start after 8:00 PM (last 14 days)",
            days = 14
        )
    )
}
