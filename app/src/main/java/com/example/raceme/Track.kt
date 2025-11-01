package com.example.raceme

import com.google.firebase.Timestamp

data class Track(
    val title: String = "",
    val distanceMiles: Double = 0.0,
    val durationSeconds: Long = 0,
    val createdAt: Timestamp = Timestamp.now(),
    val userId: String = "",
    val public: Boolean = true
)
