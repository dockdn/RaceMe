package com.example.raceme

import android.os.Bundle
import android.widget.Toast
import com.example.raceme.databinding.ActivityHomeBinding
import com.google.firebase.auth.FirebaseAuth

class HomeActivity : BaseActivity() {
    private lateinit var b: ActivityHomeBinding
    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val user = auth.currentUser ?: run { go(LoginActivity::class.java); finish(); return }
        b = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(b.root)

        val display = user.displayName ?: user.email ?: "Racer"
        b.tvGreeting.text = "Welcome back, $display!"
        b.tvSub.text = "Stay consistent. Small steps win races."

        b.btnStart.setOnClickListener { go(StartRunActivity::class.java) }
        b.btnCreate.setOnClickListener { go(CreateRaceActivity::class.java) }
        b.btnReports.setOnClickListener { go(ReportsActivity::class.java) }
        b.btnChallenges.setOnClickListener { go(ChallengesActivity::class.java) }
        b.btnBadges.setOnClickListener { go(BadgesActivity::class.java) }
        b.btnEditProfile.setOnClickListener { go(ProfileActivity::class.java) }
        b.btnQuotes.setOnClickListener { go(QuotesActivity::class.java) }
        b.btnMyRuns.setOnClickListener { go(RunsActivity::class.java) }
        b.btnLogout.setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()
            go(LoginActivity::class.java); finishAffinity()
        }
        b.btnReminder?.setOnClickListener { go(RemindersActivity::class.java) }
    }

    override fun onResume() {
        super.onResume()
        if (FirebaseAuth.getInstance().currentUser == null) { go(LoginActivity::class.java); finish() }
    }
}
