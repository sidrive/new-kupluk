package com.ikun.waktusholat.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "prayer_settings")

/**
 * Pengganti GlobalData.java (SharedPreferences) di app lama.
 * Menyimpan lokasi terpilih (GPS atau manual), metode kalkulasi, madzhab,
 * dan koreksi manual per waktu — semua lewat DataStore (async, type-safe).
 */
class PrayerSettingsRepository(private val context: Context) {

    private object Keys {
        val LATITUDE = doublePreferencesKey("latitude")
        val LONGITUDE = doublePreferencesKey("longitude")
        val ELEVATION = doublePreferencesKey("elevation")
        val CITY_NAME = stringPreferencesKey("city_name")
        val LOCATION_MODE = stringPreferencesKey("location_mode") // "AUTO" atau "MANUAL"
        val CALCULATION_METHOD = stringPreferencesKey("calculation_method")
        val MADHAB = stringPreferencesKey("madhab")
        val ADZAN_SOUND_ID = stringPreferencesKey("adzan_sound_id")
        val ADZAN_SOUND_CUSTOM_URI = stringPreferencesKey("adzan_sound_custom_uri")
        fun correction(id: PrayerId) = intPreferencesKey("correction_${id.name}")
    }

    val settingsFlow: Flow<PrayerSettings> = context.dataStore.data.map { prefs ->
        prefs.toSettings()
    }

    val locationMode: Flow<LocationMode> = context.dataStore.data.map { prefs ->
        LocationMode.valueOf(prefs[Keys.LOCATION_MODE] ?: LocationMode.AUTO.name)
    }

    val cityName: Flow<String?> = context.dataStore.data.map { prefs -> prefs[Keys.CITY_NAME] }

    /** [AdzanSoundCatalog.builtIn] id, atau [AdzanSoundCatalog.CUSTOM_ID] kalau user pakai file sendiri. */
    val adzanSound: Flow<AdzanSoundSetting> = context.dataStore.data.map { prefs ->
        AdzanSoundSetting(
            soundId = prefs[Keys.ADZAN_SOUND_ID] ?: AdzanSoundCatalog.defaultId,
            customUri = prefs[Keys.ADZAN_SOUND_CUSTOM_URI],
        )
    }

    suspend fun currentSettings(): PrayerSettings = context.dataStore.data.first().toSettings()

    suspend fun currentAdzanSound(): AdzanSoundSetting = adzanSound.first()

    /** Pilih salah satu suara bawaan dari [AdzanSoundCatalog.builtIn]. */
    suspend fun saveAdzanSound(soundId: String) {
        context.dataStore.edit { prefs -> prefs[Keys.ADZAN_SOUND_ID] = soundId }
    }

    /**
     * Pilih file suara sendiri. [uri] harus sudah diberi
     * `takePersistableUriPermission` supaya tetap bisa dibaca setelah app di-restart.
     */
    suspend fun saveCustomAdzanSound(uri: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ADZAN_SOUND_ID] = AdzanSoundCatalog.CUSTOM_ID
            prefs[Keys.ADZAN_SOUND_CUSTOM_URI] = uri
        }
    }

    suspend fun saveLocation(latitude: Double, longitude: Double, elevation: Double = 0.0, cityName: String? = null) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LATITUDE] = latitude
            prefs[Keys.LONGITUDE] = longitude
            prefs[Keys.ELEVATION] = elevation
            if (cityName != null) prefs[Keys.CITY_NAME] = cityName
        }
    }

    suspend fun saveLocationMode(mode: LocationMode) {
        context.dataStore.edit { prefs -> prefs[Keys.LOCATION_MODE] = mode.name }
    }

    /** Dipanggil dari layar Pengaturan saat user memasukkan koordinat kota sendiri. */
    suspend fun setManualLocation(latitude: Double, longitude: Double) {
        saveLocationMode(LocationMode.MANUAL)
        saveLocation(latitude, longitude)
    }

    /** Dipanggil saat user memilih kembali ke GPS otomatis. */
    suspend fun useAutoLocation() {
        saveLocationMode(LocationMode.AUTO)
    }

    suspend fun saveCalculationMethod(method: CalculationMethodOption) {
        context.dataStore.edit { prefs -> prefs[Keys.CALCULATION_METHOD] = method.name }
    }

    suspend fun saveMadhab(madhab: MadhabOption) {
        context.dataStore.edit { prefs -> prefs[Keys.MADHAB] = madhab.name }
    }

    suspend fun saveCorrection(id: PrayerId, minutes: Int) {
        context.dataStore.edit { prefs -> prefs[Keys.correction(id)] = minutes }
    }

    private fun Preferences.toSettings(): PrayerSettings {
        val corrections = PrayerId.entries.associateWith { id -> this[Keys.correction(id)] ?: 0 }
        return PrayerSettings(
            latitude = this[Keys.LATITUDE] ?: DEFAULT_LATITUDE,
            longitude = this[Keys.LONGITUDE] ?: DEFAULT_LONGITUDE,
            elevation = this[Keys.ELEVATION] ?: 0.0,
            calculationMethod = this[Keys.CALCULATION_METHOD]?.let { runCatching { CalculationMethodOption.valueOf(it) }.getOrNull() }
                ?: CalculationMethodOption.KEMENAG,
            madhabAshar = this[Keys.MADHAB]?.let { runCatching { MadhabOption.valueOf(it) }.getOrNull() }
                ?: MadhabOption.SYAFII,
            correctionMinutes = corrections,
        )
    }

    companion object {
        // Default awal: Jakarta — dipakai hanya sebelum lokasi GPS/manual pertama tersimpan.
        const val DEFAULT_LATITUDE = -6.2
        const val DEFAULT_LONGITUDE = 106.8167
    }
}

enum class LocationMode { AUTO, MANUAL }
