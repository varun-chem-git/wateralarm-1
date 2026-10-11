package com.waterbuddy.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Called by Android every time an alarm goes off. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!Scheduler.isEnabled(context)) return

        // Always line up the next one first, so the 20-minute chain never breaks.
        Scheduler.scheduleNext(context)

        // Paused (sleeping / in a meeting)? Stay quiet for this one.
        if (Scheduler.isPaused(context)) return

        try {
            context.startForegroundService(Intent(context, AlarmService::class.java))
        } catch (e: Exception) {
            Notifications.postFallback(context)
        }
    }
}
