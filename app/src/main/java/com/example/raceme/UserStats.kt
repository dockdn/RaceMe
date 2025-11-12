package com.example.raceme

import com.google.firebase.firestore.DocumentSnapshot

/**
 * Simple model for leaderboard entries.
 * - uid: Firestore document id for the user (kept as string)
 * - name: displayName from Firestore (fallback to uid prefix)
 * - steps: integer step count (use 0 if missing)
 * - distance: meters as double (use 0.0 if missing)
 */

data class UserStats(
    val name: String = "",
    val steps: Int = 0,
    val distance: Double = 0.0
)
