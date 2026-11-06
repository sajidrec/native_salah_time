package com.infinitybd71.salah_time.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.infinitybd71.salah_time.R
import com.infinitybd71.salah_time.ui.HomeActivity
import com.infinitybd71.salah_time.utils.AlarmPrefs

class AlarmService : Service() {

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var wakeLock: PowerManager.WakeLock
    private var currentPrayerName: String = ""

    companion object {
        const val CHANNEL_ID = "salah_time_alarm_service"
        const val CHANNEL_NAME = "Salah Time Alarm Service"
        const val ACTION_STOP_ALARM = "com.infinitybd71.salah_time.STOP_ALARM"
        const val NOTIFICATION_ID = 2001

        fun getAlarmSoundUri(): Uri {
            // Use custom adhan sound from res/raw/
            return Uri.parse("android.resource://com.infinitybd71.salah_time/${R.raw.adhan}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_ALARM) {
            stopSelf()
            return START_NOT_STICKY
        }

        // Store prayer name for cleanup on destroy
        currentPrayerName = intent?.getStringExtra(AlarmReceiver.EXTRA_PRAYER_NAME) ?: ""

        // Android 14+ (API 34+): FOREGROUND_SERVICE_SPECIAL_USE permission is required.
        // Must match the permission declared in AndroidManifest.xml
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (checkSelfPermission("android.permission.FOREGROUND_SERVICE_SPECIAL_USE")
                != PackageManager.PERMISSION_GRANTED
            ) {
                stopSelf()
                return START_NOT_STICKY
            }
        }

        createNotificationChannel()
        startMediaPlayer()

        // Android 14+ (API 34+): Must pass the foreground service type that matches
        // android:foregroundServiceType="specialUse" in the manifest.
        // ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE = 0x40 (added in API 34, not available at minSdk 24)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                buildNotification(),
                0x40 // FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, buildNotification())
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopMediaPlayer()
        releaseWakeLock()

        // Auto-reset alarm state so bell shows unset after alarm has rung
        if (currentPrayerName.isNotEmpty()) {
            val alarmPrefs = AlarmPrefs(applicationContext)
            alarmPrefs.set(currentPrayerName, false)
            alarmPrefs.setTime(currentPrayerName, null)
        }

        super.onDestroy()
    }

    private fun startMediaPlayer() {
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            setLooping(true)
            setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)

            try {
                setDataSource(applicationContext, AlarmService.getAlarmSoundUri())
                prepare()
                start()
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback to system alarm sound
                try {
                    reset()
                    setDataSource(
                        applicationContext,
                        android.media.RingtoneManager.getDefaultUri(
                            android.media.RingtoneManager.TYPE_ALARM
                        )
                    )
                    prepare()
                    start()
                } catch (e2: Exception) {
                    e2.printStackTrace()
                }
            }
        }

        acquireWakeLock()
    }

    private fun stopMediaPlayer() {
        mediaPlayer.run {
            if (isPlaying) stop()
            release()
        }
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "SalahTime:AlarmLock"
        )
        wakeLock.acquire()
    }

    private fun releaseWakeLock() {
        wakeLock.run {
            if (isHeld) release()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Active prayer alarm"
                enableLights(true)
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, StopAlarmReceiver::class.java).apply {
            action = AlarmService.ACTION_STOP_ALARM
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openIntent = Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Adhan")
            .setContentText("Tap to stop alarm")
            .setAutoCancel(false)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(openPendingIntent, true)
            .addAction(
                NotificationCompat.Action(
                    android.R.drawable.ic_input_delete,
                    "Stop",
                    stopPendingIntent
                )
            )
            .setContentIntent(openPendingIntent)
            .build()
    }
}