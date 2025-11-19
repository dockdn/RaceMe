package com.example.raceme

// Represents a friend request in Firestore
data class FriendRequest(
    val id: String = "",
    val fromUid: String = "",
    val fromEmail: String = "",
    val toUid: String = "",
    val toEmail: String = "",
    val status: String = "pending"
)