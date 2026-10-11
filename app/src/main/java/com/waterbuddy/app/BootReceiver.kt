package com.waterbuddy.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Re-arms the alarm after a reboot, app update, or clock/timezone change. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Scheduler.isEnabled(context)) Scheduler.scheduleNext(context)
    }
}
