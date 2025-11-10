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

        setGreeting(user, preferFirestore = true)
        b.tvSub.text = "Stay consistent. Small steps win races."

        b.btnStart.setOnClickListener { go(StartRunActivity::class.java) }
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
        val u = FirebaseAuth.getInstance().currentUser
        if (u == null) {
            go(LoginActivity::class.java); finish()
            return
        }
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
                if (!name.isNullOrBlank()) b.tvGreeting.text = "Welcome back, $name!"
            }
    }
}
