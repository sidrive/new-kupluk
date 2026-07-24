# Blueprint Rebuild — Referensi dari App "Kupluk" (Legacy)

> Dokumen ini adalah hasil **analisis fungsional** dari APK lama (bukan hasil copy source code).
> Tujuan: jadi `PROJECT_CONTEXT.md` untuk memulai project BARU dari nol — kode, aset, nama, dan branding semuanya baru.

## 1. Identitas App Lama (untuk konteks saja)
- Package: `com.mukminin.kupluk`
- Version: 1.3 (versionCode 8), **build Agustus 2013**
- `minSdkVersion=4` (Android 1.6), `targetSdkVersion=18` (Android 4.3) — makanya sudah "sekarat" di Android modern
- Library yang dipakai: ActionBarSherlock, AndroidQuery, GraphView, ViewPagerIndicator — semua sudah deprecated/mati sejak lama
- Menyimpan data Quran & kota di file `KuplukDB.sqlite` (~7MB) dalam assets, dan file `adzan.mp3` untuk suara azan

**Catatan legal:** ikon, nama "Kupluk", file audio adzan asli, dan isi database Quran (kalau pakai terjemahan berlisensi khusus) sebaiknya **tidak** dipakai ulang persis. Ganti dengan nama/ikon/audio/sumber data baru (mudah dicari yang open-license).

## 2. Ternyata Ini Bukan Cuma App Adzan — Ini Aplikasi Islami Lengkap

| Modul | Fitur |
|---|---|
| **Jadwal Shalat** | Hitung 5 waktu shalat + terbit, alarm/notifikasi per waktu, koreksi manual per waktu, pilih madzhab Ashar (Syafi'i/Hanafi), metode perhitungan sudut subuh/isya bisa diganti |
| **Adzan Otomatis** | AlarmManager + BroadcastReceiver, trigger service pemutar suara azan, mode: suara / getar, auto-restart setelah reboot HP |
| **Lokasi** | Pilih kota manual dari database, atau GPS otomatis, simpan koordinat + zona waktu + ketinggian (altitude) |
| **Qibla Finder** | Kompas arah kiblat pakai sensor compass HP |
| **Al-Qur'an Reader** | List 114 surat, baca ayat per surat (arab + terjemahan Indonesia), font Arab custom |
| **Pencarian Qur'an** | Cari ayat, cari kata dalam ayat/terjemahan |
| **Bookmark** | Simpan ayat favorit |
| **Tasbih Digital** | Counter dzikir + reset |
| **Kalender Hijriah** | Konversi tanggal Masehi ↔ Hijriah |
| **Pengaturan** | Semua toggle reminder per waktu shalat, mode notifikasi |
| **About/Feedback** | Halaman info app, changelog, kontak developer |

## 3. Algoritma Perhitungan Waktu Shalat (Referensi — Aman Dipakai)

Ini **algoritma astronomi standar** (dipakai banyak app: Muslim Pro, dll), berbasis rumus deklinasi matahari + equation of time. Bukan kekayaan intelektual eksklusif siapa pun. Referensi teknisnya:

- Hitung **Julian Day** dari tanggal lokal
- Hitung **deklinasi matahari (delta)** dan **equation of time**
- **Waktu Zuhur** = 12 + zona waktu − (longitude/15) − (eq. of time/60) + koreksi
- **Subuh/Isya** pakai sudut matahari di bawah ufuk (default: subuh -20°, isya -18°, tapi ini configurable — beberapa app pakai Kemenag RI 20°/18°, MWL 18°/17°, dst)
- **Ashar** pakai rumus bayangan (mazhab Syafi'i: shadow = 1x, Hanafi: shadow = 2x)
- **Maghrib/Terbit** pakai sudut ufuk -0.833° + koreksi ketinggian (altitude)

Untuk rebuild, **tidak perlu tulis ulang rumus ini dari nol** — sudah ada library modern teruji seperti [`adhan`](https://github.com/batoulapps/adhan) (tersedia untuk JS/Dart/Swift/Kotlin/Python), yang mengimplementasi rumus yang sama plus lebih banyak metode kalkulasi siap pakai (Kemenag, MWL, ISNA, Umm al-Qura, Egyptian, dll).

## 4. Rekomendasi Stack Baru

Berdasarkan kebutuhan utama (**alarm tepat waktu walau app ditutup** + kompatibel Android terbaru), opsi yang paling reliable:

### Opsi A — Native Android (Kotlin + Jetpack Compose) — **paling direkomendasikan**
- `adhan-kotlin` atau `adhan-java` untuk kalkulasi waktu shalat
- `AlarmManager.setExactAndAllowWhileIdle()` + `WorkManager` untuk scheduling adzan tepat waktu (mengatasi Doze Mode Android 12+)
- Notification Channel wajib (Android 8+)
- Foreground Service khusus saat memutar suara adzan
- Room (SQLite modern) untuk data Quran + kota
- Compass sensor API untuk Qibla (sama seperti dulu, API-nya masih ada)

### Opsi B — Flutter
- Package `adhan_dart` + `flutter_local_notifications` (support scheduled + exact alarm)
- Cross-platform kalau suatu saat mau rilis iOS juga
- Development lebih cepat untuk UI kompleks (Qur'an reader, tasbih, dst)

### Opsi C — PWA/Web (kurang disarankan untuk use-case ini)
- Alarm background di Android via browser **tidak reliable** untuk trigger suara tepat waktu kalau app/tab ditutup — butuh app tetap jalan atau push notification server, jadi kurang cocok untuk "wajib bunyi tepat waktu"

## 5. Next Steps di Claude Code
1. Tentukan stack final (A atau B)
2. Scaffold project baru + setup `adhan` library
3. Bangun modul secara bertahap: Jadwal Shalat & Alarm dulu (core value), lalu Qibla, lalu Qur'an reader, baru fitur tambahan (tasbih, kalender hijriah)
4. Cari sumber data Qur'an + terjemahan open-license (misal dari [Quran.com API](https://quran.com/api) atau [Al-Qur'an Cloud API](https://alquran.cloud/api))
5. Cari/ rekam file audio adzan dengan lisensi bebas pakai
6. Desain ulang nama & ikon app (branding baru)
