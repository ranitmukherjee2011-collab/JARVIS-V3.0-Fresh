package com.jarvis.v3

import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat

class WakeWordService : Service() {
    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel("jarvis_wake", "JARVIS Wake Service",
            NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        val n = NotificationCompat.Builder(this, "jarvis_wake")
            .setContentTitle("JARVIS is ready")
            .setContentText("Wake service is active where Android permits.")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true).build()
        startForeground(1001, n)
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null
}
