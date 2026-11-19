package com.adhd.donebuzz.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.adhd.donebuzz.data.Todo
import com.adhd.donebuzz.i18n.LocalStrings
import com.adhd.donebuzz.i18n.SpeechProblem
import com.adhd.donebuzz.logic.DurationOption
import com.adhd.donebuzz.speech.SpeechInput
import kotlinx.coroutines.delay

private const val MAX_TITLE = 200

/** Used both to add a new item and to edit an existing one. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorSheet(todo: Todo?, onDismiss: () -> Unit, onSave: (title: String, minutes: Int?) -> Unit) {
    val s = LocalStrings.current
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by rememberSaveable { mutableStateOf(todo?.title ?: "") }
    var minutes by rememberSaveable { mutableStateOf(todo?.durationMinutes) }
    val trimmed = title.trim()
    val focus = remember { FocusRequester() }

    val speech = remember { SpeechInput(context) }
    var beforeDictation by remember { mutableStateOf("") }
    DisposableEffect(Unit) { onDispose { speech.release() } }
    LaunchedEffect(speech.isRecording) {
        if (speech.isRecording) {
            val needsSpace = !s.isChinese && title.isNotEmpty() && !title.last().isWhitespace()
            beforeDictation = title + if (needsSpace) " " else ""
        }
    }
    LaunchedEffect(speech.transcript) {
        if (speech.transcript.isNotEmpty()) title = (beforeDictation + speech.transcript).take(MAX_TITLE)
    }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) speech.toggle(s.speechTag) else speech.problem = SpeechProblem.PERMISSION_DENIED
    }

    fun save() {
        if (trimmed.isEmpty()) return
        speech.stop()
        onSave(trimmed.take(MAX_TITLE), minutes)
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Palette.card,
        dragHandle = { Box(Modifier.padding(top = 10.dp).size(40.dp, 5.dp).clip(CircleShape).background(Palette.muted.copy(alpha = 0.4f))) },
    ) {
        LaunchedEffect(Unit) {
            // Focusing while the sheet slides in can be dropped, so wait for it to settle.
            delay(300)
            runCatching { focus.requestFocus() }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(start = 22.dp, end = 22.dp, top = 12.dp, bottom = 22.dp),
        ) {
            Text(if (todo != null) s.editItem else s.newItem, style = Type.sheetTitle)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                    val fieldShape = RoundedCornerShape(14.dp)
                    BasicTextField(
                        value = title,
                        // Return adds the item instead of starting a new line; pasted line breaks become spaces.
                        onValueChange = { value ->
                            if (value.endsWith("\n") && value.dropLast(1) == title) save()
                            else title = value.replace('\n', ' ').take(MAX_TITLE)
                        },
                        textStyle = Type.itemTitle.copy(color = Palette.ink),
                        cursorBrush = SolidColor(Palette.ink),
                        maxLines = 3,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { save() }),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focus)
                            .background(Palette.skyLight.copy(alpha = 0.6f), fieldShape)
                            .border(2.dp, Palette.ink, fieldShape)
                            .padding(14.dp),
                        decorationBox = { inner ->
                            if (title.isEmpty()) Text(s.titlePlaceholder, style = Type.itemTitle.copy(color = Palette.muted.copy(alpha = 0.5f)), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            inner()
                        },
                    )
                    // The mic.
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(if (speech.isRecording) Palette.recording else Color.White)
                            .border(2.dp, Palette.ink, CircleShape)
                            .clickable(role = Role.Button) {
                                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                if (granted || speech.isRecording) speech.toggle(s.speechTag) else micPermission.launch(Manifest.permission.RECORD_AUDIO)
                            }
                            .semantics { contentDescription = if (speech.isRecording) s.stopVoiceInput else s.voiceInput },
                    ) {
                        Icon(
                            if (speech.isRecording) Icons.Rounded.Stop else Icons.Rounded.Mic, null,
                            tint = if (speech.isRecording) Color.White else Palette.ink,
                        )
                    }
                }
                when {
                    speech.isRecording -> Text(s.listening, style = Type.hint)
                    speech.problem != null -> Text(s.message(speech.problem!!), style = Type.hint.copy(color = Palette.recording))
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(s.howLong, style = Type.label.copy(color = Palette.muted))
                DurationOption.all.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { option ->
                            val selected = minutes == option
                            Text(
                                s.chipDuration(option),
                                style = Type.label.copy(color = if (selected) Palette.ink else Palette.tagText, textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                                maxLines = 1,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(CircleShape)
                                    .background(if (selected) Palette.gold else Palette.tagBackground)
                                    .border(if (selected) 3.dp else 2.dp, Palette.ink, CircleShape)
                                    .clickable(role = Role.RadioButton) {
                                        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                        // Tapping the selected chip again clears it.
                                        minutes = if (selected) null else option
                                    }
                                    .semantics { this.selected = selected }
                                    .padding(vertical = 12.dp),
                            )
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }

            val buttonShape = RoundedCornerShape(16.dp)
            Text(
                if (todo != null) s.save else s.add,
                style = Type.button.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (trimmed.isEmpty()) 0.45f else 1f)
                    .gameSurface(Palette.gold, buttonShape)
                    .clip(buttonShape)
                    .clickable(enabled = trimmed.isNotEmpty(), role = Role.Button) { save() }
                    .padding(vertical = 15.dp),
            )
        }
    }
}
