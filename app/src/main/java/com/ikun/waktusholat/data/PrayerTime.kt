package com.ikun.waktusholat.data

import java.util.Date

/** Satu waktu shalat: nama + jam-nya + apakah reminder aktif */
data class PrayerTime(
    val id: PrayerId,
    val label: String,
    val time: Date,
)

enum class PrayerId {
    SUBUH, TERBIT, ZUHUR, ASHAR, MAGHRIB, ISYA
}

/** Pengaturan lokasi & metode — pengganti GlobalData.java di app lama */
data class PrayerSettings(
    val latitude: Double,
    val longitude: Double,
    val elevation: Double = 0.0,
    val calculationMethod: CalculationMethodOption = CalculationMethodOption.KEMENAG,
    val madhabAshar: MadhabOption = MadhabOption.SYAFII,
    // koreksi manual per waktu, dalam menit — setara fitur "koreksi" di app lama
    val correctionMinutes: Map<PrayerId, Int> = emptyMap(),
)

enum class CalculationMethodOption {
    KEMENAG,        // Kementerian Agama RI: subuh 20°, isya 18°
    MWL,            // Muslim World League: 18°, 17°
    ISNA,           // 15°, 15°
    UMM_AL_QURA,    // Mekah: 18.5°, 90 menit setelah maghrib
    EGYPTIAN,       // 19.5°, 17.5°
    KARACHI,        // 18°, 18°
}

enum class MadhabOption {
    SYAFII, // bayangan 1x — dipakai mayoritas Indonesia
    HANAFI, // bayangan 2x
}
