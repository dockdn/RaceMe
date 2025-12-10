package com.example.raceme

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.widget.Toast
import com.example.raceme.databinding.ActivityCreateRaceBinding
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale
import kotlin.concurrent.thread

class CreateRaceActivity : BaseActivity() {

    private lateinit var b: ActivityCreateRaceBinding
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    // From Google Places autocomplete (if user chooses a place)
    private var selectedLat: Double? = null
    private var selectedLng: Double? = null
    private var selectedLocationName: String? = null

    companion object {
        private const val AUTOCOMPLETE_REQUEST_CODE_RACE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityCreateRaceBinding.inflate(layoutInflater)
        setContentView(b.root)

        initPlacesIfNeeded()

        // Back button
        b.btnBackCreateRace.setOnClickListener { finish() }

        // Cancel button
        b.btnCancel.setOnClickListener { finish() }

        // 🔥 Location field: open Google Places autocomplete, don't type manually
        b.inputLocation.apply {
            isFocusable = false
            isClickable = true
            setOnClickListener {
                launchPlaceAutocompleteForRace()
            }
        }

        // Save button
        b.btnSave.setOnClickListener {
            saveRace()
        }
    }

    // Initialize Places using API key from manifest
    private fun initPlacesIfNeeded() {
        if (!Places.isInitialized()) {
            try {
                val ai = applicationContext.packageManager
                    .getApplicationInfo(packageName, PackageManager.GET_META_DATA)
                val apiKey = ai.metaData.getString("com.google.android.geo.API_KEY")

                if (!apiKey.isNullOrBlank()) {
                    Places.initialize(applicationContext, apiKey, Locale.getDefault())
                } else {
                    Toast.makeText(
                        this,
                        "Places API key not found in manifest.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    this,
                    "Failed to initialize Places: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // Launch the Google Places autocomplete overlay
    private fun launchPlaceAutocompleteForRace() {
        val fields = listOf(
            Place.Field.NAME,
            Place.Field.ADDRESS,
            Place.Field.LAT_LNG
        )

        val intent = Autocomplete.IntentBuilder(
            AutocompleteActivityMode.OVERLAY,
            fields
        ).build(this)

        startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE_RACE)
    }

    // Handle result from Places autocomplete UI
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == AUTOCOMPLETE_REQUEST_CODE_RACE) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    val place = Autocomplete.getPlaceFromIntent(data!!)
                    val latLng = place.latLng

                    selectedLat = latLng?.latitude
                    selectedLng = latLng?.longitude
                    selectedLocationName = place.address ?: place.name

                    // Show the chosen real address in the text field
                    b.inputLocation.setText(selectedLocationName ?: "")
                }

                AutocompleteActivity.RESULT_ERROR -> {
                    val status = Autocomplete.getStatusFromIntent(data!!)
                    Toast.makeText(
                        this,
                        "Places error: ${status.statusMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                Activity.RESULT_CANCELED -> {
                    // user backed out, ignore
                }
            }
        }
    }

