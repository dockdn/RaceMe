package com.example.raceme.ui.badges

import com.example.raceme.R

object BadgeRepo {
    fun all(): List<Badge> = listOf(
        Badge("first_run",       "First Run",         "🏁", colorRes = R.color.badge_yellow),
        Badge("mile_one",        "One Miler",         "1️⃣", colorRes = R.color.badge_blue),
        Badge("run_5k",          "5K Finisher",       "🥇", colorRes = R.color.badge_orange),
        Badge("run_10k",         "10K Hero",          "🏅", colorRes = R.color.badge_purple),
        Badge("long_run_6",      "Long Run Lover",    "💪", colorRes = R.color.badge_green),

        // Time-based
        Badge("half_hour_hero",  "Half-Hour Hero",    "⏱️", colorRes = R.color.badge_blue),
        Badge("hour_of_power",   "Hour of Power",     "🕒", colorRes = R.color.badge_purple),
        Badge("steady_20x5",     "Steady Strider",    "🎧", colorRes = R.color.badge_green),

        // Volume / consistency
        Badge("weekly_15",       "Mileage Beast",     "💨", colorRes = R.color.badge_blue),
        Badge("ten_runs_30",     "Ten-Run Titan",     "🔟", colorRes = R.color.badge_green),
        Badge("weekend_warrior", "Weekend Warrior",   "🎉", colorRes = R.color.badge_orange),

        // Time-of-day + streak
        Badge("early_bird",      "Ultra Early Bird",  "🌅", colorRes = R.color.badge_yellow),
        Badge("early_bird_3",    "Morning Momentum",  "☕", colorRes = R.color.badge_yellow),
        Badge("night_owl_3",     "Night Owl Hustler", "🌙", colorRes = R.color.badge_purple),
        Badge("streak7",         "Streak Queen",      "🔥", colorRes = R.color.badge_orange)
    )
}
