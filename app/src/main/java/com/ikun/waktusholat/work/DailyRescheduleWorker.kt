package com.ikun.waktusholat.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ikun.waktusholat.alarm.AlarmScheduler
import com.ikun.waktusholat.data.PrayerSettingsRepository
import com.ikun.waktusholat.data.PrayerTimesRepository
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Exact alarm di Android hanya dijadwalkan sekali per waktu shalat, bukan
 * berulang — jadi tiap hari perlu ada yang menghitung ulang & menjadwalkan
 * ulang jadwal hari itu. Worker ini reschedule alarm untuk hari berjalan,
 * lalu menjadwalkan dirinya sendiri lagi untuk beberapa menit setelah
 * tengah malam berikutnya (chained one-time work, bukan PeriodicWorkRequest,
 * supaya waktunya bisa presisi "beberapa menit setelah jam 00:00" alih-alih
 * terikat ke jam saat worker pertama kali di-enqueue).
 */
class DailyRescheduleWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val settings = PrayerSettingsRepository(applicationContext).currentSettings()
        val prayerTimes = PrayerTimesRepository().calculate(settings)
        AlarmScheduler(applicationContext).scheduleAll(prayerTimes)

        enqueueNext(applicationContext)
        return Result.success()
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "daily_reschedule"

        /** Jadwalkan reschedule berikutnya, [minutesAfterMidnight] menit setelah tengah malam. */
        fun enqueueNext(context: Context, minutesAfterMidnight: Long = 5) {
            val now = Calendar.getInstance()
            val nextMidnight = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, minutesAfterMidnight.toInt())
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val delay = nextMidnight.timeInMillis - now.timeInMillis

            val request = OneTimeWorkRequestBuilder<DailyRescheduleWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }

        /** Panggil sekali saat app start / setelah boot untuk memastikan chain-nya aktif. */
        fun ensureScheduled(context: Context) {
            WorkManager.getInstance(context)
                .enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.KEEP, buildInitialRequest())
        }

        private fun buildInitialRequest() = run {
            val now = Calendar.getInstance()
            val nextMidnight = Calendar.getInstance().apply {
                if (now.get(Calendar.HOUR_OF_DAY) >= 0) add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 5)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val delay = (nextMidnight.timeInMillis - now.timeInMillis).coerceAtLeast(0)
            OneTimeWorkRequestBuilder<DailyRescheduleWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()
        }
    }
}
