package com.example.raceme.models

data class BadgeDef(
    val id: String,
    val title: String,
    val desc: String,
    val emoji: String,            // e.g. "🏁"
    val condition: String,        // e.g. "first_run", "distance_once", "early_bird", "streak7"
    val distanceMiles: Double? = null, // used when condition == "distance_once"
    val beforeHour: Int? = null        // used when condition == "early_bird"
)

data class BadgeRow(
    val def: BadgeDef,
    val earned: Boolean
)
