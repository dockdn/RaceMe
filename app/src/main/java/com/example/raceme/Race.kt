package com.example.raceme

import com.google.firebase.Timestamp

data class Race(
    val title: String = "",
    val type: String = "",              // e.g., "Marathon", "5K", "10K", "Trail", etc.
    val lapsPerMile: Double = 0.0,      // how many laps per mile
    val description: String = "",
    val public: Boolean = true,         // privacy toggle
    val imageUrl: String = "",          // Firebase Storage download URL
    val userId: String = "",
    val createdAt: Timestamp = Timestamp.now()
)
