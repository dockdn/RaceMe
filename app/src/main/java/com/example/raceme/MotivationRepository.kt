package com.example.raceme

data class UserMotivationStats(
    val loginCount: Long,
    val challengesCompleted: Long
)

object MotivationRepository {

    fun getQuoteFor(stats: UserMotivationStats): String {
        val (logins, challenges) = stats

        return when {
            // 🏆 Many challenges completed
            challenges >= 15 -> {
                "You don’t just run races — you set the pace. 🏆"
            }
            challenges >= 8 -> {
                "Challenges fear you now. Keep stacking those wins. 🔥"
            }
            challenges >= 3 -> {
                "Every challenge you finish rewires your limits. Keep going. 💪"
            }

            // 📈 Frequent logins
            logins >= 20 -> {
                "You keep showing up. That’s how champions are built. ⭐"
            }
            logins >= 10 -> {
                "Consistency unlocked. Your future self is already proud. ✨"
            }
            logins >= 5 -> {
                "You’re building a habit one login at a time. Keep it up. 🌱"
            }

            // 🌱 Early users
            logins >= 1 -> {
                "Every session counts. Tiny steps → big results. 🚶‍♀️"
            }
            else -> {
                "Stay consistent. Small steps win races. 🏁"
            }
        }
    }
}
