# Changelog

Format mengikuti [Keep a Changelog](https://keepachangelog.com). Entri terbaru di atas.

## [Unreleased] — 2026-07-25 (branch `claude/checkout-new-kupluk-repo-qzymx4`)

### Added
- `res/raw/adzan.ogg` — mengganti placeholder teks dengan rekaman adzan asli: "Beautiful adhan.ogg" dari Wikimedia Commons, didedikasikan ke public domain (CC0 1.0 Universal), bukan file dari APK "Kupluk" lama. Resource tetap diakses lewat `R.raw.adzan` (nama resource Android tidak bergantung ekstensi file).

### Changed
- `service/AdzanPlayerService.kt` — komentar TODO soal audio placeholder dihapus, diganti keterangan sumber & lisensi file yang sudah terpasang.
- `README.md` / bagian "Yang BELUM ada" — item audio adzan asli dipindah ke "Yang SUDAH ada" karena sudah terpasang.

### Fixed
Ditemukan & diperbaiki lewat build `./gradlew assembleDebug` sungguhan (Android SDK cmdline-tools + platform-35 + build-tools 35 diinstal di sandbox, bukan cuma review manual):
- `data/PrayerTimesRepository.kt` — import `com.batoulapps.adhan.DateComponents` salah paket; class ini sebenarnya ada di `com.batoulapps.adhan.data.DateComponents`.
- `MainActivity.kt` — nama fungsi composable root `WaktuSholatApp()` bentrok dengan class `WaktuSholatApp` (Application) di file lain dalam package yang sama ("Conflicting overloads"). Di-rename jadi `WaktuSholatRoot()`.
- `MainActivity.kt` & `ui/screens/SettingsScreen.kt` — import eksplisit `androidx.compose.foundation.layout.weight` salah resolve ke property internal (`RowColumnParentData.weight`), bukan extension function `RowScope`/`ColumnScope.weight()` publik yang dimaksud. Import tersebut dihapus; `Modifier.weight()` tetap resolve otomatis lewat implicit scope receiver Row/Column.
- Ditambahkan `.gitignore` (build/, .gradle/, local.properties) — sebelumnya belum ada sehingga folder `app/build/` berisiko ter-commit.

Hasil: `gradle assembleDebug` sukses menghasilkan `app-debug.apk` yang bisa diinstal & dijalankan.

## [Unreleased] — 2026-07-24 (branch `claude/checkout-new-kupluk-repo-qzymx4`)

### Added
- Import starting-point project Android "Waktu Sholat" (Kotlin + Jetpack Compose) dari starter zip yang dilampirkan user, dan `KUPLUK_REBUILD_BLUEPRINT.md` (analisis fungsional app lama "Kupluk") sebagai referensi di root repo.
- `data/PrayerSettingsRepository.kt` — DataStore-backed repository untuk lokasi (lat/lon/elevation/mode), metode kalkulasi, madzhab, dan koreksi manual per waktu shalat. Pengganti hardcode Jakarta.
- `location/LocationProvider.kt` — wrapper `FusedLocationProviderClient` untuk deteksi lokasi GPS otomatis (`kotlinx-coroutines-play-services` untuk `Task.await()`).
- `permission/PermissionHelper.kt` — helper cek/minta izin `ACCESS_FINE_LOCATION`, `POST_NOTIFICATIONS` (Android 13+), dan deteksi/deep-link Settings untuk `SCHEDULE_EXACT_ALARM` (Android 12+).
- `ui/screens/SettingsScreen.kt` — layar pengaturan baru: pilih metode perhitungan, madzhab Ashar, koreksi manual per waktu, toggle lokasi GPS otomatis vs manual (input latitude/longitude).
- `work/DailyRescheduleWorker.kt` — chained one-time `WorkManager` request yang reschedule alarm ~5 menit setelah tengah malam tiap hari (exact alarm Android tidak repeating secara native).
- Ikon app baru: `res/drawable/ic_launcher_background.xml`, `ic_launcher_foreground.xml`, `ic_launcher_monochrome.xml` (bulan sabit + bintang, palet hijau/emas), `mipmap-anydpi-v26/ic_launcher_round.xml`, dan `<monochrome>` di `ic_launcher.xml` untuk themed icon Android 13+.
- Dependency baru di `app/build.gradle.kts`: `kotlinx-coroutines-android`, `kotlinx-coroutines-play-services`, `androidx.compose.material:material-icons-core`.

### Changed
- `MainActivity.kt` — dashboard sekarang membaca `PrayerSettings` dari DataStore + GPS (bukan hardcode Jakarta), meminta permission runtime saat dibuka, menampilkan status izin/lokasi, dan menambah navigasi ke `SettingsScreen` (tombol ikon Settings di TopAppBar).
- `alarm/BootCompletedReceiver.kt` — reschedule setelah reboot sekarang membaca lokasi & pengaturan terakhir dari `PrayerSettingsRepository` (pakai `goAsync()` karena baca DataStore itu suspend), bukan placeholder Jakarta; juga memastikan chain `DailyRescheduleWorker` aktif lagi.
- `WaktuSholatApp.kt` — `onCreate()` memanggil `DailyRescheduleWorker.ensureScheduled()`.
- `README.md` — diperbarui: daftar "Yang SUDAH ada" mencakup semua 6 TODO yang sudah dikerjakan; "Yang BELUM ada" dipersempit ke item yang butuh tindakan manual (audio adzan asli) dan fitur blueprint di luar core (Qur'an reader, tasbih, kalender Hijriah, Qibla compass UI).

### Known limitation (butuh tindakan manual, bukan bug)
- `res/raw/adzan.mp3` masih placeholder teks — agent tidak bisa menyediakan file audio berlisensi bebas. Wajib diganti file mp3 asli sebelum build/run di device nyata.
