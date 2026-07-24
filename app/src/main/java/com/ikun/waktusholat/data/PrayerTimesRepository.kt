package com.ikun.waktusholat.data

import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.DateComponents
import com.batoulapps.adhan.Madhab
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.Qibla
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

/**
 * Pembungkus library `adhan` (com.batoulapps.adhan:adhan) — pengganti
 * ShalatEngine.java/PerhitunganShalat1.java di app lama. Rumus astronomi
 * yang dipakai sama persis (deklinasi matahari + equation of time),
 * hanya di sini pakai implementasi yang sudah teruji & terawat.
 */
class PrayerTimesRepository {

    fun calculate(settings: PrayerSettings, date: Date = Date()): List<PrayerTime> {
        val coordinates = Coordinates(settings.latitude, settings.longitude)

        val calendar = Calendar.getInstance().apply { time = date }
        val dateComponents = DateComponents(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH),
        )

        val params = settings.calculationMethod.toAdhanMethod().parameters
        params.madhab = if (settings.madhabAshar == MadhabOption.HANAFI) Madhab.HANAFI else Madhab.SHAFI

        val prayerTimes = PrayerTimes(coordinates, dateComponents, params)

        fun withCorrection(id: PrayerId, raw: Date): Date {
            val minutes = settings.correctionMinutes[id] ?: 0
            if (minutes == 0) return raw
            return Date(raw.time + minutes * 60_000L)
        }

        return listOf(
            PrayerTime(PrayerId.SUBUH, "Subuh", withCorrection(PrayerId.SUBUH, prayerTimes.fajr)),
            PrayerTime(PrayerId.TERBIT, "Terbit", withCorrection(PrayerId.TERBIT, prayerTimes.sunrise)),
            PrayerTime(PrayerId.ZUHUR, "Zuhur", withCorrection(PrayerId.ZUHUR, prayerTimes.dhuhr)),
            PrayerTime(PrayerId.ASHAR, "Ashar", withCorrection(PrayerId.ASHAR, prayerTimes.asr)),
            PrayerTime(PrayerId.MAGHRIB, "Maghrib", withCorrection(PrayerId.MAGHRIB, prayerTimes.maghrib)),
            PrayerTime(PrayerId.ISYA, "Isya", withCorrection(PrayerId.ISYA, prayerTimes.isha)),
        )
    }

    /** Arah kiblat dalam derajat dari utara — pengganti KiblatView.java lama */
    fun qiblaDirection(settings: PrayerSettings): Double {
        val coordinates = Coordinates(settings.latitude, settings.longitude)
        return Qibla(coordinates).direction
    }

    private fun CalculationMethodOption.toAdhanMethod(): CalculationMethod = when (this) {
        CalculationMethodOption.KEMENAG -> CalculationMethod.MOON_SIGHTING_COMMITTEE // paling dekat 20°/18°; lihat catatan di README
        CalculationMethodOption.MWL -> CalculationMethod.MUSLIM_WORLD_LEAGUE
        CalculationMethodOption.ISNA -> CalculationMethod.NORTH_AMERICA
        CalculationMethodOption.UMM_AL_QURA -> CalculationMethod.UMM_AL_QURA
        CalculationMethodOption.EGYPTIAN -> CalculationMethod.EGYPTIAN
        CalculationMethodOption.KARACHI -> CalculationMethod.KARACHI
    }
}
