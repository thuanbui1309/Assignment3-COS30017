package com.example.assignment3_cos30017.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.assignment3_cos30017.R

object NotificationChannelHelper {

    const val DEFAULT_CHANNEL_ID = "car_rental_general"

    fun ensureDefaultChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val name = context.getString(R.string.notification_channel_general_name)
        val channel = NotificationChannel(
            DEFAULT_CHANNEL_ID,
            name,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = context.getString(R.string.notification_channel_general_desc) }
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }
}
