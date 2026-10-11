package com.waterbuddy.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager

object Notifications {
    const val CH_RING = "water_ring"     // silent channel, the service plays the sound itself
    const val CH_ALERT = "water_alert"   // backup channel that makes its own alarm sound
    const val ID_RING = 1
    const val ID_MISSED = 2
    const val ID_ALERT = 3

    fun ensureChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)

        val ring = NotificationChannel(CH_RING, "Water alarm", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Shown while the water alarm is ringing"
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        val alert = NotificationChannel(CH_ALERT, "Water alert (backup)", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Used if the alarm can't ring the normal way"
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        nm.createNotificationChannels(listOf(ring, alert))
    }

    fun build(
        ctx: Context,
        channel: String,
        title: String,
        text: String,
        fullScreen: Boolean = true
    ): Notification {
        val intent = Intent(ctx, AlarmActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pi = PendingIntent.getActivity(
            ctx, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val b = Notification.Builder(ctx, channel)
            .setSmallIcon(R.drawable.ic_stat_drop)
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(Notification.CATEGORY_ALARM)
            .setContentIntent(pi)
            .setAutoCancel(true)
        if (fullScreen) b.setFullScreenIntent(pi, true)
        return b.build()
    }

    /** Shown after the alarm has rung for a while with nobody turning it off. */
    fun postMissed(ctx: Context) {
        ensureChannels(ctx)
        val n = build(ctx, CH_RING, "💧 You missed a water break", "Tap to open", fullScreen = false)
        ctx.getSystemService(NotificationManager::class.java).notify(ID_MISSED, n)
    }

    /** Plain notification (with alarm sound) used if the ringing service can't start. */
    fun postFallback(ctx: Context) {
        ensureChannels(ctx)
        val n = build(ctx, CH_ALERT, "💧 Water time!", "Tap to open")
        ctx.getSystemService(NotificationManager::class.java).notify(ID_ALERT, n)
    }
}
