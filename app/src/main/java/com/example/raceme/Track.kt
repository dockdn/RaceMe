package com.example.raceme

import com.google.firebase.Timestamp

data class Track(
    val id: String = "",
    val name: String = "",
    val type: String = "",
    val distanceMiles: Double = 0.0,
    val addressText: String? = null,
    val public: Boolean = true,
    val createdBy: String? = null,
    val createdAt: Timestamp? = null
)
