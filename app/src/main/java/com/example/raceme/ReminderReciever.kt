package com.example.raceme

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

// receives alarm and shows notification
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "RaceMe"
        val text = intent.getStringExtra("text") ?: "Ready for your run?"

        val channelId = "raceme_reminders"
        createChannelIfNeeded(context, channelId)

        val notif = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .build()

        with(NotificationManagerCompat.from(context)) {
            notify(2001, notif)
        }
    }

    // create notification channel
    private fun createChannelIfNeeded(ctx: Context, id: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(id) == null) {
                val ch = NotificationChannel(id, "RaceMe Reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Daily/weekly run reminders"
                    enableLights(true)
                    lightColor = Color.RED
                }
                nm.createNotificationChannel(ch)
            }
        }
    }
}
