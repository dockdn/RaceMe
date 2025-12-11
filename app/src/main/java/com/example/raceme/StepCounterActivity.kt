package com.example.raceme

import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.example.raceme.databinding.ActivityStepCounterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class StepCounterActivity : BaseActivity() {

    private lateinit var binding: ActivityStepCounterBinding

    // Lifetime goal (local only) + lifetime steps (from Firestore)
    private var stepGoal: Int = 0
    private var stepsTaken: Int = 0

    // Local prefs just for goal
    private val prefs by lazy { getSharedPreferences("step_prefs", MODE_PRIVATE) }

    // Firebase
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStepCounterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 🔙 Back arrow
        binding.root.findViewById<View>(R.id.btnBackSteps).setOnClickListener {
            finish()
        }

        // Load saved goal from prefs
        stepGoal = prefs.getInt("step_goal", 0)

        // Load lifetime steps from Firestore
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "You must be logged in to view steps", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        loadStepsFromFirestore(uid)

        // Set step goal button
        binding.btnSetGoal.setOnClickListener {
            val goalText = binding.etStepGoal.text.toString()
            if (goalText.isNotEmpty()) {
                stepGoal = goalText.toInt()
                prefs.edit().putInt("step_goal", stepGoal).apply()
                updateUI()
                Toast.makeText(this, "Step goal set to $stepGoal", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Please enter a valid goal", Toast.LENGTH_SHORT).show()
            }
        }

        // "Reset" – just clears the local view for this session
        // (lifetime steps will reload from runs next time you open the screen)
        binding.btnReset.setOnClickListener {
            stepsTaken = 0
            updateUI()
            Toast.makeText(this, "Progress cleared (lifetime steps stay in your runs)", Toast.LENGTH_SHORT).show()
        }

        // Initial UI (will be updated again once Firestore comes back)
        updateUI()
    }

    /**
     * Pull lifetime steps from Firestore user doc:
     * users/{uid}.steps (updated when you save runs).
     */
    private fun loadStepsFromFirestore(uid: String) {
        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                stepsTaken = (doc.getLong("steps") ?: 0L).toInt()
                updateUI()
            }
            .addOnFailureListener {
                // If it fails, we just keep whatever we have (default 0)
                updateUI()
            }
    }

    // Update UI based on steps + goal
    private fun updateUI() {
        // Update steps text
        binding.tvSteps.text = "Steps Taken: $stepsTaken"

        // Update goal text
        binding.tvStepGoal.text =
            if (stepGoal > 0) "Step Goal: $stepGoal" else "Step Goal: Not Set"

        // Calculate and show progress
        val progressPercent = if (stepGoal > 0) {
            ((stepsTaken.toFloat() / stepGoal.toFloat()) * 100)
                .toInt()
                .coerceIn(0, 100)
        } else {
            0
        }

        binding.progressSteps.progress = progressPercent
        binding.tvProgressPercent.text = "$progressPercent% of goal"
    }
}
