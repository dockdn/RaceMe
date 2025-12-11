package com.example.raceme

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Toast
import com.example.raceme.databinding.ActivityCreateEventBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class CreateEventActivity : BaseActivity() {

    private lateinit var b: ActivityCreateEventBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private var pickedYear: Int? = null
    private var pickedMonth: Int? = null   // 0-based
    private var pickedDay: Int? = null
    private var pickedHour: Int? = null
    private var pickedMinute: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityCreateEventBinding.inflate(layoutInflater)
        setContentView(b.root)

        // back + cancel
        b.btnBackCreateEvent.setOnClickListener { finish() }
        b.buttonCancel.setOnClickListener { finish() }

        // date picker
        b.buttonPickDate.setOnClickListener { showDatePicker() }

        // time picker
        b.buttonPickTime.setOnClickListener { showTimePicker() }

        // create event
        b.buttonCreateEvent.setOnClickListener { createEvent() }
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        val y = pickedYear ?: cal.get(Calendar.YEAR)
        val m = pickedMonth ?: cal.get(Calendar.MONTH)
        val d = pickedDay ?: cal.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, year, month, dayOfMonth ->
            pickedYear = year
            pickedMonth = month
            pickedDay = dayOfMonth
            refreshSelectedDateTimeLabel()
        }, y, m, d).show()
    }

    private fun showTimePicker() {
        val cal = Calendar.getInstance()
        val h = pickedHour ?: cal.get(Calendar.HOUR_OF_DAY)
        val min = pickedMinute ?: cal.get(Calendar.MINUTE)

        TimePickerDialog(this, { _, hourOfDay, minute ->
            pickedHour = hourOfDay
            pickedMinute = minute
            refreshSelectedDateTimeLabel()
        }, h, min, false).show()
    }

    private fun refreshSelectedDateTimeLabel() {
        val y = pickedYear
        val m = pickedMonth
        val d = pickedDay
        val h = pickedHour
        val min = pickedMinute

        if (y != null && m != null && d != null && h != null && min != null) {
            // display as MM/DD • h:mm a
            val cal = Calendar.getInstance().apply {
                set(y, m, d, h, min, 0)
            }
            val month = cal.get(Calendar.MONTH) + 1
            val day = cal.get(Calendar.DAY_OF_MONTH)
            val hour12 = ((cal.get(Calendar.HOUR) - 1 + 12) % 12) + 1
            val ampm = if (cal.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"
            val minuteStr = String.format("%02d", cal.get(Calendar.MINUTE))

            b.textSelectedDateTime.text = "$month/$day • $hour12:$minuteStr $ampm"
        } else if (y != null && m != null && d != null) {
            val month = m + 1
            b.textSelectedDateTime.text = "$month/$d • time not set"
        } else {
            b.textSelectedDateTime.text = "No date/time selected"
        }
    }

    private fun createEvent() {
        val title = b.editEventTitle.text.toString().trim()
        val description = b.editEventDescription.text.toString().trim()

        val street = b.editAddressStreet.text.toString().trim()
        val city = b.editAddressCity.text.toString().trim()
        val state = b.editAddressState.text.toString().trim()
        val zip = b.editAddressZip.text.toString().trim()

        if (title.isBlank()) {
            Toast.makeText(this, "Please enter an event title", Toast.LENGTH_SHORT).show()
            return
        }

        // require date + time
        val y = pickedYear
        val m = pickedMonth
        val d = pickedDay
        val h = pickedHour
        val min = pickedMinute
        if (y == null || m == null || d == null || h == null || min == null) {
            Toast.makeText(this, "Please pick a date and time", Toast.LENGTH_SHORT).show()
            return
        }

        val cal = Calendar.getInstance().apply {
            set(y, m, d, h, min, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = Timestamp(cal.time)

        val addressParts = listOf(street, city, state, zip).filter { it.isNotBlank() }
        val locationName = if (addressParts.isNotEmpty()) {
            addressParts.joinToString(", ")
        } else {
            null
        }

        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "You must be signed in to create an event", Toast.LENGTH_LONG).show()
            return
        }

        val data = hashMapOf<String, Any?>(
            "title" to title,
            "description" to description,
            "locationName" to locationName,
            "addressStreet" to street,
            "addressCity" to city,
            "addressState" to state,
            "addressZip" to zip,
            "startTime" to startTime,
            "ownerUid" to user.uid,
            "ownerName" to (user.displayName ?: user.email ?: "Racer"),
            "createdAt" to FieldValue.serverTimestamp(),
            "interestedUserIds" to emptyList<String>()
        )

        b.buttonCreateEvent.isEnabled = false

        db.collection("events")
            .add(data)
            .addOnSuccessListener {
                Toast.makeText(this, "Event created!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                b.buttonCreateEvent.isEnabled = true
                Toast.makeText(
                    this,
                    "Failed to create event: ${e.message ?: "Unknown error"}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}
