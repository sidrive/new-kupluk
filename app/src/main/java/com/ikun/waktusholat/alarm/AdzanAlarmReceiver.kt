package com.ikun.waktusholat.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.ikun.waktusholat.service.AdzanPlayerService

/**
 * Pengganti WaktuShalatReceiver.java lama.
 * Dipicu AlarmManager tepat saat masuk waktu shalat -> start foreground
 * service untuk memutar suara adzan + tampilkan notifikasi.
 */
class AdzanAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prayerLabel = intent.getStringExtra(AlarmScheduler.EXTRA_PRAYER_LABEL) ?: "Waktu Shalat"

        val serviceIntent = Intent(context, AdzanPlayerService::class.java).apply {
            putExtra(AdzanPlayerService.EXTRA_PRAYER_LABEL, prayerLabel)
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}
