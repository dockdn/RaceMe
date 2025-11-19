package com.example.raceme

import android.location.Geocoder
import android.os.Bundle
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityCreateRaceBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.btnCancel.setOnClickListener {
            finish()
        }

        b.btnSave.setOnClickListener {
            saveRace()
        }
    }

    private fun saveRace() {
        val name = b.inputName.text.toString().trim()
        val description = b.inputDescription.text.toString().trim()
        val distanceText = b.inputDistanceMiles.text.toString().trim()
        val locationText = b.inputLocation.text.toString().trim()
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

        // If no address given, just save without coords
        if (locationText.isBlank()) {
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
        } else {
            // Geocode on background thread → then save
            geocodeAndSave(
                name = name,
                description = description,
                distanceMiles = distanceMiles,
                visibility = visibility,
                ownerUid = ownerUid,
                ownerName = ownerName,
                locationName = locationText
            )
        }
    }

    private fun geocodeAndSave(
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

                val (lat, lng) = if (!results.isNullOrEmpty()) {
                    val loc = results[0]
                    loc.latitude to loc.longitude
                } else {
                    null to null
                }

                runOnUiThread {
                    saveRaceToFirestore(
                        name = name,
                        description = description,
                        distanceMiles = distanceMiles,
                        visibility = visibility,
                        ownerUid = ownerUid,
                        ownerName = ownerName,
                        locationName = locationName,
                        startLat = lat,
                        startLng = lng
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "Could not find that address. Saving track without location.",
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

        // Text version of the address
        if (!locationName.isNullOrBlank()) {
            data["locationName"] = locationName
        }

        // Real coordinates if geocoding worked
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
