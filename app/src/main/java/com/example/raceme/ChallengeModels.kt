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

object ChallengeCatalog {
    val defs = listOf(
        ChallengeDef("run5k", "Run 5K", "Finish a 5 km run", distanceMiles = 3.10),
        ChallengeDef("streak7", "7-Day Streak", "Run every day for a week", days = 7),
        ChallengeDef("weekendWarrior", "Weekend Warrior", "Run on weekends (last 4)", weekends = 4),
        ChallengeDef("miles30", "30-Day Miles", "Accumulate 20 miles in last 30 days", distanceMiles = 20.0)
    )
}
