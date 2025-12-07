package com.example.raceme

import com.google.firebase.firestore.DocumentSnapshot

data class UserStats(
    val name: String = "",
    val steps: Int = 0,
    val distance: Double = 0.0
)
