package com.example.raceme.ui.badges

data class Badge(
    val id: String,
    val name: String,
    val emoji: String = "",
    val earned: Boolean = false,
    val colorRes: Int? = null
)
