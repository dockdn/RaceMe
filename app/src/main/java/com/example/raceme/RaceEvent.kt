package com.example.raceme

import com.google.firebase.Timestamp

data class RaceEvent(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val locationName: String = "",
    val startTime: Timestamp? = null,
    val createdByUserId: String = "",
    val createdAt: Timestamp? = null,
    val interestedUserIds: List<String> = emptyList()
)
