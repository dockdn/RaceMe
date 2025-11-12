package com.example.raceme

// Represents a friend request in Firestore
data class FriendRequest(
    val id: String = "",       // Firestore document ID
    val fromUid: String = "",  // UID of the user who sent the request
    val toUid: String = "",    // UID of the user receiving the request
    var status: String = "pending"  // Status: "pending", "accepted", or "rejected"
)
