package com.waterbuddy.app

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.InputType
import android.text.format.DateFormat
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.util.Calendar
import java.util.Date

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var startTimeBtn: Button
    private lateinit var toggleBtn: Button
    private lateinit var fixBtn: Button
    private lateinit var customMinutes: EditText
    private var fixes: List<Pair<String, () -> Unit>> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(28), dp(20), dp(28))
        }

        root.addView(label("💧 Water Buddy", 28f, bold = true))
        status = label("", 18f)
        root.addView(status)

        root.addView(label("1. When should the alarms start?", 16f, bold = true))
        startTimeBtn = button("") { pickStartTime() }
        root.addView(startTimeBtn)

        root.addView(label("2. Switch on (rings every ${Config.INTERVAL_MINUTES} min)", 16f, bold = true))
        toggleBtn = button("") { toggle() }
        root.addView(toggleBtn)

        root.addView(label("Pause alarms (sleeping, meetings...)", 16f, bold = true))
        val presets = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("30 min" to 30L, "1 hr" to 60L, "2 hr" to 120L, "8 hr" to 480L).forEach { (name, mins) ->
            presets.addView(
                button(name) { pause(mins) },
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            )
        }
        root.addView(presets)

        val custom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        customMinutes = EditText(this).apply {
            hint = "custom minutes"
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        custom.addView(customMinutes, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        custom.addView(button("Pause") { pause(customMinutes.text.toString().toLongOrNull() ?: 0L) })
        root.addView(custom)
        root.addView(button("▶ Resume now") { resumeNow() })

        fixBtn = button("") { fixes.firstOrNull()?.second?.invoke() }
        fixBtn.setTextColor(0xFFB71C1C.toInt())
        root.addView(label("", 12f))
        root.addView(fixBtn)

        setContentView(ScrollView(this).apply { addView(root) })

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    // ---------- actions ----------

    private fun pickStartTime() {
        TimePickerDialog(this, { _, h, m ->
            Scheduler.setStartTime(this, h, m)
            if (Scheduler.isEnabled(this)) Scheduler.scheduleNext(this)
            refresh()
        }, Scheduler.startHour(this), Scheduler.startMinute(this), DateFormat.is24HourFormat(this)).show()
    }

    private fun toggle() {
        if (Scheduler.isEnabled(this)) {
            Scheduler.disable(this)
            stopService(Intent(this, AlarmService::class.java))
        } else {
            Scheduler.enable(this)
        }
        refresh()
    }

    private fun pause(minutes: Long) {
        if (!Scheduler.isEnabled(this)) {
            toast("Switch the alarms on first")
            return
        }
        if (minutes <= 0) {
            toast("Enter how many minutes")
            return
        }
        Scheduler.pauseFor(this, minutes)
        stopService(Intent(this, AlarmService::class.java))
        customMinutes.setText("")
        refresh()
    }

    private fun resumeNow() {
        Scheduler.resume(this)
        refresh()
    }

    // ---------- UI state ----------

    private fun refresh() {
        val tf = DateFormat.getTimeFormat(this)
        val on = Scheduler.isEnabled(this)

        val startCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, Scheduler.startHour(this@MainActivity))
            set(Calendar.MINUTE, Scheduler.startMinute(this@MainActivity))
        }
        startTimeBtn.text = "Start time: ${tf.format(startCal.time)}"
        toggleBtn.text = if (on) "Switch OFF" else "Switch ON"

        status.text = when {
            !on -> "Alarms are OFF"
            Scheduler.isPaused(this) -> "⏸ Paused until ${tf.format(Date(Scheduler.pausedUntil(this)))}"
            else -> "✅ ON - next alarm at ${tf.format(Date(Scheduler.nextTriggerMillis(this)))}"
        }

        fixes = pendingFixes()
        if (fixes.isEmpty()) {
            fixBtn.visibility = View.GONE
        } else {
            fixBtn.visibility = View.VISIBLE
            fixBtn.text = "⚠ ${fixes.first().first}"
        }
    }

    /** Things the phone still needs to allow so alarms work in the background. */
    private fun pendingFixes(): List<Pair<String, () -> Unit>> {
        val list = mutableListOf<Pair<String, () -> Unit>>()

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            list.add("Allow notifications" to {
                open(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
            })
        }

        val pm = getSystemService(PowerManager::class.java)
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            list.add("Allow background running (tap, then Allow)" to {
                open(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")))
            })
        }

        if (Build.VERSION.SDK_INT >= 31) {
            val am = getSystemService(AlarmManager::class.java)
            if (!am.canScheduleExactAlarms()) {
                list.add("Allow exact alarms" to {
                    open(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName")))
                })
            }
        }

        if (Build.VERSION.SDK_INT >= 34) {
            val nm = getSystemService(NotificationManager::class.java)
            if (!nm.canUseFullScreenIntent()) {
                list.add("Allow full-screen alarm popup" to {
                    open(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:$packageName")))
                })
            }
        }
        return list
    }

    // ---------- tiny helpers ----------

    private fun open(i: Intent) {
        try {
            startActivity(i)
        } catch (e: Exception) {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }
    }

    private fun toast(t: String) = Toast.makeText(this, t, Toast.LENGTH_SHORT).show()

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun label(t: String, size: Float, bold: Boolean = false) = TextView(this).apply {
        text = t
        textSize = size
        if (bold) setTypeface(typeface, Typeface.BOLD)
        setPadding(0, dp(14), 0, dp(4))
    }

    private fun button(t: String, onClick: () -> Unit) = Button(this).apply {
        text = t
        isAllCaps = false
        setOnClickListener { onClick() }
    }
}
