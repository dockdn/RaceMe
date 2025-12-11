package com.example.raceme

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.example.raceme.databinding.ActivityStepCounterBinding

class StepCounterActivity : BaseActivity(), SensorEventListener {

    private lateinit var binding: ActivityStepCounterBinding

    private var stepGoal: Int = 0
    private var stepsTaken: Int = 0

    private val prefs by lazy { getSharedPreferences("step_prefs", MODE_PRIVATE) }

    // Sensor-related fields
    private lateinit var sensorManager: SensorManager
    private var stepCounterSensor: Sensor? = null
    private var baselineSteps: Int? = null   // used to turn device's lifetime steps into "today/session" steps
    private var isSensorMode: Boolean = false

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

        // Initialize sensor manager + step counter sensor
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        isSensorMode = stepCounterSensor != null

        if (!isSensorMode) {
            // Emulator / devices without sensor
            Toast.makeText(
                this,
                "Step sensor not available – demo mode (tap steps to increment).",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            // Real device with sensor
            Toast.makeText(
                this,
                "Using device step sensor when on your phone.",
                Toast.LENGTH_SHORT
            ).show()
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
            baselineSteps = null        // reset baseline so sensor session starts fresh
            prefs.edit().putInt("steps_taken", stepsTaken).apply()
            updateUI()
        }

        // Demo mode: tap steps to increment ONLY if no sensor
        if (!isSensorMode) {
            binding.tvSteps.setOnClickListener {
                stepsTaken++
                prefs.edit().putInt("steps_taken", stepsTaken).apply()
                updateUI()
            }
        } else {
            // On real device, sensor will control step count, no tap listener needed
            binding.tvSteps.setOnClickListener(null)
        }
    }

    // --- Lifecycle: register/unregister sensor listener ---

    override fun onResume() {
        super.onResume()
        if (isSensorMode && stepCounterSensor != null) {
            sensorManager.registerListener(
                this,
                stepCounterSensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }

    override fun onPause() {
        super.onPause()
        if (isSensorMode) {
            sensorManager.unregisterListener(this)
        }
    }

    // --- SensorEventListener implementation ---

    override fun onSensorChanged(event: SensorEvent?) {
        if (!isSensorMode) return
        if (event?.sensor?.type != Sensor.TYPE_STEP_COUNTER) return

        // This is the device's cumulative step count since last reboot
        val deviceTotalSteps = event.values[0].toInt()

        if (baselineSteps == null) {
            // Calibrate baseline so we don't show lifetime steps,
            // just "today / session" + whatever we may have already saved.
            baselineSteps = deviceTotalSteps - stepsTaken
        }

        val base = baselineSteps ?: return
        stepsTaken = (deviceTotalSteps - base).coerceAtLeast(0)

        prefs.edit().putInt("steps_taken", stepsTaken).apply()
        updateUI()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // no-op
    }

    // --- UI update logic ---

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
