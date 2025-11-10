package com.example.raceme.ui.badges

import com.example.raceme.R

object BadgeRepo {
    fun all(): List<Badge> = listOf(
        Badge("first_run",   "First Run",        "🏁", earned = true,  colorRes = R.color.badge_yellow),
        Badge("speedster",   "Speedster",        "⚡",                  colorRes = R.color.badge_blue),
        Badge("early_bird",  "Early Bird",       "🌅",                  colorRes = R.color.badge_orange),
        Badge("consistency", "Consistency Champ","📅",                  colorRes = R.color.badge_green),
        Badge("marathon",    "Marathon Master",  "⛰️",                 colorRes = R.color.badge_purple),
        Badge("safety",      "Safety First",     "🛡️",                 colorRes = R.color.badge_blue),
        Badge("hydration",   "Hydration Pro",    "💧",                  colorRes = R.color.badge_blue),
        Badge("streak7",     "7-Day Streak",     "🔥",                  colorRes = R.color.badge_orange),
        Badge("officy_out",  "Officy Out",       "🧑‍🦰",               colorRes = R.color.badge_gray)
    )
}