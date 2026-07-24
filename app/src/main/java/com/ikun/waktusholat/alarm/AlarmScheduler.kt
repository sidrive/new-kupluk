package com.ikun.waktusholat.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.ikun.waktusholat.data.PrayerId
import com.ikun.waktusholat.data.PrayerTime

/**
 * Pengganti AlarmService.java + WaktuShalatReceiver.java di app lama.
 *
 * Perbedaan penting vs app lama (yang targetSdk 18):
 * - Android 6+ (Doze mode): wajib pakai `setExactAndAllowWhileIdle` supaya alarm
 *   tetap bunyi walau HP dalam mode hemat baterai/layar mati lama.
 * - Android 12+ (API 31): exact alarm butuh izin eksplisit `SCHEDULE_EXACT_ALARM`,
 *   dan bisa dicabut user dari Settings. Selalu cek `canScheduleExactAlarms()`
 *   sebelum schedule, dan sediakan fallback (inexact alarm / notification saja)
 *   kalau izin ditolak.
 * - Reschedule dipanggil ulang tiap hari (mis. dari BootCompletedReceiver atau
 *   worker harian) karena exact alarm di Android tidak otomatis berulang presisi.
 */
class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    /** Jadwalkan alarm adzan untuk seluruh waktu shalat hari ini yang belum lewat. */
    fun scheduleAll(prayerTimes: List<PrayerTime>) {
        val now = System.currentTimeMillis()
        prayerTimes.forEach { prayer ->
            if (prayer.id == PrayerId.TERBIT) return@forEach // terbit = reminder, bukan waktu adzan
            if (prayer.time.time > now) {
                schedule(prayer)
            }
        }
    }

    private fun schedule(prayer: PrayerTime) {
        val intent = Intent(context, AdzanAlarmReceiver::class.java).apply {
            putExtra(EXTRA_PRAYER_ID, prayer.id.name)
            putExtra(EXTRA_PRAYER_LABEL, prayer.label)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            prayer.id.ordinal,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        if (canScheduleExact()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                prayer.time.time,
                pendingIntent,
            )
        } else {
            // Fallback kalau user menolak izin exact alarm — tetap lebih baik
            // daripada tidak bunyi sama sekali, walau waktunya bisa meleset.
            alarmManager.setWindow(
                AlarmManager.RTC_WAKEUP,
                prayer.time.time,
                5 * 60_000L,
                pendingIntent,
            )
        }
    }

    fun cancelAll() {
        PrayerId.entries.forEach { id ->
            val intent = Intent(context, AdzanAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, id.ordinal, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            alarmManager.cancel(pendingIntent)
        }
    }

    companion object {
        const val EXTRA_PRAYER_ID = "extra_prayer_id"
        const val EXTRA_PRAYER_LABEL = "extra_prayer_label"
    }
}
