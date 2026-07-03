package com.v7lthronyx.v7lpanel.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.v7lthronyx.v7lpanel.MainActivity

object NotificationHelper {
    private const val CHANNEL_TRAFFIC   = "v7l_traffic"
    private const val CHANNEL_FOREGROUND = "v7l_foreground"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_FOREGROUND, "Live Polling",
                    NotificationManager.IMPORTANCE_LOW).apply { description = "Background live traffic polling" }
            )
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_TRAFFIC, "Traffic Alerts",
                    NotificationManager.IMPORTANCE_DEFAULT).apply { description = "Traffic and expiry alerts" }
            )
        }
    }

    fun buildForegroundNotification(context: Context) =
        NotificationCompat.Builder(context, CHANNEL_FOREGROUND)
            .setContentTitle("Spiritus — Live")
            .setContentText("Monitoring active connections")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(mainPendingIntent(context))
            .build()

    fun notifyLowTraffic(context: Context, userName: String, percentLeft: Int) {
        val nm  = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val ntf = NotificationCompat.Builder(context, CHANNEL_TRAFFIC)
            .setContentTitle("Low Traffic Warning")
            .setContentText("$userName: only ${percentLeft}% remaining")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setAutoCancel(true)
            .setContentIntent(mainPendingIntent(context))
            .build()
        nm.notify(userName.hashCode(), ntf)
    }

    fun notifyExpiringSoon(context: Context, userName: String, days: Long) {
        val nm  = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val ntf = NotificationCompat.Builder(context, CHANNEL_TRAFFIC)
            .setContentTitle("Account Expiring")
            .setContentText("$userName expires in $days day${if (days == 1L) "" else "s"}")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .setContentIntent(mainPendingIntent(context))
            .build()
        nm.notify(userName.hashCode() + 10_000, ntf)
    }

    private fun mainPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}
