package com.example.raceme

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class RaceEvent(
    @DocumentId
    var id: String = "",                // Firestore document ID

    var title: String = "",
    var description: String = "",

    // What you'll show on the card as the address
    var locationName: String = "",

    // If you ever decide to split address, they can stay blank
    var addressLine1: String = "",
    var city: String = "",
    var state: String = "",

    // When the event happens
    var startTime: Timestamp? = null,

    // People who tapped "Interested"
    var interestedUserIds: List<String> = emptyList(),

    // Creator info (optional but nice to have)
    var createdByUid: String = "",
    var createdByName: String = ""
)
