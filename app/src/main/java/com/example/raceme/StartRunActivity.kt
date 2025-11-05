package com.example.raceme

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.TranslateAnimation
import android.widget.ArrayAdapter
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.example.raceme.databinding.ActivityStartRunBinding
import com.google.android.gms.location.*
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.round

class StartRunActivity : BaseActivity() {

    private lateinit var b: ActivityStartRunBinding
    private var menuOpen = false

    // Timer state
    private var isPaused = true       // app opens with "Start"
    private var pauseOffset: Long = 0L
    private var startedAtMillis: Long? = null

    // Firebase
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    // Optional public race link & labels
    private var selectedPublicRaceId: String? = null
    private var selectedTrackName: String? = null
    private var selectedType: String? = null

    // Location tracking
    private lateinit var fused: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private var locationCallback: LocationCallback? = null
    private var lastFix: Location? = null
    private var distanceMeters: Double = 0.0

    // Runtime location perms
    private val locationPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val fine = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fine || coarse) startLocationUpdates()
        else Toast.makeText(this, "Location permission is required to track miles", Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityStartRunBinding.inflate(layoutInflater)
        setContentView(b.root)

        // Location set-up
        fused = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1500L)
            .setMinUpdateIntervalMillis(1000L)
            .setMaxUpdateDelayMillis(3000L)
            .setMinUpdateDistanceMeters(2.0f)
            .build()

        // Optional dropdown on the layout (AutoCompleteTextView with id=ddTrack)
        runCatching {
            val options = listOf("Start a new track", "Browse public tracks…")
            b.ddTrack.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, options))
            b.ddTrack.setOnItemClickListener { _, _, position, _ ->
                when (position) {
                    0 -> {
                        selectedPublicRaceId = null
                        selectedTrackName = "New Track"
                        Toast.makeText(this, "New track selected", Toast.LENGTH_SHORT).show()
                    }
                    1 -> {
                        go(TracksActivity::class.java)
                    }
                }
            }
            b.ddTrack.setText(options.first(), false)
            selectedTrackName = "New Track"
        }

        // Controls
        b.tvMiles.text = "0.00 mi"
        b.btnPause.text = "Start"
        b.btnPause.setOnClickListener {
            // First start → prompt for metadata
            if (isPaused && startedAtMillis == null) {
                promptRunMeta { name, type, makePublic ->
                    selectedTrackName = name
                    selectedType = type
                    if (makePublic) {
                        publishPublicRace(name, type) { raceId ->
                            selectedPublicRaceId = raceId
                            startRun()
                        }
                    } else {
                        selectedPublicRaceId = null
                        startRun()
                    }
                }
            } else {
                onStartPauseClicked()
            }
        }
        b.btnStop.setOnClickListener { stopRunShowSummary() }

        // Collapsible menu
        b.fabMenu.setOnClickListener { toggleMenu() }
        b.btnMenuStartRun.setOnClickListener {
            // Use same start flow (will prompt if first start)
            b.btnPause.performClick()
            toggleMenu(closeOnly = true)
        }
        b.btnMenuCustomizeRace.setOnClickListener { go(CreateRaceActivity::class.java); toggleMenu(closeOnly = true) }
        b.btnMenuProfile.setOnClickListener { go(ProfileActivity::class.java); toggleMenu(closeOnly = true) }
    }

    // ------------------------------------------------------------
    // Pre-run metadata dialog
    // ------------------------------------------------------------
    private fun promptRunMeta(onReady: (name: String, type: String, makePublic: Boolean) -> Unit) {
        val view = layoutInflater.inflate(R.layout.dialog_run_meta, null, false)
        val inputName = view.findViewById<android.widget.EditText>(R.id.inputName)
        val inputType = view.findViewById<android.widget.EditText>(R.id.inputType)
        val switchPublic = view.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchPublic)

        inputName.setText(selectedTrackName ?: "New Track")

        val dlg = AlertDialog.Builder(this).setView(view).create()
        view.findViewById<View>(R.id.btnCancel).setOnClickListener { dlg.dismiss() }
        view.findViewById<View>(R.id.btnOk).setOnClickListener {
            val name = inputName.text.toString().trim().ifBlank { "New Track" }
            val type = inputType.text.toString().trim()
            val makePublic = switchPublic.isChecked
            dlg.dismiss()
            onReady(name, type, makePublic)
        }
        dlg.show()
    }

    private fun publishPublicRace(name: String, type: String, onSaved: (raceId: String) -> Unit = {}) {
        val u = auth.currentUser ?: run {
            Toast.makeText(this, "Sign in required to publish", Toast.LENGTH_LONG).show()
            return
        }
        val data = hashMapOf(
            "name" to name,
            "type" to type,
            "ownerUid" to u.uid,
            "ownerName" to (u.displayName ?: u.email ?: "Racer"),
            "createdAt" to FieldValue.serverTimestamp()
        )
        db.collection("publicRaces")
            .add(data)
            .addOnSuccessListener { ref -> onSaved(ref.id) }
            .addOnFailureListener { e ->
                Toast.makeText(this, e.message ?: "Failed to publish", Toast.LENGTH_LONG).show()
            }
    }

    // ------------------------------------------------------------
    // Start / Pause
    // ------------------------------------------------------------
    private fun onStartPauseClicked() {
        if (isPaused) {
            // resume
            b.chronometer.base = SystemClock.elapsedRealtime() - pauseOffset
            b.chronometer.start()
            isPaused = false
            b.btnPause.text = "Pause"
            ensureLocationPermissionThenStart()
        } else {
            // pause
            pauseOffset = SystemClock.elapsedRealtime() - b.chronometer.base
            b.chronometer.stop()
            isPaused = true
            b.btnPause.text = "Resume"
            stopLocationUpdates()
        }
    }

    private fun startRun() {
        if (startedAtMillis == null) startedAtMillis = System.currentTimeMillis()
        if (pauseOffset == 0L && isPaused) {
            distanceMeters = 0.0
            lastFix = null
            b.tvMiles.text = "0.00 mi"
        }
        b.chronometer.base = SystemClock.elapsedRealtime() - pauseOffset
        b.chronometer.start()
        isPaused = false
        b.btnPause.text = "Pause"
        ensureLocationPermissionThenStart()
        Toast.makeText(this, "Run started!", Toast.LENGTH_SHORT).show()
    }

    // ------------------------------------------------------------
    // Location
    // ------------------------------------------------------------
    private fun ensureLocationPermissionThenStart() {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates()
        } else {
            locationPermLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

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
            fused.requestLocationUpdates(locationRequest, locationCallback as LocationCallback, mainLooper)
        } catch (_: SecurityException) {
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show()
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

    // ------------------------------------------------------------
    // Stop → Summary → Save
    // ------------------------------------------------------------
    private fun stopRunShowSummary() {
        val endMs = System.currentTimeMillis()
        val startMs = startedAtMillis ?: endMs
        val elapsedMs = SystemClock.elapsedRealtime() - b.chronometer.base

        b.chronometer.stop()
        stopLocationUpdates()
        isPaused = true
        b.btnPause.text = "Start"

        val miles = distanceMeters / 1609.344
        val pace = computePace(elapsedMs, miles)

        val view = layoutInflater.inflate(R.layout.run_summary, null, false)
        view.findViewById<TextView>(R.id.tvDistance).text = String.format("Distance: %.2f mi", miles)
        view.findViewById<TextView>(R.id.tvPace).text = "Pace: $pace"
        val quote = pickQuote()
        view.findViewById<TextView>(R.id.tvQuote).text = "“$quote”"
        val ratingBar = view.findViewById<RatingBar>(R.id.ratingBar)

        val dlg = AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(false)
            .create()

        view.findViewById<View>(R.id.btnBrowse).setOnClickListener {
            go(TracksActivity::class.java)
            dlg.dismiss()
        }

        view.findViewById<View>(R.id.btnCancel).setOnClickListener {
            dlg.dismiss()
            resetRunUI()
        }
        view.findViewById<View>(R.id.btnSave).setOnClickListener {
            val rating = ratingBar.rating.toInt()
            saveRunToFirestore(startMs, endMs, elapsedMs, miles, pace, quote, rating)
            dlg.dismiss()
        }
        dlg.show()
    }

    private fun saveRunToFirestore(
        startMs: Long,
        endMs: Long,
        elapsedMs: Long,
        miles: Double,
        pace: String,
        quote: String,
        rating: Int
    ) {
        val u = auth.currentUser
        if (u == null) {
            Toast.makeText(this, "Not signed in", Toast.LENGTH_LONG).show()
            return
        }

        val friendlyName = selectedTrackName
            ?: "Run " + java.text.SimpleDateFormat("M/d h:mma", java.util.Locale.US).format(java.util.Date(startMs))

        val runDoc = hashMapOf(
            "name" to friendlyName,
            "type" to (selectedType ?: ""),
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
        }

        db.collection("users").document(u.uid)
            .collection("runs")
            .add(runDoc)
            .addOnSuccessListener {
                Toast.makeText(this, "Saved – ${String.format("%.2f", miles)} mi @ $pace", Toast.LENGTH_LONG).show()
                resetRunUI()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, e.message ?: "Failed to save run", Toast.LENGTH_LONG).show()
            }
    }

    // ------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------
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
        b.tvMiles.text = "0.00 mi"
        // keep dropdown selection & selectedPublicRaceId as-is on purpose
    }

    // Collapsible fab menu animation
    private fun toggleMenu(closeOnly: Boolean = false) {
        if (menuOpen || closeOnly) {
            val slideOut = TranslateAnimation(0f, 0f, 0f, 40f).apply { duration = 140 }
            val fadeOut  = AlphaAnimation(1f, 0f).apply { duration = 140 }
            b.menuCard.startAnimation(slideOut)
            b.menuCard.startAnimation(fadeOut)
            b.menuCard.visibility = View.GONE
            menuOpen = false
            b.fabMenu.shrink()
        } else {
            b.menuCard.visibility = View.VISIBLE
            val slideIn = TranslateAnimation(0f, 0f, 40f, 0f).apply { duration = 140 }
            val fadeIn  = AlphaAnimation(0f, 1f).apply { duration = 140 }
            b.menuCard.startAnimation(slideIn)
            b.menuCard.startAnimation(fadeIn)
            b.fabMenu.extend()
            menuOpen = true
        }
    }

    override fun onPause() {
        super.onPause()
        if (!isPaused) stopLocationUpdates()
    }

    override fun onResume() {
        super.onResume()
        if (!isPaused && startedAtMillis != null) ensureLocationPermissionThenStart()
    }
}
