package com.example.raceme

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.RadioButton
import android.widget.TimePicker
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.raceme.databinding.ActivityRemindersBinding
import java.util.Calendar
import android.text.format.DateFormat


class RemindersActivity : BaseActivity() {

    private lateinit var b: ActivityRemindersBinding

    private val notifPermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Toast.makeText(this, "Notifications permission denied", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityRemindersBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.timePicker.setIs24HourView(DateFormat.is24HourFormat(this))

        ensureNotificationPermission()

        // restore saved selection
        val prefs = getSharedPreferences("reminders", Context.MODE_PRIVATE)
        val daily = prefs.getBoolean("daily", true)
        val hour = prefs.getInt("hour", 7)
        val min = prefs.getInt("min", 0)

        (if (daily) b.rbDaily else b.rbWeekly).isChecked = true
        setTimePickerTime(b.timePicker, hour, min)

        b.btnSave.setOnClickListener {
            val isDaily = (findViewById<RadioButton>(b.groupFrequency.checkedRadioButtonId) == b.rbDaily)
            val (h, m) = getTime(b.timePicker)
            saveReminder(isDaily, h, m)
        }

        b.btnCancel.setOnClickListener {
            cancelReminder()
        }
    }

    private fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun getTime(tp: TimePicker): Pair<Int, Int> {
        val h = if (Build.VERSION.SDK_INT >= 23) tp.hour else tp.currentHour
        val m = if (Build.VERSION.SDK_INT >= 23) tp.minute else tp.currentMinute
        return h to m
    }

    private fun setTimePickerTime(tp: TimePicker, h: Int, m: Int) {
        if (Build.VERSION.SDK_INT >= 23) {
            tp.hour = h; tp.minute = m
        } else {
            tp.currentHour = h; tp.currentMinute = m
        }
    }

    private fun saveReminder(daily: Boolean, hour: Int, min: Int) {
        val prefs = getSharedPreferences("reminders", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("daily", daily)
            .putInt("hour", hour)
            .putInt("min", min)
            .apply()

        scheduleAlarm(daily, hour, min)
        Toast.makeText(this, "Reminder saved", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun cancelReminder() {
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(buildPendingIntent())  // cancel any existing
        Toast.makeText(this, "Reminder cancelled", Toast.LENGTH_SHORT).show()
    }

    private fun scheduleAlarm(daily: Boolean, hour: Int, min: Int) {
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = buildPendingIntent()

        val cal = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, min)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        if (daily) {
            // Inexact repeating is fine for reminders (battery-friendly)
            am.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                cal.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pi
            )
        } else {
            // Weekly (7 days)
            am.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                cal.timeInMillis,
                AlarmManager.INTERVAL_DAY * 7,
                pi
            )
        }
    }

    private fun buildPendingIntent(): PendingIntent {
        val intent = Intent(this, ReminderReceiver::class.java)
            .putExtra("title", "Time to run 🏃‍♀️")
            .putExtra("text", "Stay consistent. Small steps win races.")

        val flags = if (Build.VERSION.SDK_INT >= 23)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        else
            PendingIntent.FLAG_UPDATE_CURRENT

        return PendingIntent.getBroadcast(this, 1001, intent, flags)
    }
}
