package com.example.raceme

import android.os.Bundle
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
        stepGoal = prefs.getInt("step_goal", 0)
        stepsTaken = prefs.getInt("steps_taken", 0)
        setContentView(binding.root)

        updateUI()

        // Set step goal button
        binding.btnSetGoal.setOnClickListener {
            val goalText = binding.etStepGoal.text.toString()
            if (goalText.isNotEmpty()) {
                stepGoal = goalText.toInt()
                prefs.edit().putInt("step_goal", stepGoal).apply()
                binding.tvStepGoal.text = "Step Goal: $stepGoal"
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

    // Added progress bar update logic for steps vs goal
    private fun updateUI() {

        // Update steps
        binding.tvSteps.text = "Steps Taken: $stepsTaken"

        // Update goal
        binding.tvStepGoal.text =
            if (stepGoal > 0) "Step Goal: $stepGoal" else "Step Goal: Not Set"

        // Update ProgressBar based on steps vs goal
        if (stepGoal > 0) {
            val progressPercent =
                ((stepsTaken.toFloat() / stepGoal.toFloat()) * 100).toInt()
            binding.progressSteps.progress = progressPercent.coerceIn(0, 100)
        } else {
            binding.progressSteps.progress = 0
        }
    }
}
