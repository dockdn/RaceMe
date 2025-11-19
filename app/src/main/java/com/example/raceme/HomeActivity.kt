package com.example.raceme

import android.os.Bundle
import android.widget.Toast
import com.example.raceme.databinding.ActivityHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class HomeActivity : BaseActivity() {
    private lateinit var b: ActivityHomeBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val user = auth.currentUser ?: run {
            go(LoginActivity::class.java)
            finish()
            return
        }

        b = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(b.root)

        // Greeting + smart subheader quote
        setGreeting(user, preferFirestore = true)
        updateLoginCountAndMotivationQuote(user)

        // Core navigation
        b.btnStart.setOnClickListener { go(StartRunActivity::class.java) }
        b.btnReports.setOnClickListener { go(ReportsActivity::class.java) }
        b.btnChallenges.setOnClickListener { go(ChallengesActivity::class.java) }
        b.btnBadges.setOnClickListener { go(BadgesActivity::class.java) }
        b.btnEditProfile.setOnClickListener { go(ProfileActivity::class.java) }
        b.btnMyRuns.setOnClickListener { go(RunsActivity::class.java) }

        // Community feed
        b.btnFeed.setOnClickListener { go(FeedActivity::class.java) }

        // Events screen
        b.btnEvents.setOnClickListener { go(EventsActivity::class.java) }

        // Reminders
        b.btnReminder.setOnClickListener { go(RemindersActivity::class.java) }

        // Extra features
        b.btnStepCounter.setOnClickListener {
            go(StepCounterActivity::class.java)
        }
        b.btnFriendRequests.setOnClickListener {
            go(FriendRequestsActivity::class.java)
        }
        b.btnLeaderboard.setOnClickListener {
            go(LeaderboardActivity::class.java)
        }

        // Logout
        b.btnLogout.setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()
            go(LoginActivity::class.java)
            finishAffinity()
        }
    }

    override fun onResume() {
        super.onResume()
        val u = FirebaseAuth.getInstance().currentUser
        if (u == null) {
            go(LoginActivity::class.java)
            finish()
            return
        }
        // Refresh greeting after a possible profile change
        u.reload().addOnCompleteListener {
            setGreeting(u, preferFirestore = true)
        }
    }

    private fun setGreeting(user: FirebaseUser?, preferFirestore: Boolean) {
        val fallback = user?.displayName ?: user?.email ?: "Racer"
        b.tvGreeting.text = "Welcome back, $fallback!"
        if (!preferFirestore || user == null) return

        db.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { snap ->
                val name = snap.getString("displayName")
                if (!name.isNullOrBlank()) {
                    b.tvGreeting.text = "Welcome back, $name!"
                }
            }
    }

    private fun updateLoginCountAndMotivationQuote(user: FirebaseUser) {
        val userDoc = db.collection("users").document(user.uid)

        // Increment loginCount every time Home is opened
        userDoc.update("loginCount", FieldValue.increment(1))
            .addOnCompleteListener {
                // After increment, read stats and choose quote
                userDoc.get()
                    .addOnSuccessListener { snap ->
                        val loginCount = snap.getLong("loginCount") ?: 0L
                        val challengesCompleted = snap.getLong("challengesCompleted") ?: 0L

                        val stats = UserMotivationStats(
                            loginCount = loginCount,
                            challengesCompleted = challengesCompleted
                        )

                        val quote = MotivationRepository.getQuoteFor(stats)
                        b.tvSub.text = quote
                    }
                    .addOnFailureListener {
                        // Fallback quote if Firestore read fails
                        val fallbackQuote = MotivationRepository.getQuoteFor(
                            UserMotivationStats(0L, 0L)
                        )
                        b.tvSub.text = fallbackQuote
                    }
            }
    }
}
