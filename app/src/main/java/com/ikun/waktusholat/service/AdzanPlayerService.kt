package com.ikun.waktusholat.service

import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.IBinder
import com.ikun.waktusholat.R
import com.ikun.waktusholat.notification.NotificationHelper

/**
 * Pengganti AlarmService.java (yang lama namanya membingungkan — sebenarnya
 * itu service pemutar suara, bukan pendaftar alarm).
 *
 * Foreground service WAJIB di Android 8+ untuk audio yang jalan lebih dari
 * beberapa detik di background, kalau tidak sistem akan langsung kill proses.
 */
class AdzanPlayerService : Service() {

    private var mediaPlayer: MediaPlayer? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prayerLabel = intent?.getStringExtra(EXTRA_PRAYER_LABEL) ?: "Waktu Shalat"

        val notification = NotificationHelper(this).buildAdzanNotification(prayerLabel)
        startForeground(NotificationHelper.NOTIFICATION_ID_ADZAN, notification)

        playAdzan()
        return START_NOT_STICKY
    }

    private fun playAdzan() {
        // NOTE: taruh file suara adzan bebas-lisensi di res/raw/adzan.mp3
        // (JANGAN pakai file adzan.mp3 dari APK lama — cari sumber baru,
        // banyak yang free-to-use, atau rekam sendiri).
        mediaPlayer = MediaPlayer.create(this, R.raw.adzan)?.apply {
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
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val EXTRA_PRAYER_LABEL = "extra_prayer_label"
    }
}
