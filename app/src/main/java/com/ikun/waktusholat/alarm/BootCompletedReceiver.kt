package com.ikun.waktusholat.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ikun.waktusholat.data.PrayerSettingsRepository
import com.ikun.waktusholat.data.PrayerTimesRepository
import com.ikun.waktusholat.work.DailyRescheduleWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Pengganti AfterBootReceiver.java lama.
 * Alarm exact di Android otomatis hilang tiap reboot HP, jadi wajib
 * di-reschedule ulang dari sini, memakai lokasi & pengaturan terakhir yang
 * disimpan user di DataStore (bukan placeholder Jakarta lagi).
 */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        // Reschedule butuh baca DataStore (suspend) — BroadcastReceiver harus
        // pakai goAsync() supaya proses tidak dibunuh sistem sebelum selesai.
        val pendingResult: PendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = PrayerSettingsRepository(context).currentSettings()
                val prayerTimes = PrayerTimesRepository().calculate(settings)
                AlarmScheduler(context).scheduleAll(prayerTimes)
                DailyRescheduleWorker.ensureScheduled(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
