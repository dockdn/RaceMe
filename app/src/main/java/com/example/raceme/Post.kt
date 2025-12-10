package com.example.raceme.models

import com.google.firebase.Timestamp

data class Post(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val text: String = "",
    val imageBase64: String? = null,
    val createdAt: Timestamp? = null,
    val likesCount: Long = 0L
)
