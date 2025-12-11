package com.example.raceme

import android.location.Geocoder
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import com.example.raceme.databinding.ActivityCreateRaceBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale
import kotlin.concurrent.thread

class CreateRaceActivity : BaseActivity() {

    private lateinit var b: ActivityCreateRaceBinding
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private val raceTypes = listOf(
        "5K",
        "10K",
        "Half Marathon",
        "Marathon",
        "Open run"
    )

    private var selectedType: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityCreateRaceBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.btnBackCreateRace.setOnClickListener { finish() }
        b.btnCancel.setOnClickListener { finish() }

        setupTypeDropdown()

        b.btnSave.setOnClickListener {
            saveTrack()
        }
    }

    private fun setupTypeDropdown() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            raceTypes
        )
        b.inputType.setAdapter(adapter)

        b.inputType.setOnItemClickListener { _, _, position, _ ->
            selectedType = raceTypes[position]
        }
    }

    private fun saveTrack() {
        val name = b.inputName.text?.toString()?.trim().orEmpty()
        val description = b.inputDescription.text?.toString()?.trim().orEmpty()
        val distanceText = b.inputDistanceMiles.text?.toString()?.trim().orEmpty()
        val locationText = b.inputLocation.text?.toString()?.trim().orEmpty()
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

        // If user never picked a type, default to Open run
        val typeToSave = selectedType ?: b.inputType.text?.toString()?.trim().takeIf { !it.isNullOrBlank() }
        ?: "Open run"

        b.btnSave.isEnabled = false

        val visibility = if (isPublic) "public" else "private"
        val ownerUid = user.uid
        val ownerName = user.displayName ?: user.email ?: "Unknown"

        if (locationText.isBlank()) {
            saveTrackToFirestore(
                name = name,
                description = description,
                distanceMiles = distanceMiles,
                type = typeToSave,
                visibility = visibility,
                ownerUid = ownerUid,
                ownerName = ownerName,
                addressText = null,
                lat = null,
                lng = null
            )
        } else {
            geocodeAndSaveTrack(
                name = name,
                description = description,
                distanceMiles = distanceMiles,
                type = typeToSave,
                visibility = visibility,
                ownerUid = ownerUid,
                ownerName = ownerName,
                addressText = locationText
            )
        }
    }

    private fun geocodeAndSaveTrack(
        name: String,
        description: String,
        distanceMiles: Double,
        type: String,
        visibility: String,
        ownerUid: String,
        ownerName: String,
        addressText: String
    ) {
        thread {
            var lat: Double? = null
            var lng: Double? = null

            try {
                val geocoder = Geocoder(this, Locale.getDefault())
                val results = geocoder.getFromLocationName(addressText, 1)

                if (!results.isNullOrEmpty()) {
                    val loc = results[0]
                    lat = loc.latitude
                    lng = loc.longitude
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            runOnUiThread {
                if (lat == null || lng == null) {
                    Toast.makeText(
                        this,
                        "Could not resolve that address exactly. Saving track with text only.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                saveTrackToFirestore(
                    name = name,
                    description = description,
                    distanceMiles = distanceMiles,
                    type = type,
                    visibility = visibility,
                    ownerUid = ownerUid,
                    ownerName = ownerName,
                    addressText = addressText,
                    lat = lat,
                    lng = lng
                )
            }
        }
    }

    private fun saveTrackToFirestore(
        name: String,
        description: String,
        distanceMiles: Double,
        type: String,
        visibility: String,
        ownerUid: String,
        ownerName: String,
        addressText: String?,
        lat: Double?,
        lng: Double?
    ) {
        val data = mutableMapOf<String, Any?>(
            "name" to name,
            "description" to description,
            "distanceMiles" to distanceMiles,
            "type" to type,
            "visibility" to visibility,
            "ownerUid" to ownerUid,
            "ownerName" to ownerName,
            "createdAt" to Timestamp.now()
        )

        if (!addressText.isNullOrBlank()) {
            data["addressText"] = addressText
        }

        if (lat != null && lng != null) {
            data["latitude"] = lat
            data["longitude"] = lng
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
