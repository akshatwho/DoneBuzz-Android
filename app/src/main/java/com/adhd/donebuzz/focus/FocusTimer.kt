package com.adhd.donebuzz.focus

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.adhd.donebuzz.Notifications
import com.adhd.donebuzz.R
import com.adhd.donebuzz.app
import com.adhd.donebuzz.i18n.Strings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * A countdown for one task. It shows as an ongoing notification that asks to be promoted to an
 * Android 16 Live Update, so the time left sits in a chip in the status bar and on the lock screen.
 * When the time is up an alarm posts a "Time's up" notification.
 */
object FocusTimer {
    private const val ACTION_STOP = "com.adhd.donebuzz.focus.STOP"
    private const val ACTION_DONE = "com.adhd.donebuzz.focus.DONE"

    suspend fun start(context: Context, todoId: Long, title: String, minutes: Int) {
        val end = System.currentTimeMillis() + minutes * 60_000L
        val store = context.app.settingsStore
        store.setFocus(todoId, title, end)
        val strings = store.current().strings
        showOngoing(context, title, end, strings)
        schedule(context, end)
    }

    suspend fun stop(context: Context) {
        context.app.settingsStore.setFocus(null, "", 0)
        NotificationManagerCompat.from(context).cancel(Notifications.ID_FOCUS)
        context.getSystemService(AlarmManager::class.java).cancel(alarmIntent(context))
    }

    suspend fun stopIfRunning(context: Context, todoId: Long) {
        if (context.app.settingsStore.current().focusTodoId == todoId) stop(context)
    }

    /** After a restart: show the countdown again, or tidy up one that ran out while the phone was off. */
    suspend fun restore(context: Context) {
        val settings = context.app.settingsStore.current()
        if (settings.focusTodoId == null) return
        if (settings.focusEndMillis <= System.currentTimeMillis()) {
            stop(context)
        } else {
            showOngoing(context, settings.focusTitle, settings.focusEndMillis, settings.strings)
            schedule(context, settings.focusEndMillis)
        }
    }

    private fun showOngoing(context: Context, title: String, end: Long, s: Strings) {
        if (!Notifications.allowed(context)) return
        val builder = NotificationCompat.Builder(context, Notifications.CHANNEL_FOCUS)
            .setSmallIcon(R.drawable.ic_stat_cat)
            .setContentTitle(title)
            .setContentText(s.focusing)
            .setStyle(NotificationCompat.BigTextStyle().bigText(s.focusing))
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setWhen(end)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setTimeoutAfter(end - System.currentTimeMillis() + 1_000)
            .setColor(0xFFF5B301.toInt())
            .setContentIntent(Notifications.openAppIntent(context))
            .addAction(0, s.focusStop, receiverIntent(context, ACTION_STOP, 1))
            .setRequestPromotedOngoing(true)
        @Suppress("MissingPermission")
        NotificationManagerCompat.from(context).notify(Notifications.ID_FOCUS, builder.build())
    }

    private fun schedule(context: Context, end: Long) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()
        if (exactAllowed) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, end, alarmIntent(context))
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, end, alarmIntent(context))
        }
    }

    private fun alarmIntent(context: Context) = receiverIntent(context, ACTION_DONE, 2)

    private fun receiverIntent(context: Context, action: String, code: Int) = PendingIntent.getBroadcast(
        context, code, Intent(context, FocusReceiver::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    class FocusReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val pending = goAsync()
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    val settings = context.app.settingsStore.current()
                    if (intent.action == ACTION_DONE && settings.focusTodoId != null && Notifications.allowed(context)) {
                        val s = settings.strings
                        val notification = NotificationCompat.Builder(context, Notifications.CHANNEL_FOCUS_DONE)
                            .setSmallIcon(R.drawable.ic_stat_cat)
                            .setContentTitle(s.timesUp)
                            .setContentText(s.focusEnded(settings.focusTitle))
                            .setContentIntent(Notifications.openAppIntent(context))
                            .setAutoCancel(true)
                            .build()
                        @Suppress("MissingPermission")
                        NotificationManagerCompat.from(context).notify(Notifications.ID_FOCUS_DONE, notification)
                    }
                    stop(context)
                } finally {
                    pending.finish()
                }
            }
        }
    }
}
