package com.example.raceme.models

data class BadgeDef(
    val id: String,
    val title: String,
    val desc: String,
    val emoji: String,
    val condition: String,
    val distanceMiles: Double? = null,
    val beforeHour: Int? = null
)

data class BadgeRow(
    val def: BadgeDef,
    val earned: Boolean
)