    // Validate input and start save flow
    private fun saveRace() {
        val name = b.inputName.text.toString().trim()
        val description = b.inputDescription.text.toString().trim()
        val distanceText = b.inputDistanceMiles.text.toString().trim()
        val uiLocationText = b.inputLocation.text.toString().trim()
        val isPublic = b.switchPublic.isChecked

        if (name.isBlank()) {
            Toast.makeText(this, "Please enter a track name", Toast.LENGTH_SHORT).show()
            return
        }

        val distanceMiles = distanceText.toDoubleOrNull()
        if (distanceMiles == null || distanceMiles <= 0.0) {
            Toast.makeText(this, "Please enter a valid distance in miles", Toast.LENGTH_SHORT).show()
            return
        }

        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "You must be logged in to create a track", Toast.LENGTH_SHORT).show()
            return
        }

        b.btnSave.isEnabled = false

        val visibility = if (isPublic) "public" else "private"
        val ownerUid = user.uid
        val ownerName = user.displayName ?: user.email ?: "Unknown"

        // ✅ For races: location is OPTIONAL

        // Priority 1: user picked a Google Place
        if (selectedLocationName != null && selectedLat != null && selectedLng != null) {
            saveRaceToFirestore(
                name = name,
                description = description,
                distanceMiles = distanceMiles,
                visibility = visibility,
                ownerUid = ownerUid,
                ownerName = ownerName,
                locationName = selectedLocationName,
                startLat = selectedLat,
                startLng = selectedLng
            )
            return
        }

        // Priority 2: user typed something (if we ever allow typing) → fallback Geocoder
        if (uiLocationText.isNotBlank()) {
            geocodeAndSaveRace(
                name = name,
                description = description,
                distanceMiles = distanceMiles,
                visibility = visibility,
                ownerUid = ownerUid,
                ownerName = ownerName,
                locationName = uiLocationText
            )
            return
        }

        // Priority 3: no location at all → save without coordinates
        saveRaceToFirestore(
            name = name,
            description = description,
            distanceMiles = distanceMiles,
            visibility = visibility,
            ownerUid = ownerUid,
            ownerName = ownerName,
            locationName = null,
            startLat = null,
            startLng = null
        )
    }

    // Geocode fallback if user manually typed address (not strictly needed but nice)
    private fun geocodeAndSaveRace(
        name: String,
        description: String,
        distanceMiles: Double,
        visibility: String,
        ownerUid: String,
        ownerName: String,
        locationName: String
    ) {
        thread {
            try {
                val geocoder = Geocoder(this, Locale.getDefault())
                val results = geocoder.getFromLocationName(locationName, 1)

                val (lat, lng, resolvedName) = if (!results.isNullOrEmpty()) {
                    val loc = results[0]
                    val formatted = loc.getAddressLine(0) ?: locationName
                    Triple(loc.latitude, loc.longitude, formatted)
                } else {
                    Triple(null, null, locationName)
                }

                runOnUiThread {
                    b.inputLocation.setText(resolvedName)

                    if (lat == null || lng == null) {
                        Toast.makeText(
                            this,
                            "Could not find that address exactly. Saving without coordinates.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    saveRaceToFirestore(
                        name = name,
                        description = description,
                        distanceMiles = distanceMiles,
                        visibility = visibility,
                        ownerUid = ownerUid,
                        ownerName = ownerName,
                        locationName = resolvedName,
                        startLat = lat,
                        startLng = lng
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "Error looking up address. Saving track without location.",
                        Toast.LENGTH_SHORT
                    ).show()

                    saveRaceToFirestore(
                        name = name,
                        description = description,
                        distanceMiles = distanceMiles,
                        visibility = visibility,
                        ownerUid = ownerUid,
                        ownerName = ownerName,
                        locationName = locationName,
                        startLat = null,
                        startLng = null
                    )
                }
            }
        }
    }

    // Final write to Firestore (same collection your TracksActivity uses)
    private fun saveRaceToFirestore(
        name: String,
        description: String,
        distanceMiles: Double,
        visibility: String,
        ownerUid: String,
        ownerName: String,
        locationName: String?,
        startLat: Double?,
        startLng: Double?
    ) {
        val data = mutableMapOf<String, Any?>(
            "name" to name,
            "description" to description,
            "distanceMiles" to distanceMiles,
            "visibility" to visibility,
            "ownerUid" to ownerUid,
            "ownerName" to ownerName,
            "createdAt" to Timestamp.now(),
            "type" to ""
        )

        if (!locationName.isNullOrBlank()) {
            data["locationName"] = locationName
        }

        if (startLat != null && startLng != null) {
            data["startLat"] = startLat
            data["startLng"] = startLng
        }

        db.collection("publicRaces")
            .add(data)
            .addOnSuccessListener {
                Toast.makeText(this, "Track saved!", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                Toast.makeText(
                    this,
                    "Failed to save track: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                b.btnSave.isEnabled = true
            }
    }
}
