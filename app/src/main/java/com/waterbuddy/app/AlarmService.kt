package com.waterbuddy.app

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** Foreground service that rings + vibrates until the alarm is turned off (or gives up). */
class AlarmService : Service() {
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val handler = Handler(Looper.getMainLooper())

    private val giveUp = Runnable {
        Notifications.postMissed(this)
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Notifications.ensureChannels(this)
        val n = Notifications.build(
            this, Notifications.CH_RING, "💧 Water time!", "Tap to turn off the alarm"
        )
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(Notifications.ID_RING, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(Notifications.ID_RING, n)
            }
        } catch (e: Exception) {
            Notifications.postFallback(this)
            stopSelf()
            return START_NOT_STICKY
        }

        startRinging()
        handler.removeCallbacks(giveUp)
        handler.postDelayed(giveUp, Config.RING_SECONDS * 1000L)
        return START_NOT_STICKY
    }

    @Suppress("DEPRECATION")
    private fun startRinging() {
        stopRinging()

        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        try {
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@AlarmService, uri)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            player?.release()
            player = null
        }

        val v: Vibrator = if (Build.VERSION.SDK_INT >= 31) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 700, 500), 0))
        vibrator = v
    }

    private fun stopRinging() {
        player?.let {
            try {
                if (it.isPlaying) it.stop()
            } catch (e: Exception) {
            }
            it.release()
        }
        player = null
        vibrator?.cancel()
        vibrator = null
    }

    override fun onDestroy() {
        handler.removeCallbacks(giveUp)
        stopRinging()
        super.onDestroy()
    }
}
