package com.shadowcam.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.content.getSystemService
import com.shadowcam.R

class VirtualCameraService : Service() {

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
        startForeground(
            NOTIFICATION_ID,
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("ShadowCam")
                .setContentText("Virtual camera active")
                .setSmallIcon(R.mipmap.ic_launcher)
                .build()
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService<NotificationManager>()
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Virtual Camera",
                NotificationManager.IMPORTANCE_LOW
            )
            nm?.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "shadowcam_virtual_camera"
        private const val NOTIFICATION_ID = 101
    }
}
