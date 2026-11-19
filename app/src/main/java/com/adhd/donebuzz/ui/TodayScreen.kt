package com.adhd.donebuzz.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.adhd.donebuzz.Notifications
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adhd.donebuzz.app
import com.adhd.donebuzz.data.AppSettings
import com.adhd.donebuzz.data.Days
import com.adhd.donebuzz.data.Todo
import com.adhd.donebuzz.feedback.Feedback
import com.adhd.donebuzz.focus.FocusTimer
import com.adhd.donebuzz.i18n.LocalStrings
import com.adhd.donebuzz.logic.CatProgress
import com.adhd.donebuzz.logic.Streak
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/** Keeps "today" right if the app stays open past midnight or comes back the next day. */
@Composable
fun rememberToday(): java.time.LocalDate {
    var today by remember { mutableStateOf(Days.today()) }
    LifecycleResumeEffect(Unit) {
        today = Days.today()
        onPauseOrDispose { }
    }
    LaunchedEffect(today) {
        val now = LocalDateTime.now()
        val midnight = now.toLocalDate().plusDays(1).atStartOfDay()
        delay(java.time.Duration.between(now, midnight).toMillis() + 500)
        today = Days.today()
    }
    return today
}

@Composable
fun TodayScreen(
    settings: AppSettings,
    feedback: Feedback,
    onSettings: () -> Unit,
    onEdit: (Todo) -> Unit,
    contentPadding: PaddingValues,
) {
    val s = LocalStrings.current
    val context = LocalContext.current
    val app = context.app
    val scope = rememberCoroutineScope()
    val today = rememberToday()
    val dayStart = Days.startMillis(today)
    val all by remember(dayStart) { app.database.todos().today(dayStart) }.collectAsStateWithLifecycle(null)
    val yearAgo = Days.startMillis(today.minusDays(400))
    val completions by remember(yearAgo) { app.database.todos().completionsSince(yearAgo) }.collectAsStateWithLifecycle(emptyList())

    // Open items, plus whatever was finished today, kept in place (gold) so the day's progress stays visible.
    val todos = all?.filter { it.completedAt == null || Days.of(it.completedAt) == today } ?: emptyList()
    val doneCount = todos.count { it.isDone }
    val streak = Streak.compute(completions.map(Days::of), today)
    var hops by remember { mutableIntStateOf(0) }
    var cat by remember { mutableStateOf(CatProgress.decode(app.localState.catProgress, today)) }

    // The focus countdown lives in a notification, so ask for permission the first time it is used.
    var pendingFocus by remember { mutableStateOf<Todo?>(null) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        pendingFocus?.let { todo -> scope.launch { FocusTimer.start(context, todo.id, todo.title, todo.durationMinutes ?: 25) } }
        pendingFocus = null
    }

    if (all != null) {
        LaunchedEffect(doneCount, todos.size, today) {
            val before = cat
            cat = cat.update(doneCount, todos.size, today)
            app.localState.catProgress = cat.encode()
            // Ticking off the last open item of the day gets the bigger celebration.
            if (before.day == today && doneCount > before.done && doneCount == todos.size && doneCount > 0) {
                feedback.dayCleared()
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        // The header is drawn above the list so the coin can fly over it.
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.zIndex(1f).padding(horizontal = 20.dp).padding(top = contentPadding.calculateTopPadding() + 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(s.todoTab, style = Type.screenTitle, modifier = Modifier.weight(1f))
                CoinCounter(doneCount)
                IconButton(
                    onClick = onSettings,
                    modifier = Modifier.size(48.dp).gameSurface(Color.White, CircleShape),
                ) { Icon(Icons.Rounded.Settings, s.settings, tint = Palette.ink) }
            }
            if (todos.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(s.progress(doneCount, todos.size - doneCount), style = Type.progress, modifier = Modifier.weight(1f))
                    if (streak.current > 0) StreakBadge(streak)
                }
            }
            Stage(doneCount, todos.size, cat.position.toFloat(), hops, Modifier.padding(top = 4.dp))
        }

        if (all != null && todos.isEmpty()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize().padding(32.dp).padding(bottom = contentPadding.calculateBottomPadding()),
            ) {
                Text("✨", style = Type.screenTitle)
                Text(s.emptyTitle, style = Type.sheetTitle, textAlign = TextAlign.Center)
                Text(s.emptyHint, style = Type.hint, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
            }
        } else {
            val listState = rememberLazyListState()
            var lastCount by remember { mutableIntStateOf(todos.size) }
            // A newly added item lands at the top; bring it into view. Only when the list grew, so deletes keep their own animation.
            LaunchedEffect(todos.size) {
                if (todos.size > lastCount) listState.animateScrollToItem(0)
                lastCount = todos.size
            }
            val focus = FocusState(settings.focusTodoId, settings.focusEndMillis)
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = contentPadding.calculateBottomPadding() + 16.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(todos, key = { it.id }) { todo ->
                    SwipeToDelete(onDelete = { app.repository.delete(todo) }, modifier = Modifier.animateItem()) {
                        TodoRow(
                            todo = todo,
                            focus = focus,
                            onEdit = { onEdit(todo) },
                            onToggle = {
                                if (!todo.isDone) {
                                    hops++
                                    feedback.taskCompleted()
                                } else {
                                    feedback.tick()
                                }
                                app.repository.setDone(todo, !todo.isDone)
                            },
                            onFocus = { start ->
                                if (start && !Notifications.allowed(context) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    pendingFocus = todo
                                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    scope.launch {
                                        if (start) FocusTimer.start(context, todo.id, todo.title, todo.durationMinutes ?: 25)
                                        else FocusTimer.stop(context)
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

/** Swipe left to delete, with a full swipe deleting in one motion. */
@Composable
fun SwipeToDelete(onDelete: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val s = LocalStrings.current
    val state = rememberSwipeToDismissBoxState()
    LaunchedEffect(state.currentValue) {
        if (state.currentValue == SwipeToDismissBoxValue.EndToStart) onDelete()
    }
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        modifier = modifier,
        backgroundContent = {
            Box(
                contentAlignment = Alignment.CenterEnd,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Palette.recording)
                    .padding(end = 24.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.Delete, null, tint = Color.White)
                    Text(s.delete, style = Type.label.copy(color = Color.White))
                }
            }
        },
    ) { content() }
}
