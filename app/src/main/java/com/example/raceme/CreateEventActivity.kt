package com.example.raceme

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CreateEventActivity : AppCompatActivity() {

    private lateinit var editTitle: EditText
    private lateinit var editDescription: EditText
    private lateinit var editAddress: EditText
    private lateinit var buttonPickDate: Button
    private lateinit var buttonPickTime: Button
    private lateinit var textSelectedDateTime: TextView
    private lateinit var buttonCreate: Button
    private lateinit var buttonCancel: Button

    private val calendar: Calendar = Calendar.getInstance()
    private var selectedTimestamp: Timestamp? = null

    private val db = FirebaseFirestore.getInstance()
    private val eventsRef = db.collection("events")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_event)

        editTitle = findViewById(R.id.editEventTitle)
        editDescription = findViewById(R.id.editEventDescription)
        editAddress = findViewById(R.id.editEventAddress)
        buttonPickDate = findViewById(R.id.buttonPickDate)
        buttonPickTime = findViewById(R.id.buttonPickTime)
        textSelectedDateTime = findViewById(R.id.textSelectedDateTime)
        buttonCreate = findViewById(R.id.buttonCreateEvent)
        buttonCancel = findViewById(R.id.buttonCancel)

        buttonPickDate.setOnClickListener { showDatePicker() }
        buttonPickTime.setOnClickListener { showTimePicker() }

        buttonCreate.setOnClickListener { createEvent() }
        buttonCancel.setOnClickListener { finish() }
    }

    private fun showDatePicker() {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, y, m, d ->
            calendar.set(Calendar.YEAR, y)
            calendar.set(Calendar.MONTH, m)
            calendar.set(Calendar.DAY_OF_MONTH, d)
            updateSelectedDateTime()
        }, year, month, day).show()
    }

    private fun showTimePicker() {
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(this, { _, h, m ->
            calendar.set(Calendar.HOUR_OF_DAY, h)
            calendar.set(Calendar.MINUTE, m)
            updateSelectedDateTime()
        }, hour, minute, false).show()
    }

    private fun updateSelectedDateTime() {
        selectedTimestamp = Timestamp(calendar.time)
        val formatter = SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.getDefault())
        val formatted = formatter.format(calendar.time)
        textSelectedDateTime.text = "Selected: $formatted"
    }

    private fun createEvent() {
        val title = editTitle.text.toString().trim()
        val description = editDescription.text.toString().trim()
        val address = editAddress.text.toString().trim()
        val startTime = selectedTimestamp

        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a title", Toast.LENGTH_SHORT).show()
            return
        }

        if (address.isEmpty()) {
            Toast.makeText(this, "Please enter an address/location", Toast.LENGTH_SHORT).show()
            return
        }

        if (startTime == null) {
            Toast.makeText(this, "Please pick a date and time", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId == null) {
            Toast.makeText(this, "You must be logged in to create an event", Toast.LENGTH_SHORT).show()
            return
        }

        val docRef = eventsRef.document()
        val event = RaceEvent(
            id = docRef.id,
            title = title,
            description = description,
            locationName = address,
            startTime = startTime,
            createdByUserId = currentUserId,
            createdAt = Timestamp.now(),
            interestedUserIds = emptyList()
        )

        docRef.set(event)
            .addOnSuccessListener {
                Toast.makeText(this, "Event created!", Toast.LENGTH_SHORT).show()
                finish() // go back to EventsActivity
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
