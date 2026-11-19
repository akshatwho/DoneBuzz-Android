package com.adhd.donebuzz.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.adhd.donebuzz.Notifications
import com.adhd.donebuzz.app
import com.adhd.donebuzz.data.AppSettings
import com.adhd.donebuzz.i18n.AppLanguage
import com.adhd.donebuzz.i18n.LocalStrings
import com.adhd.donebuzz.reminders.Reminders
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().gameSurface(Color.White, shape).padding(16.dp),
    ) { content() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(settings: AppSettings, onDismiss: () -> Unit, onWhatsNew: () -> Unit) {
    val s = LocalStrings.current
    val context = LocalContext.current
    val app = context.app
    val scope = rememberCoroutineScope()
    var allowed by remember { mutableStateOf(Notifications.allowed(context)) }
    var pickingTime by remember { mutableStateOf(false) }
    // Coming back from system settings may have changed the permission.
    LifecycleResumeEffect(Unit) {
        allowed = Notifications.allowed(context)
        onPauseOrDispose { }
    }

    fun setReminder(enabled: Boolean, minutes: Int) = scope.launch {
        app.settingsStore.setReminder(enabled, minutes)
        Reminders.sync(context, app.settingsStore.current())
    }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        allowed = granted
        setReminder(granted, settings.reminderMinutes)
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = Palette.skyLight) {
        Column(
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 24.dp),
        ) {
            Text(s.settings, style = Type.sheetTitle)

            // Language
            SectionCard {
                Text(s.languageTitle, style = Type.label.copy(color = Palette.ink))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppLanguage.entries.forEach { option ->
                        val selected = settings.language == option
                        Text(
                            s.name(option),
                            style = Type.label.copy(color = Palette.ink, textAlign = TextAlign.Center),
                            maxLines = 1,
                            modifier = Modifier
                                .weight(1f)
                                .clip(CircleShape)
                                .background(if (selected) Palette.gold else Palette.card)
                                .border(if (selected) 3.dp else 2.dp, Palette.ink, CircleShape)
                                .clickable(role = Role.RadioButton) { scope.launch { app.settingsStore.setLanguage(option) } }
                                .padding(vertical = 10.dp),
                        )
                    }
                }
                Text(s.languageFootnote, style = Type.hint.copy(color = Palette.muted))
            }

            // Daily reminder
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(s.reminderTitle, style = Type.label.copy(color = Palette.ink), modifier = Modifier.weight(1f))
                    Switch(
                        checked = settings.reminderEnabled && allowed,
                        onCheckedChange = { on ->
                            when {
                                !on -> setReminder(false, settings.reminderMinutes)
                                allowed -> setReminder(true, settings.reminderMinutes)
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = Palette.grass, checkedThumbColor = Color.White),
                    )
                }
                if (settings.reminderEnabled && allowed) {
                    val time = LocalTime.of(settings.reminderMinutes / 60, settings.reminderMinutes % 60)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable(role = Role.Button) { pickingTime = true },
                    ) {
                        Text(s.reminderTime, style = Type.hint, modifier = Modifier.weight(1f))
                        Text(
                            time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(s.locale)),
                            style = Type.label.copy(color = Palette.ink),
                            modifier = Modifier.background(Palette.tagBackground, CircleShape).border(2.dp, Palette.ink, CircleShape).padding(horizontal = 14.dp, vertical = 6.dp),
                        )
                    }
                }
                if (!allowed) {
                    Text(s.reminderDenied, style = Type.hint.copy(color = Palette.recording))
                    Text(
                        s.openSystemSettings,
                        style = Type.label.copy(color = Color(0xFF1F5FCC)),
                        modifier = Modifier.clickable(role = Role.Button) {
                            context.startActivity(
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                            )
                        },
                    )
                } else {
                    Text(s.reminderFootnote, style = Type.hint.copy(color = Palette.muted))
                }
            }

            // What's new
            SectionCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onWhatsNew),
                ) {
                    Text("✨  " + s.whatsNewSettingsRow, style = Type.label.copy(color = Palette.ink), modifier = Modifier.weight(1f))
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Palette.ink)
                }
            }
        }
    }

    if (pickingTime) {
        val picker = rememberTimePickerState(settings.reminderMinutes / 60, settings.reminderMinutes % 60)
        AlertDialog(
            onDismissRequest = { pickingTime = false },
            confirmButton = {
                TextButton(onClick = {
                    pickingTime = false
                    setReminder(true, picker.hour * 60 + picker.minute)
                }) { Text(s.save) }
            },
            text = { TimePicker(picker) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsNewSheet(onDismiss: () -> Unit) {
    val s = LocalStrings.current
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = Palette.card) {
        Column(
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 24.dp).padding(bottom = 24.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(s.whatsNewTitle, style = Type.sheetTitle)
                Text(s.whatsNewSubtitle, style = Type.hint.copy(color = Palette.muted))
            }
            s.whatsNewItems.forEach { item ->
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
                    Text(
                        item.icon,
                        style = Type.sheetTitle,
                        modifier = Modifier.gameSurface(Palette.goldLight, RoundedCornerShape(14.dp)).padding(8.dp),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(item.title, style = Type.label.copy(color = Palette.ink))
                        Text(item.body, style = Type.hint.copy(color = Palette.muted))
                    }
                }
            }
            val shape = RoundedCornerShape(16.dp)
            Text(
                s.gotIt,
                style = Type.button.copy(textAlign = TextAlign.Center),
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp).gameSurface(Palette.gold, shape).clip(shape)
                    .clickable(role = Role.Button, onClick = onDismiss).padding(vertical = 15.dp),
            )
        }
    }
}
