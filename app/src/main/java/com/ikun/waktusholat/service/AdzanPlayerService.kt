package com.ikun.waktusholat.service

import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.IBinder
import com.ikun.waktusholat.R
import com.ikun.waktusholat.data.AdzanSoundCatalog
import com.ikun.waktusholat.data.AdzanSoundSetting
import com.ikun.waktusholat.data.PrayerSettingsRepository
import com.ikun.waktusholat.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Pengganti AlarmService.java (yang lama namanya membingungkan — sebenarnya
 * itu service pemutar suara, bukan pendaftar alarm).
 *
 * Foreground service WAJIB di Android 8+ untuk audio yang jalan lebih dari
 * beberapa detik di background, kalau tidak sistem akan langsung kill proses.
 */
class AdzanPlayerService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prayerLabel = intent?.getStringExtra(EXTRA_PRAYER_LABEL) ?: "Waktu Shalat"

        val notification = NotificationHelper(this).buildAdzanNotification(prayerLabel)
        startForeground(NotificationHelper.NOTIFICATION_ID_ADZAN, notification)

        serviceScope.launch {
            val sound = PrayerSettingsRepository(this@AdzanPlayerService).currentAdzanSound()
            playAdzan(sound)
        }
        return START_NOT_STICKY
    }

    private fun playAdzan(sound: AdzanSoundSetting) {
        // res/raw/adzan*.{ogg,mp3} — semua rekaman CC0/domain publik dari
        // Wikimedia Commons (lihat AdzanSoundCatalog), bukan file dari APK
        // "Kupluk" lama. User juga bisa pilih file sendiri lewat Pengaturan.
        val player = if (sound.soundId == AdzanSoundCatalog.CUSTOM_ID && sound.customUri != null) {
            runCatching { MediaPlayer.create(this, Uri.parse(sound.customUri)) }.getOrNull()
                ?: MediaPlayer.create(this, R.raw.adzan) // file custom sudah tak terbaca -> fallback bawaan
        } else {
            MediaPlayer.create(this, AdzanSoundCatalog.rawResIdFor(sound.soundId))
        }

        mediaPlayer = player?.apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            setOnCompletionListener { stopSelf() }
            start()
        }
    }

    override fun onDestroy() {
        mediaPlayer?.release()
        mediaPlayer = null
        serviceJob.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val EXTRA_PRAYER_LABEL = "extra_prayer_label"
    }
}
