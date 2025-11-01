package com.example.raceme

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.TranslateAnimation
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.example.raceme.databinding.ActivityStartRunBinding
import com.google.android.gms.location.*
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.round

class StartRunActivity : BaseActivity() {
    private lateinit var b: ActivityStartRunBinding
    private var menuOpen = false

    // Chronometer state
    private var isPaused = false
    private var pauseOffset: Long = 0L
    private var startedAtMillis: Long? = null

    // Firebase
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    // Public race linkage
    private var selectedPublicRaceId: String? = null

    // ===== Location tracking =====
    private lateinit var fused: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private var locationCallback: LocationCallback? = null

    private var lastFix: Location? = null
    private var distanceMeters: Double = 0.0

    // permission launcher
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

        // init fused location
        fused = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1500L)
            .setMinUpdateIntervalMillis(1000L)
            .setMaxUpdateDelayMillis(3000L)
            .setMinUpdateDistanceMeters(2.0f)
            .build()

        // FAB toggles menu
        b.fabMenu.setOnClickListener { toggleMenu() }

        // Menu actions
        b.btnMenuStartRun.setOnClickListener {
            showStartChooser()
            toggleMenu(closeOnly = true)
        }
        b.btnMenuCustomizeRace.setOnClickListener {
            go(CreateRaceActivity::class.java)
            toggleMenu(closeOnly = true)
        }
        b.btnMenuProfile.setOnClickListener {
            go(ProfileActivity::class.java)
            toggleMenu(closeOnly = true)
        }

        // ON-SCREEN CONTROLS
        b.btnPause.setOnClickListener { togglePause() }
        b.btnStop.setOnClickListener { stopRunAndSave() }

        // Reset UI text
        b.tvMiles.text = "0.00 mi"
    }

    // ===== PUBLIC OR NEW ?! =====
    private fun showStartChooser() {
        val items = arrayOf("Browse public races", "Start a new race")
        AlertDialog.Builder(this)
            .setTitle("How do you want to start?")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> loadAndPickPublicRace()
                    1 -> { selectedPublicRaceId = null; startRun() }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadAndPickPublicRace() {
        db.collection("publicRaces")
            .orderBy("createdAt")
            .limit(20)
            .get()
            .addOnSuccessListener { snap ->
                if (snap.isEmpty) {
                    Toast.makeText(this, "No public races yet", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                val docs = snap.documents
                val names = docs.map { it.getString("name") ?: "(unnamed race)" }.toTypedArray()
                AlertDialog.Builder(this)
                    .setTitle("Choose a public race")
                    .setItems(names) { _, idx ->
                        val doc = docs[idx]
                        selectedPublicRaceId = doc.id
                        b.inputRaceName.setText(doc.getString("name") ?: "")
                        startRun()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, e.message ?: "Failed to load public races", Toast.LENGTH_LONG).show()
            }
    }

    // ===== RUN CONTROLS =====
    private fun startRun() {
        if (b.runControls.visibility != View.VISIBLE) b.runControls.visibility = View.VISIBLE
        if (startedAtMillis == null) startedAtMillis = System.currentTimeMillis()

        // RESET NOT PAUSE ...
        if (pauseOffset == 0L && !isPaused) {
            distanceMeters = 0.0
            lastFix = null
            b.tvMiles.text = "0.00 mi"
        }

        // TIMER
        b.chronometer.base = SystemClock.elapsedRealtime() - pauseOffset
        b.chronometer.start()
        isPaused = false
        b.btnPause.text = "Pause"

        // LOCATION
        ensureLocationPermissionThenStart()
        Toast.makeText(this, "Run started!", Toast.LENGTH_SHORT).show()
    }

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
        if (locationCallback != null) return // already running
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val fix = result.lastLocation ?: return
                if (fix.hasAccuracy() && fix.accuracy > 50f) return // ignore bad fixes

                lastFix?.let { prev ->
                    val delta = prev.distanceTo(fix) // meters
                    if (delta in 0.5f..100f) {       // discard micro-moves and jumps
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

    private fun togglePause() {
        if (isPaused) {
            b.chronometer.base = SystemClock.elapsedRealtime() - pauseOffset
            b.chronometer.start()
            isPaused = false
            b.btnPause.text = "Pause"
            startLocationUpdates()
        } else {
            pauseOffset = SystemClock.elapsedRealtime() - b.chronometer.base
            b.chronometer.stop()
            isPaused = true
            b.btnPause.text = "Resume"
            stopLocationUpdates()
        }
    }

    private fun stopRunAndSave() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "Not signed in", Toast.LENGTH_LONG).show()
            return
        }

        val endMs = System.currentTimeMillis()
        val startMs = startedAtMillis ?: endMs
        val elapsedMs = SystemClock.elapsedRealtime() - b.chronometer.base
        b.chronometer.stop()
        stopLocationUpdates()

        val name = b.inputRaceName.text?.toString()?.trim().orEmpty()
        val miles = distanceMeters / 1609.344

        val runDoc = hashMapOf(
            "name" to name,
            "elapsedMs" to elapsedMs,
            "distanceMeters" to distanceMeters,
            "distanceMiles" to miles,
            "startedAt" to Timestamp(startMs / 1000, ((startMs % 1000) * 1_000_000).toInt()),
            "endedAt" to Timestamp(endMs / 1000, ((endMs % 1000) * 1_000_000).toInt()),
            "createdAt" to Timestamp.now(),
            "device" to android.os.Build.MODEL,
            "sdkInt" to android.os.Build.VERSION.SDK_INT,
            "appVersion" to runCatching {
                packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0"
            }.getOrElse { "1.0" }
        ).apply {
            selectedPublicRaceId?.let { put("publicRaceId", it) }
        }

        db.collection("users").document(uid)
            .collection("runs")
            .add(runDoc)
            .addOnSuccessListener {
                val pretty = formatElapsed(elapsedMs)
                Toast.makeText(
                    this,
                    "Saved ${if (name.isNotEmpty()) "“$name” " else ""}– $pretty, ${String.format("%.2f", miles)} mi",
                    Toast.LENGTH_LONG
                ).show()
                resetRunUI()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, e.message ?: "Failed to save run", Toast.LENGTH_LONG).show()
            }
    }

    private fun resetRunUI() {
        pauseOffset = 0L
        isPaused = false
        startedAtMillis = null
        lastFix = null
        distanceMeters = 0.0
        b.btnPause.text = "Pause"
        b.chronometer.base = SystemClock.elapsedRealtime()
        b.inputRaceName.setText("")
        b.tvMiles.text = "0.00 mi"
        // keep or hide controls as you prefer
        // b.runControls.visibility = View.GONE
    }

    private fun formatElapsed(ms: Long): String {
        val totalSec = ms / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    }

    private fun toggleMenu(closeOnly: Boolean = false) {
        if (menuOpen || closeOnly) {
            val slideOut = TranslateAnimation(0f, 0f, 0f, 40f).apply { duration = 140 }
            val fadeOut = AlphaAnimation(1f, 0f).apply { duration = 140 }
            b.menuCard.startAnimation(slideOut)
            b.menuCard.startAnimation(fadeOut)
            b.menuCard.visibility = View.GONE
            menuOpen = false
            b.fabMenu.shrink()
        } else {
            b.menuCard.visibility = View.VISIBLE
            val slideIn = TranslateAnimation(0f, 0f, 40f, 0f).apply { duration = 140 }
            val fadeIn = AlphaAnimation(0f, 1f).apply { duration = 140 }
            b.menuCard.startAnimation(slideIn)
            b.menuCard.startAnimation(fadeIn)
            b.fabMenu.extend()
            menuOpen = true
        }
    }

    override fun onPause() {
        super.onPause()
        // For true background tracking, move updates to a Foreground Service.
        if (!isPaused) stopLocationUpdates()
    }

    override fun onResume() {
        super.onResume()
        if (!isPaused && startedAtMillis != null) ensureLocationPermissionThenStart()
    }
}