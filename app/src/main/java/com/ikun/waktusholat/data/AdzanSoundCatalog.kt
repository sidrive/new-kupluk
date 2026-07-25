package com.ikun.waktusholat.data

import com.ikun.waktusholat.R

/**
 * Daftar suara adzan bawaan — semua berlisensi CC0 / domain publik (bebas
 * dipakai tanpa syarat atribusi), diunduh dari Wikimedia Commons:
 * - "default": Beautiful adhan.ogg (CC0 1.0)
 * - "sabah_fakhry": Call to prayer by Sabah Fakhry.mp3 (public domain, PD-1923)
 * - "al_azzan": Oración Al-Azzan.ogg (public domain)
 *
 * ID "custom" bukan bagian dari daftar ini — artinya user memakai file sendiri
 * (URI-nya disimpan terpisah di PrayerSettingsRepository).
 */
object AdzanSoundCatalog {
    const val CUSTOM_ID = "custom"

    data class BuiltInSound(val id: String, val label: String, val rawResId: Int)

    val builtIn = listOf(
        BuiltInSound("default", "Adzan — Wikimedia Commons", R.raw.adzan),
        BuiltInSound("sabah_fakhry", "Adzan — Sabah Fakhry", R.raw.adzan_sabah_fakhry),
        BuiltInSound("al_azzan", "Al-Azzan (singkat)", R.raw.adzan_al_azzan),
    )

    val defaultId: String = builtIn.first().id

    fun rawResIdFor(id: String): Int =
        builtIn.firstOrNull { it.id == id }?.rawResId ?: builtIn.first().rawResId
}
