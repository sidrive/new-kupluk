package com.ikun.waktusholat

import android.app.Application
import com.ikun.waktusholat.work.DailyRescheduleWorker

class WaktuSholatApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Pastikan chain reschedule harian selalu aktif selama app pernah dibuka sekali.
        DailyRescheduleWorker.ensureScheduled(this)
    }
}
