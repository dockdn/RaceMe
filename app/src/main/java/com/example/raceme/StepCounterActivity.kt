package com.example.raceme

import android.os.Bundle
import android.widget.Toast
import com.example.raceme.databinding.ActivityStepCounterBinding

class StepCounterActivity : BaseActivity() {

    private lateinit var binding: ActivityStepCounterBinding
    private var stepGoal: Int = 0
    private var stepsTaken: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStepCounterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        updateUI()

        // Set step goal button
        binding.btnSetGoal.setOnClickListener {
            val goalText = binding.etStepGoal.text.toString()
            if (goalText.isNotEmpty()) {
                stepGoal = goalText.toInt()
                binding.tvStepGoal.text = "Step Goal: $stepGoal"
                Toast.makeText(this, "Step goal set to $stepGoal", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Please enter a valid goal", Toast.LENGTH_SHORT).show()
            }
        }

        // Reset steps button
        binding.btnReset.setOnClickListener {
            stepsTaken = 0
            updateUI()
        }

        // Simulate step increment for testing
        binding.tvSteps.setOnClickListener {
            stepsTaken++
            updateUI()
        }
    }

    private fun updateUI() {
        binding.tvSteps.text = "Steps Taken: $stepsTaken"
        binding.tvStepGoal.text = if (stepGoal > 0) "Step Goal: $stepGoal" else "Step Goal: Not Set"
    }
}