package com.waterbuddy.app

import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/** The screen that appears when the alarm rings. Turning it off shows the message. */
class AlarmActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val pad = (24 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(pad, pad, pad, pad)
            setBackgroundColor(0xFF0D47A1.toInt())
        }
        root.addView(TextView(this).apply {
            text = "💧"
            textSize = 72f
            gravity = Gravity.CENTER
        })
        root.addView(TextView(this).apply {
            text = "Time to drink water!"
            textSize = 28f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, pad, 0, pad * 2)
        })
        root.addView(Button(this).apply {
            text = "Turn off alarm"
            textSize = 20f
            isAllCaps = false
            setOnClickListener { turnOff() }
        })
        setContentView(root)
    }

    private fun turnOff() {
        stopService(Intent(this, AlarmService::class.java))
        getSystemService(NotificationManager::class.java).apply {
            cancel(Notifications.ID_MISSED)
            cancel(Notifications.ID_ALERT)
        }

        AlertDialog.Builder(this)
            .setTitle("💧")
            .setMessage(Config.MESSAGE)
            .setCancelable(false)
            .setPositiveButton("ok 🥤") { _, _ -> finish() }
            .show()
    }
}
