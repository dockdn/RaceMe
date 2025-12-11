package com.example.raceme

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.raceme.databinding.ActivityTracksBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class TracksActivity : BaseActivity() {

    private lateinit var b: ActivityTracksBinding
    private val db by lazy { FirebaseFirestore.getInstance() }
    private lateinit var adapter: TracksAdapter

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var userLat: Double? = null
    private var userLng: Double? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityTracksBinding.inflate(layoutInflater)
        setContentView(b.root)

        adapter = TracksAdapter { track ->
            val data = Intent()
                .putExtra("selected_track_name", track.name)
                .putExtra("selected_public_race_id", track.id)
            setResult(Activity.RESULT_OK, data)
            finish()
        }

        b.rvTracks.layoutManager = LinearLayoutManager(this)
        b.rvTracks.adapter = adapter

        b.btnBackTracks.setOnClickListener { finish() }

        b.inputSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.filter(s?.toString().orEmpty())
            }
        })

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        tryGetUserLocationThenFetch()
    }

    private fun tryGetUserLocationThenFetch() {
        val fine = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val coarse = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        userLat = location.latitude
                        userLng = location.longitude
                    }
                    fetchPublicTracks()
                }
                .addOnFailureListener {
                    fetchPublicTracks()
                }
        } else {
            fetchPublicTracks()
        }
    }

    private fun fetchPublicTracks() {
        b.progressTracks.visibility = View.VISIBLE
        b.tvEmptyTracks.visibility = View.GONE

        db.collection("publicRaces")
            .orderBy("name", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, err ->
                b.progressTracks.visibility = View.GONE

                if (err != null) {
                    Toast.makeText(
                        this,
                        err.message ?: "Failed to load tracks",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addSnapshotListener
                }

                val currentLat = userLat
                val currentLng = userLng

                val distanceMap = mutableMapOf<String, Float>()

                val list = snap?.documents?.map { doc ->
                    val id = doc.id
                    val name = doc.getString("name") ?: ""
                    val type = doc.getString("type") ?: ""
                    val addressText = doc.getString("addressText")

                    val distanceMiles = when {
                        doc.getDouble("distanceMiles") != null ->
                            doc.getDouble("distanceMiles")!!
                        doc.getDouble("distanceMeters") != null ->
                            doc.getDouble("distanceMeters")!! / 1609.344
                        else -> 0.0
                    }

                    val track = Track(
                        id = id,
                        name = name,
                        type = type,
                        distanceMiles = distanceMiles,
                        addressText = addressText,
                        public = true,
                        createdBy = doc.getString("ownerUid"),
                        createdAt = doc.getTimestamp("createdAt")
                    )

                    val lat = doc.getDouble("latitude")
                    val lng = doc.getDouble("longitude")

                    if (currentLat != null && currentLng != null && lat != null && lng != null) {
                        val results = FloatArray(1)
                        Location.distanceBetween(
                            currentLat,
                            currentLng,
                            lat,
                            lng,
                            results
                        )
                        distanceMap[id] = results[0]
                    }

                    track
                }.orEmpty()

                val sorted = if (distanceMap.isNotEmpty()) {
                    list.sortedWith(compareBy { distanceMap[it.id] ?: Float.MAX_VALUE })
                } else {
                    list.sortedBy { it.name.lowercase() }
                }

                if (sorted.isEmpty()) {
                    b.tvEmptyTracks.visibility = View.VISIBLE
                } else {
                    b.tvEmptyTracks.visibility = View.GONE
                }

                adapter.setItems(sorted)
            }
    }
}
