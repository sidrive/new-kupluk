# Waktu Sholat

Android native (Kotlin + Jetpack Compose) rebuild fungsi "Kupluk" versi modern.
Kode, nama, aset, dan ikon di sini semua baru — bukan hasil copy dari APK lama,
hanya konsep fungsinya yang direplikasi (lihat `KUPLUK_REBUILD_BLUEPRINT.md`
untuk analisis fungsional app lama yang jadi referensi).

## Cara buka project
1. Buka Android Studio (versi Ladybug/2024.2 ke atas direkomendasikan)
2. `Open` → pilih folder root repo ini
3. Biarkan Gradle sync (akan download dependency: Compose, `adhan`, WorkManager, Play Services Location, DataStore, kotlinx-coroutines)
4. Sambungkan device/emulator dengan **API 24+** (Android 7.0 ke atas)

## Yang SUDAH ada
- ✅ Perhitungan 6 waktu shalat pakai library `adhan` (algoritma sama dengan app lama, implementasi lebih teruji)
- ✅ Arah kiblat (`PrayerTimesRepository.qiblaDirection()`)
- ✅ Alarm exact + fallback untuk Doze mode & Android 12+ permission
- ✅ Notification Channel modern (pengganti API lama yang sudah dihapus)
- ✅ Foreground service pemutar suara adzan
- ✅ Reschedule otomatis setelah reboot, memakai lokasi & pengaturan tersimpan (bukan hardcode lagi)
- ✅ **Lokasi dinamis** — `LocationProvider` (FusedLocationProviderClient) mengambil GPS otomatis; hasil & pilihan lokasi manual disimpan lewat `PrayerSettingsRepository` (DataStore)
- ✅ **Runtime permission** — dashboard meminta `ACCESS_FINE_LOCATION`/`ACCESS_COARSE_LOCATION` + `POST_NOTIFICATIONS` (Android 13+) saat dibuka, dan menampilkan tombol ke Settings kalau `SCHEDULE_EXACT_ALARM` ditolak (Android 12+)
- ✅ **Layar Pengaturan** (`ui/screens/SettingsScreen.kt`) — pilih metode perhitungan (Kemenag/MWL/ISNA/Umm al-Qura/Egyptian/Karachi), madzhab Ashar (Syafi'i/Hanafi), koreksi manual per waktu (menit), dan toggle lokasi GPS otomatis vs manual (input latitude/longitude)
- ✅ **Reschedule harian otomatis** — `work/DailyRescheduleWorker.kt`, chained one-time `WorkManager` request yang reschedule alarm ~5 menit setelah tengah malam tiap hari, di-enqueue dari `WaktuSholatApp.onCreate()` dan `BootCompletedReceiver`
- ✅ **Ikon app baru** — adaptive icon custom (bulan sabit + bintang, palet hijau/emas sesuai `ui/theme/Color.kt`) plus varian monokrom untuk themed icon Android 13+

## Yang BELUM ada / butuh tindakan manual
1. **File audio adzan asli** — `res/raw/adzan.mp3` masih placeholder teks (agent tidak bisa menyediakan file audio berlisensi bebas). **Wajib** diganti dengan file mp3 asli sebelum build/run — cari sumber open-license atau rekam sendiri, JANGAN pakai file dari APK "Kupluk" lama.
2. Fitur tambahan sesuai blueprint (`KUPLUK_REBUILD_BLUEPRINT.md`): Qur'an reader, tasbih digital, kalender Hijriah, bookmark ayat, Qibla compass UI — belum dikerjakan, bisa dibangun modul per modul setelah core (jadwal + alarm + pengaturan) stabil.
3. Belum ada test otomatis (unit/instrumented) untuk `PrayerTimesRepository`, `PrayerSettingsRepository`, atau `AlarmScheduler`.

## Catatan soal `CalculationMethodOption.KEMENAG`
Library `adhan` tidak punya preset resmi "Kemenag RI" (sudut 20°/18°). Sementara
di-map ke `MoonSightingCommittee` yang paling dekat, tapi untuk akurasi penuh
sesuai Kemenag sebaiknya buat `CalculationParameters` custom manual:
```kotlin
val params = CalculationParameters(20.0, 18.0)
```
Ini tinggal disesuaikan di `PrayerTimesRepository.toAdhanMethod()`.

## Struktur folder
```
app/src/main/java/com/ikun/waktusholat/
├── MainActivity.kt                    # entry point + navigasi Dashboard <-> Settings (Compose)
├── WaktuSholatApp.kt                  # Application class, enqueue DailyRescheduleWorker
├── data/
│   ├── PrayerTime.kt                  # model data (PrayerTime, PrayerSettings, enum opsi)
│   ├── PrayerTimesRepository.kt       # kalkulasi waktu shalat & kiblat (adhan)
│   └── PrayerSettingsRepository.kt    # DataStore: lokasi, mode lokasi, metode, madzhab, koreksi
├── location/
│   └── LocationProvider.kt            # wrapper FusedLocationProviderClient
├── permission/
│   └── PermissionHelper.kt            # cek & minta izin lokasi/notifikasi/exact alarm
├── alarm/
│   ├── AlarmScheduler.kt              # jadwalkan exact alarm
│   ├── AdzanAlarmReceiver.kt          # dipicu saat waktu shalat tiba
│   └── BootCompletedReceiver.kt       # reschedule setelah reboot (baca DataStore)
├── work/
│   └── DailyRescheduleWorker.kt       # chained WorkManager, reschedule tiap tengah malam
├── service/
│   └── AdzanPlayerService.kt          # foreground service pemutar suara
├── notification/
│   └── NotificationHelper.kt          # notification channel + builder
└── ui/
    ├── theme/                         # Color.kt, Theme.kt
    └── screens/
        └── SettingsScreen.kt          # layar pengaturan
```
