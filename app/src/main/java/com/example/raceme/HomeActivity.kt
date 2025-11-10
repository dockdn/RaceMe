package com.example.raceme

import android.os.Bundle
import android.widget.Toast
import com.example.raceme.databinding.ActivityHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class HomeActivity : BaseActivity() {
    private lateinit var b: ActivityHomeBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val user = auth.currentUser ?: run { go(LoginActivity::class.java); finish(); return }
        b = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(b.root)

        // Greeting + subheader
        setGreeting(user, preferFirestore = true)
        b.tvSub.text = "Stay consistent. Small steps win races."

        // Core navigation
        b.btnStart.setOnClickListener { go(StartRunActivity::class.java) }
        b.btnReports.setOnClickListener { go(ReportsActivity::class.java) }
        b.btnChallenges.setOnClickListener { go(ChallengesActivity::class.java) }
        b.btnBadges.setOnClickListener { go(BadgesActivity::class.java) }
        b.btnEditProfile.setOnClickListener { go(ProfileActivity::class.java) }
        b.btnQuotes.setOnClickListener { go(QuotesActivity::class.java) }
        b.btnMyRuns.setOnClickListener { go(RunsActivity::class.java) }

        // 🆕 Community Feed + New Post (safe-call in case buttons aren’t in XML yet)
        b.btnFeed?.setOnClickListener { go(FeedActivity::class.java) }
        b.btnNewPost?.setOnClickListener { go(NewPostActivity::class.java) }

        // Reminders (optional button in your layout)
        b.btnReminder?.setOnClickListener { go(RemindersActivity::class.java) }

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
            go(LoginActivity::class.java); finish()
            return
        }
        // Refresh greeting after a possible profile change
        u.reload().addOnCompleteListener { setGreeting(u, preferFirestore = true) }
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
}