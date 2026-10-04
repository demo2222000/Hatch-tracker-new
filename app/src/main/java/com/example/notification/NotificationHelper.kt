package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.receiver.NotificationActionReceiver

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_TURNING_ID = "channel_egg_turning"
        const val CHANNEL_CLIMATE_ID = "channel_incubator_climate"
        const val CHANNEL_MILESTONES_ID = "channel_incubator_milestones"

        const val TURNING_NOTIFICATION_ID = 2001
        const val CLIMATE_NOTIFICATION_ID = 2002
        const val MILESTONE_NOTIFICATION_ID = 2003

        const val ACTION_MARK_TURNED = "com.example.action.MARK_TURNED"
        const val ACTION_SNOOZE = "com.example.action.SNOOZE"
        const val EXTRA_BATCH_ID = "extra_batch_id"
    }

    private val notificationManager = NotificationManagerCompat.from(context)

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val systemNotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val defaultAlarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            // 1. Egg Turning Reminders Channel
            val turningChannel = NotificationChannel(
                CHANNEL_TURNING_ID,
                "Egg Turning Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical alerts to rotate incubating eggs and prevent embryo adhesion"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400)
                setSound(defaultAlarmSound, audioAttributes)
            }

            // 2. Climate Alerts Channel
            val climateChannel = NotificationChannel(
                CHANNEL_CLIMATE_ID,
                "Incubator Climate Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent alerts when temperature or humidity breaches safe limits"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500, 250, 500)
                setSound(defaultAlarmSound, audioAttributes)
            }

            // 3. Milestones Channel
            val milestoneChannel = NotificationChannel(
                CHANNEL_MILESTONES_ID,
                "Incubation Milestones",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Lockdown start, pipping, and hatch day notifications"
            }

            systemNotificationManager.createNotificationChannels(
                listOf(turningChannel, climateChannel, milestoneChannel)
            )
        }
    }

    fun showEggTurningNotification(
        batchId: Long,
        batchName: String,
        targetOrientation: String = "Opposite Side"
    ) {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_BATCH_ID, batchId)
            putExtra("navigate_to", "turning")
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            batchId.toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Mark Turned Action
        val turnIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_MARK_TURNED
            putExtra(EXTRA_BATCH_ID, batchId)
        }
        val turnPendingIntent = PendingIntent.getBroadcast(
            context,
            (batchId * 10 + 1).toInt(),
            turnIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze 15 Min Action
        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_BATCH_ID, batchId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (batchId * 10 + 2).toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_TURNING_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Egg Turning Due: $batchName")
            .setContentText("Time to rotate eggs to $targetOrientation to prevent embryo adhesion.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(android.R.drawable.ic_menu_rotate, "Mark Turned", turnPendingIntent)
            .addAction(android.R.drawable.ic_popup_reminder, "Snooze 15m", snoozePendingIntent)
            .build()

        try {
            notificationManager.notify(TURNING_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Android 13+ permission not granted yet
        }
    }

    fun showClimateAlertNotification(
        notificationId: Int,
        title: String,
        message: String
    ) {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "sensor")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_CLIMATE_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            notificationManager.notify(notificationId, notification)
        } catch (_: SecurityException) {}
    }

    fun showMilestoneNotification(
        title: String,
        message: String
    ) {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "batches")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            MILESTONE_NOTIFICATION_ID,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_MILESTONES_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            notificationManager.notify(MILESTONE_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {}
    }

    fun cancelTurningNotification() {
        notificationManager.cancel(TURNING_NOTIFICATION_ID)
    }
}
