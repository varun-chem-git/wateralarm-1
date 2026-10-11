package com.waterbuddy.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

/** Stores settings and (re)schedules the next alarm. */
object Scheduler {
    private const val PREFS = "water_prefs"
    private const val K_ENABLED = "enabled"
    private const val K_HOUR = "start_hour"
    private const val K_MIN = "start_min"
    private const val K_PAUSED_UNTIL = "paused_until"

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isEnabled(c: Context) = prefs(c).getBoolean(K_ENABLED, false)
    fun startHour(c: Context) = prefs(c).getInt(K_HOUR, 9)
    fun startMinute(c: Context) = prefs(c).getInt(K_MIN, 0)
    fun pausedUntil(c: Context) = prefs(c).getLong(K_PAUSED_UNTIL, 0L)
    fun isPaused(c: Context) = pausedUntil(c) > System.currentTimeMillis()

    fun setStartTime(c: Context, hour: Int, minute: Int) {
        prefs(c).edit().putInt(K_HOUR, hour).putInt(K_MIN, minute).apply()
    }

    fun enable(c: Context) {
        prefs(c).edit().putBoolean(K_ENABLED, true).putLong(K_PAUSED_UNTIL, 0L).apply()
        scheduleNext(c)
    }

    fun disable(c: Context) {
        prefs(c).edit().putBoolean(K_ENABLED, false).putLong(K_PAUSED_UNTIL, 0L).apply()
        alarmManager(c).cancel(pendingIntent(c))
    }

    fun pauseFor(c: Context, minutes: Long) {
        prefs(c).edit().putLong(K_PAUSED_UNTIL, System.currentTimeMillis() + minutes * 60_000L).apply()
    }

    fun resume(c: Context) {
        prefs(c).edit().putLong(K_PAUSED_UNTIL, 0L).apply()
    }

    /**
     * Alarms sit on a grid: start time, start time + 20 min, + 40 min ...
     * This returns the first grid point that is still in the future.
     */
    fun nextTriggerMillis(c: Context, now: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, startHour(c))
            set(Calendar.MINUTE, startMinute(c))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        var t = cal.timeInMillis
        val step = Config.INTERVAL_MINUTES * 60_000L
        if (t <= now) {
            t += ((now - t) / step + 1) * step
        }
        return t
    }

    fun scheduleNext(c: Context) {
        val am = alarmManager(c)
        val at = nextTriggerMillis(c)
        val pi = pendingIntent(c)
        if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    private fun alarmManager(c: Context) = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun pendingIntent(c: Context): PendingIntent {
        val i = Intent(c, AlarmReceiver::class.java).setAction("com.waterbuddy.app.ALARM")
        return PendingIntent.getBroadcast(
            c, 1001, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
