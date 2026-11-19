package com.adhd.donebuzz

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.adhd.donebuzz.i18n.AppLanguage
import com.adhd.donebuzz.i18n.Strings
import kotlinx.coroutines.runBlocking

object Notifications {
    const val CHANNEL_REMINDERS = "reminders"
    const val CHANNEL_FOCUS = "focus"
    const val CHANNEL_FOCUS_DONE = "focus_done"

    const val ID_REMINDER = 1001
    const val ID_FOCUS = 2001
    const val ID_FOCUS_DONE = 2002

    /** Channel names follow the app's language; re-creating a channel only renames it. */
    fun createChannels(context: Context) {
        val strings = runCatching { runBlocking { context.app.settingsStore.current().strings } }
            .getOrElse { Strings(AppLanguage.SYSTEM) }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(CHANNEL_REMINDERS, strings.reminderChannel, NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel(CHANNEL_FOCUS, strings.focusChannel, NotificationManager.IMPORTANCE_DEFAULT).apply {
                    setSound(null, null)
                    enableVibration(false)
                },
                NotificationChannel(CHANNEL_FOCUS_DONE, strings.timesUp, NotificationManager.IMPORTANCE_HIGH),
            ),
        )
    }

    fun allowed(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** Opens the app on the To-do screen. */
    fun openAppIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context, 0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}
