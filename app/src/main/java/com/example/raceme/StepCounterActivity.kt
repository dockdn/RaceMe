package com.example.raceme

import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.example.raceme.databinding.ActivityStepCounterBinding

class StepCounterActivity : BaseActivity() {

    private lateinit var binding: ActivityStepCounterBinding
    private var stepGoal: Int = 0
    private var stepsTaken: Int = 0
    private val prefs by lazy { getSharedPreferences("step_prefs", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStepCounterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load saved values
        stepGoal = prefs.getInt("step_goal", 0)
        stepsTaken = prefs.getInt("steps_taken", 0)

        // Back arrow
        binding.root.findViewById<View>(R.id.btnBackSteps).setOnClickListener {
            finish()
        }

        updateUI()

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

        // Reset steps button
        binding.btnReset.setOnClickListener {
            stepsTaken = 0
            prefs.edit().putInt("steps_taken", stepsTaken).apply()
            updateUI()
        }

        // Simulate step increment for testing
        binding.tvSteps.setOnClickListener {
            stepsTaken++
            prefs.edit().putInt("steps_taken", stepsTaken).apply()
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
            ((stepsTaken.toFloat() / stepGoal.toFloat()) * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }

        binding.progressSteps.progress = progressPercent
        binding.tvProgressPercent.text = "$progressPercent% of goal"
    }
}
