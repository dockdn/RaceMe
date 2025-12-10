package com.example.raceme

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.location.Geocoder
import android.os.Bundle
import android.widget.Toast
import com.example.raceme.databinding.ActivityCreateEventBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import java.util.Locale
import kotlin.concurrent.thread

class CreateEventActivity : BaseActivity() {

    private lateinit var b: ActivityCreateEventBinding
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    // backing calendar for the event date/time
    private val eventCal: Calendar = Calendar.getInstance()
    private var selectedTimestamp: Timestamp? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityCreateEventBinding.inflate(layoutInflater)
        setContentView(b.root)

        // back + cancel
        b.btnBackCreateEvent.setOnClickListener { finish() }
        b.buttonCancel.setOnClickListener { finish() }

        // date / time
        b.buttonPickDate.setOnClickListener { showDatePicker() }
        b.buttonPickTime.setOnClickListener { showTimePicker() }

        // create
        b.buttonCreateEvent.setOnClickListener { saveEvent() }
    }

    // ===== DATE / TIME PICKERS =====

    private fun showDatePicker() {
        val year = eventCal.get(Calendar.YEAR)
        val month = eventCal.get(Calendar.MONTH)
        val day = eventCal.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, y, m, d ->
            eventCal.set(Calendar.YEAR, y)
            eventCal.set(Calendar.MONTH, m)
            eventCal.set(Calendar.DAY_OF_MONTH, d)
            updateSelectedDateTimeLabel()
        }, year, month, day).show()
    }

    private fun showTimePicker() {
        val hour = eventCal.get(Calendar.HOUR_OF_DAY)
        val minute = eventCal.get(Calendar.MINUTE)

        TimePickerDialog(this, { _, h, min ->
            eventCal.set(Calendar.HOUR_OF_DAY, h)
            eventCal.set(Calendar.MINUTE, min)
            eventCal.set(Calendar.SECOND, 0)
            eventCal.set(Calendar.MILLISECOND, 0)
            updateSelectedDateTimeLabel()
        }, hour, minute, false).show()
    }

    private fun updateSelectedDateTimeLabel() {
        val fmt = java.text.SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault())
        val label = fmt.format(eventCal.time)
        b.textSelectedDateTime.text = label

        selectedTimestamp = Timestamp(eventCal.time)
    }

    // ===== SAVE FLOW =====

    private fun saveEvent() {
        val title = b.editEventTitle.text?.toString()?.trim().orEmpty()
        val description = b.editEventDescription.text?.toString()?.trim().orEmpty()
        val address = b.editEventAddress.text?.toString()?.trim().orEmpty()
        val ts = selectedTimestamp

        if (title.isBlank()) {
            Toast.makeText(this, "Please enter an event title", Toast.LENGTH_SHORT).show()
            return
        }

        if (address.isBlank()) {
            Toast.makeText(this, "Please enter an address / location", Toast.LENGTH_SHORT).show()
            return
        }

        if (ts == null) {
            Toast.makeText(this, "Please pick a date and time", Toast.LENGTH_SHORT).show()
            return
        }

        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "You must be logged in to create an event", Toast.LENGTH_SHORT)
                .show()
            return
        }

        b.buttonCreateEvent.isEnabled = false

        // Forward-geocode the address on a background thread
        geocodeAndSaveEvent(
            title = title,
            description = description,
            address = address,
            ownerUid = user.uid,
            ownerName = user.displayName ?: user.email ?: "Unknown",
            whenTs = ts
        )
    }

    private fun geocodeAndSaveEvent(
        title: String,
        description: String,
        address: String,
        ownerUid: String,
        ownerName: String,
        whenTs: Timestamp
    ) {
        thread {
            var lat: Double? = null
            var lng: Double? = null

            try {
                val geocoder = Geocoder(this, Locale.getDefault())
                val results = geocoder.getFromLocationName(address, 1)

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
                        "Could not resolve that address exactly. Saving event with text only.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                saveEventToFirestore(
                    title = title,
                    description = description,
                    address = address,
                    ownerUid = ownerUid,
                    ownerName = ownerName,
                    whenTs = whenTs,
                    lat = lat,
                    lng = lng
                )
            }
        }
    }

    private fun saveEventToFirestore(
        title: String,
        description: String,
        address: String,
        ownerUid: String,
        ownerName: String,
        whenTs: Timestamp,
        lat: Double?,
        lng: Double?
    ) {
        val data = mutableMapOf<String, Any?>(
            "title" to title,
            "description" to description,
            "addressText" to address,
            "ownerUid" to ownerUid,
            "ownerName" to ownerName,
            "when" to whenTs,
            "createdAt" to Timestamp.now()
        )

        if (lat != null && lng != null) {
            data["latitude"] = lat
            data["longitude"] = lng
        }

        db.collection("events")
            .add(data)
            .addOnSuccessListener {
                Toast.makeText(this, "Event created!", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                Toast.makeText(
                    this,
                    "Failed to create event: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                b.buttonCreateEvent.isEnabled = true
            }
    }
}
