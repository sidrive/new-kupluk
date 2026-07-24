package com.ikun.waktusholat.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.ikun.waktusholat.MainActivity
import com.ikun.waktusholat.R

/**
 * Notification Channel wajib ada sejak Android 8 (API 26) — ini yang tidak
 * dimiliki app lama (dia masih pakai Notification() + setLatestEventInfo()
 * yang sudah dihapus total dari Android SDK modern, itu sebabnya app lama
 * akan crash kalau dipaksa jalan di HP baru).
 */
class NotificationHelper(private val context: Context) {

    init {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID_ADZAN,
            "Pengingat Adzan",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Notifikasi saat masuk waktu shalat"
        }
        manager.createNotificationChannel(channel)
    }

    fun buildAdzanNotification(prayerLabel: String): Notification {
        val contentIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(context, CHANNEL_ID_ADZAN)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Waktu $prayerLabel telah tiba")
            .setContentText("Yuk, segera tunaikan shalat $prayerLabel")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()
    }

    companion object {
        const val CHANNEL_ID_ADZAN = "channel_adzan"
        const val NOTIFICATION_ID_ADZAN = 100
    }
}
