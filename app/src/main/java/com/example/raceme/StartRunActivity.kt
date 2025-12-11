package com.example.raceme

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.example.raceme.databinding.ActivityStartRunBinding
import com.google.android.gms.location.*
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlin.math.round
import kotlin.math.roundToInt

class StartRunActivity : BaseActivity() {

    private lateinit var b: ActivityStartRunBinding

    // Timer / run state
    private var isPaused = true
    private var pauseOffset: Long = 0L
    private var startedAtMillis: Long? = null

    // Firebase
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    // Track selection
    private var selectedPublicRaceId: String? = null
    private var selectedTrackName: String? = null
    private var selectedType: String? = null

    // GPS tracking
    private lateinit var fused: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private var locationCallback: LocationCallback? = null
    private var lastFix: Location? = null
    private var distanceMeters: Double = 0.0

    // Timer loop
    private val timerHandler = Handler(Looper.getMainLooper())
    private var timerRunnable: Runnable? = null

    // Permissions
    private val locationPermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            val fine = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true
            val coarse = grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (fine || coarse) {
                startLocationUpdates()
            } else {
                Toast.makeText(this, "Location permission required", Toast.LENGTH_LONG).show()
            }
        }

    // Choose existing public track
    private val pickTrack =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
            if (res.resultCode == RESULT_OK) {
                val data = res.data
                selectedTrackName = data?.getStringExtra("selected_track_name")
                selectedPublicRaceId = data?.getStringExtra("selected_public_race_id")
                selectedTrackName?.let { b.ddTrack.setText(it, false) }
                Toast.makeText(
                    this,
                    "Track selected: ${selectedTrackName ?: ""}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityStartRunBinding.inflate(layoutInflater)
        setContentView(b.root)

        // GPS setup
        fused = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1500L)
            .setMinUpdateIntervalMillis(1000L)
            .setMaxUpdateDelayMillis(3000L)
            .setMinUpdateDistanceMeters(2f)
            .build()

        // Track dropdown: new vs browse
        val options = listOf("Start a new track", "Browse public tracks…")
        b.ddTrack.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, options))
        b.ddTrack.setOnItemClickListener { _, _, position, _ ->
            when (position) {
                0 -> {
                    selectedPublicRaceId = null
                    selectedTrackName = "New Track"
                    selectedType = null
                    Toast.makeText(this, "New track selected", Toast.LENGTH_SHORT).show()
                }
                1 -> pickTrack.launch(Intent(this, TracksActivity::class.java))
            }
        }
        b.ddTrack.setText("", false)
        selectedTrackName = null

        // Initial UI
        b.tvMiles.text = "0.00 mi"
        b.btnPause.text = "Start"
        b.chronometer.base = SystemClock.elapsedRealtime()
        b.chronometer.text = formatElapsedTime(0L)

        // Start / Pause
        b.btnPause.setOnClickListener {
            if (isPaused && startedAtMillis == null) {
                startRun()
            } else {
                onStartPauseClicked()
            }
        }

        // Stop -> summary dialog
        b.btnStop.setOnClickListener {
            stopRunShowSummary()
        }

        // Back button in toolbar (if present)
        b.root.findViewById<View?>(R.id.btnBack)?.setOnClickListener {
            finish()
        }
    }

    // ========= TIMER HELPERS =========

    private fun formatElapsedTime(elapsedMs: Long): String {
        val totalSeconds = elapsedMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val hundredths = (elapsedMs % 1000) / 10
        return String.format("%02d:%02d.%02d", minutes, seconds, hundredths)
    }

    private fun startTimerLoop() {
        if (timerRunnable != null) return
        timerRunnable = object : Runnable {
            override fun run() {
                val elapsedMs = SystemClock.elapsedRealtime() - b.chronometer.base
                b.chronometer.text = formatElapsedTime(elapsedMs)
                timerHandler.postDelayed(this, 50L)
            }
        }
        timerHandler.post(timerRunnable!!)
    }

    private fun stopTimerLoop() {
        timerRunnable?.let { timerHandler.removeCallbacks(it) }
        timerRunnable = null
    }

    // ========= RUN START / PAUSE =========

    private fun onStartPauseClicked() {
        if (isPaused) {
            b.chronometer.base = SystemClock.elapsedRealtime() - pauseOffset
            startTimerLoop()
            isPaused = false
            b.btnPause.text = "Pause"
            ensureLocationPermissionThenStart()
        } else {
            pauseOffset = SystemClock.elapsedRealtime() - b.chronometer.base
            stopTimerLoop()
            isPaused = true
            b.btnPause.text = "Resume"
            stopLocationUpdates()
        }
    }

    private fun startRun() {
        if (selectedTrackName.isNullOrBlank() && selectedPublicRaceId == null) {
            Toast.makeText(this, "Pick a track before starting", Toast.LENGTH_LONG).show()
            b.ddTrack.performClick()
            return
        }

        if (startedAtMillis == null) {
            startedAtMillis = System.currentTimeMillis()
        }

        if (pauseOffset == 0L && isPaused) {
            distanceMeters = 0.0
            lastFix = null
            b.tvMiles.text = "0.00 mi"
        }

        b.chronometer.base = SystemClock.elapsedRealtime() - pauseOffset
        startTimerLoop()
        isPaused = false
        b.btnPause.text = "Pause"
        ensureLocationPermissionThenStart()

        Toast.makeText(this, "Run started!", Toast.LENGTH_SHORT).show()
    }

    private fun ensureLocationPermissionThenStart() {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse =
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates()
        } else {
            locationPermLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // ========= GPS TRACKING =========

    private fun startLocationUpdates() {
        if (locationCallback != null) return
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val fix = result.lastLocation ?: return
                if (fix.hasAccuracy() && fix.accuracy > 50f) return
                lastFix?.let { prev ->
                    val delta = prev.distanceTo(fix)
                    if (delta in 0.5f..100f) {
                        distanceMeters += delta
                        updateMilesLabel()
                    }
                }
                lastFix = fix
            }
        }
        try {
            fused.requestLocationUpdates(locationRequest, locationCallback!!, mainLooper)
        } catch (_: SecurityException) {
        }
    }

    private fun stopLocationUpdates() {
        locationCallback?.let { fused.removeLocationUpdates(it) }
        locationCallback = null
    }

    private fun updateMilesLabel() {
        val miles = distanceMeters / 1609.344
        val shown = (round(miles * 100.0) / 100.0)
        b.tvMiles.text = String.format("%.2f mi", shown)
    }

    // ========= STOP + SUMMARY =========

    private fun stopRunShowSummary() {
        val endMs = System.currentTimeMillis()
        val startMs = startedAtMillis ?: endMs
        val elapsedMs = SystemClock.elapsedRealtime() - b.chronometer.base

        stopTimerLoop()
        stopLocationUpdates()
        isPaused = true
        b.btnPause.text = "Start"

        val miles = distanceMeters / 1609.344
        val pace = computePace(elapsedMs, miles)

        val view = layoutInflater.inflate(R.layout.run_summary, null, false)
        view.findViewById<TextView>(R.id.tvDistance).text =
            String.format("Distance: %.2f mi", miles)
        view.findViewById<TextView>(R.id.tvPace).text = "Pace: $pace"
        val quote = pickQuote()
        view.findViewById<TextView>(R.id.tvQuote).text = "“$quote”"

        val ratingBar = view.findViewById<RatingBar>(R.id.ratingBar)
        ratingBar?.setIsIndicator(false)

        val groupNewTrack = view.findViewById<View>(R.id.groupNewTrack)
        val inputName = view.findViewById<TextInputEditText>(R.id.inputTrackName)
        val inputDesc = view.findViewById<TextInputEditText>(R.id.inputTrackDesc)
        val ddTrackType = view.findViewById<AutoCompleteTextView>(R.id.ddTrackType)
        val inputAddress = view.findViewById<TextInputEditText>(R.id.inputTrackAddress)
        val switchMakePublic = view.findViewById<SwitchMaterial>(R.id.switchMakePublic)

        val usingPublicTrack = (selectedPublicRaceId != null)

        if (usingPublicTrack) {
            groupNewTrack.visibility = View.GONE
        } else {
            groupNewTrack.visibility = View.VISIBLE

            if (!selectedTrackName.isNullOrBlank() && selectedTrackName != "New Track") {
                inputName.setText(selectedTrackName)
            }

            val raceTypes = listOf("Open run", "5K", "10K", "Half Marathon", "Marathon")
            val typeAdapter = ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                raceTypes
            )
            ddTrackType.setAdapter(typeAdapter)

            val initialType = selectedType?.takeIf { it.isNotBlank() } ?: "Open run"
            ddTrackType.setText(initialType, false)

            ddTrackType.setOnClickListener {
                ddTrackType.showDropDown()
            }
        }

        val dlg = AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(false)
            .create()

        view.findViewById<View>(R.id.btnCancel).setOnClickListener {
            dlg.dismiss()
            resetRunUI()
        }

        view.findViewById<View>(R.id.btnSave).setOnClickListener {
            val rating = (ratingBar?.rating ?: 0f).roundToInt()

            if (usingPublicTrack) {
                // Existing public track: just save the run
                saveRunToFirestore(
                    startMs, endMs, elapsedMs, miles, pace, quote, rating,
                    addressText = null
                )
                dlg.dismiss()
                resetRunUI()
            } else {
                val finalName = inputName.text?.toString()?.trim().orEmpty()
                val desc = inputDesc.text?.toString()?.trim().orEmpty()

                if (finalName.isEmpty()) {
                    Toast.makeText(this, "Enter a Track Name", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                val typeText = ddTrackType.text?.toString()?.trim().orEmpty()
                val finalType = if (typeText.isBlank()) "Open run" else typeText
                selectedType = finalType

                val addressTextRaw = inputAddress.text?.toString()?.trim()
                val addressToSave = addressTextRaw?.takeIf { it.isNotBlank() }

                selectedTrackName = finalName

                // If "Make public" is on, create a public track document (doesn't block UI)
                if (switchMakePublic.isChecked) {
                    createPublicTrackDoc(
                        name = finalName,
                        desc = desc,
                        type = finalType,
                        address = addressToSave
                    )
                }

                // Always save the run exactly once
                saveRunToFirestore(
                    startMs, endMs, elapsedMs, miles, pace, quote, rating,
                    addressText = addressToSave
                )

                dlg.dismiss()
                resetRunUI()
            }
        }

        dlg.show()
    }

    // ========= CREATE PUBLIC TRACK DOC =========

    private fun createPublicTrackDoc(
        name: String,
        desc: String,
        type: String,
        address: String?
    ) {
        val u = auth.currentUser ?: return

        val data = hashMapOf(
            "name" to name,
            "description" to desc,
            "type" to type,
            "ownerUid" to u.uid,
            "ownerName" to (u.displayName ?: u.email ?: "Racer"),
            "visibility" to "public",
            "createdAt" to FieldValue.serverTimestamp()
        ).apply {
            if (!address.isNullOrBlank()) {
                put("addressText", address)
            }
        }

        db.collection("publicRaces")
            .add(data)
            .addOnSuccessListener {
                Toast.makeText(this, "Track published!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    e.message ?: "Failed to publish track",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // ========= SAVE RUN =========

    private fun saveRunToFirestore(
        startMs: Long,
        endMs: Long,
        elapsedMs: Long,
        miles: Double,
        pace: String,
        quote: String,
        rating: Int,
        addressText: String?
    ) {
        val u = auth.currentUser ?: return

        val friendlyName = selectedTrackName
            ?: "Run " + java.text.SimpleDateFormat(
                "M/d h:mma",
                java.util.Locale.US
            ).format(java.util.Date(startMs))

        val runDoc = hashMapOf(
            "name" to friendlyName,
            "type" to (selectedType ?: ""),
            "description" to "",
            "elapsedMs" to elapsedMs,
            "distanceMeters" to distanceMeters,
            "distanceMiles" to miles,
            "paceMinPerMile" to pace,
            "quote" to quote,
            "rating" to rating,
            "startedAt" to Timestamp(startMs / 1000, ((startMs % 1000) * 1_000_000).toInt()),
            "endedAt" to Timestamp(endMs / 1000, ((endMs % 1000) * 1_000_000).toInt()),
            "createdAt" to Timestamp.now(),
            "device" to android.os.Build.MODEL,
            "sdkInt" to android.os.Build.VERSION.SDK_INT
        ).apply {
            selectedPublicRaceId?.let { put("publicRaceId", it) }
            if (!addressText.isNullOrBlank()) {
                put("addressText", addressText)
            }
        }

        db.collection("users").document(u.uid)
            .collection("runs")
            .add(runDoc)
            .addOnSuccessListener {
                val approxSteps = (miles * 2100.0).roundToInt()

                val updates = mapOf(
                    "distanceMeters" to FieldValue.increment(distanceMeters),
                    "steps" to FieldValue.increment(approxSteps.toLong())
                )

                db.collection("users").document(u.uid)
                    .set(updates, SetOptions.merge())

                Toast.makeText(
                    this,
                    "Saved – ${String.format("%.2f", miles)} mi @ $pace",
                    Toast.LENGTH_LONG
                ).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    e.message ?: "Failed to save run",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // ========= OTHER HELPERS =========

    private fun computePace(elapsedMs: Long, miles: Double): String {
        if (miles <= 0.0) return "--:-- / mi"
        val totalSec = (elapsedMs / 1000.0) / miles
        val m = totalSec.toInt() / 60
        val s = (totalSec % 60).toInt()
        return "%d:%02d / mi".format(m, s)
    }

    private fun pickQuote(): String {
        val quotes = listOf(
            "Small steps win races.",
            "You’re lapping everyone still on the couch.",
            "One more mile, one more smile.",
            "Consistency beats intensity.",
            "Run the day, don’t let it run you.",
            "Progress > perfection."
        )
        return quotes.random()
    }

    private fun resetRunUI() {
        pauseOffset = 0L
        isPaused = true
        startedAtMillis = null
        lastFix = null
        distanceMeters = 0.0

        b.btnPause.text = "Start"
        b.chronometer.base = SystemClock.elapsedRealtime()
        b.chronometer.text = formatElapsedTime(0L)
        b.tvMiles.text = "0.00 mi"

        selectedPublicRaceId = null
        selectedTrackName = null
        selectedType = null
        b.ddTrack.setText("", false)
    }

    override fun onPause() {
        super.onPause()
        if (!isPaused) {
            stopLocationUpdates()
            stopTimerLoop()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isPaused && startedAtMillis != null) {
            ensureLocationPermissionThenStart()
            startTimerLoop()
        }
    }
}
