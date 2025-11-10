package com.example.raceme

import com.google.firebase.Timestamp

data class Race(
    val title: String = "",
    val type: String = "",
    val lapsPerMile: Double = 0.0,
    val description: String = "",
    val public: Boolean = true,
    val imageUrl: String = "",
    val userId: String = "",
    val createdAt: Timestamp = Timestamp.now()
)
