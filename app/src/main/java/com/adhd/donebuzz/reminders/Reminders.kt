package com.adhd.donebuzz.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.adhd.donebuzz.Notifications
import com.adhd.donebuzz.R
import com.adhd.donebuzz.app
import com.adhd.donebuzz.data.AppSettings
import com.adhd.donebuzz.focus.FocusTimer
import com.adhd.donebuzz.logic.ReminderTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId

/** The once-a-day nudge. One alarm is kept for the next occurrence and re-armed each time it fires. */
object Reminders {
    private fun pendingIntent(context: Context) = PendingIntent.getBroadcast(
        context, 0, Intent(context, ReminderReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /** Arms or cancels the alarm to match [settings]. */
    fun sync(context: Context, settings: AppSettings) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        alarms.cancel(pendingIntent(context))
        if (!settings.reminderEnabled) return
        val next = ReminderTime.next(settings.reminderMinutes, LocalDateTime.now())
        val millis = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        // A gentle nudge does not need to be to-the-second, so an inexact alarm is enough (no extra permission).
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pendingIntent(context))
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val settings = context.app.settingsStore.current()
                if (settings.reminderEnabled && Notifications.allowed(context)) {
                    val s = settings.strings
                    val weekday = ReminderTime.weekdayIndex(LocalDateTime.now().dayOfWeek)
                    val notification = NotificationCompat.Builder(context, Notifications.CHANNEL_REMINDERS)
                        .setSmallIcon(R.drawable.ic_stat_cat)
                        .setContentTitle("DoneBuzz")
                        .setContentText(s.reminderMessage(weekday))
                        .setContentIntent(Notifications.openAppIntent(context))
                        .setAutoCancel(true)
                        .build()
                    @Suppress("MissingPermission")
                    NotificationManagerCompat.from(context).notify(Notifications.ID_REMINDER, notification)
                }
                Reminders.sync(context, settings)
            } finally {
                pending.finish()
            }
        }
    }
}

/** Alarms do not survive a restart or a clock change; put them back. */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                Reminders.sync(context, context.app.settingsStore.current())
                FocusTimer.restore(context)
            } finally {
                pending.finish()
            }
        }
    }
}
