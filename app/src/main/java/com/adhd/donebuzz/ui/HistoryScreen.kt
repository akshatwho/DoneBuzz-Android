package com.adhd.donebuzz.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adhd.donebuzz.app
import com.adhd.donebuzz.data.Days
import com.adhd.donebuzz.i18n.LocalStrings
import com.adhd.donebuzz.logic.Streak
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Everything finished, grouped by day: 今天 / 昨天 / 前天 with the date and the day's total time. */
@Composable
fun HistoryScreen(contentPadding: PaddingValues) {
    val s = LocalStrings.current
    val app = LocalContext.current.app
    val today = rememberToday()
    val finished by remember { app.database.todos().finished() }.collectAsStateWithLifecycle(null)
    val days = finished.orEmpty().groupBy { Days.of(it.completedAt!!) }
    val streak = Streak.compute(days.keys, today)
    val timeFormat = remember(s.locale) { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(s.locale) }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 20.dp, end = 20.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item { Text(s.doneTab, style = Type.screenTitle) }
        item { StreakBanner(streak) }

        if (finished != null && days.isEmpty()) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 60.dp)) {
                    Text("🏁", style = Type.screenTitle)
                    Text(s.noneFinishedTitle, style = Type.sheetTitle, textAlign = TextAlign.Center)
                    Text(s.noneFinishedHint, style = Type.hint, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }

        days.forEach { (day, todos) ->
            item(key = "day-$day") {
                Column(Modifier.padding(top = 10.dp).animateItem()) {
                    Text(s.dayTitle(day, today), style = Type.historyTitle)
                    Text(s.daySummary(todos.size, todos.sumOf { it.durationMinutes ?: 0 }), style = Type.hint.copy(color = Palette.muted))
                }
            }
            items(todos, key = { it.id }) { todo ->
                SwipeToDelete(onDelete = { app.repository.delete(todo) }, modifier = Modifier.animateItem()) {
                    val shape = RoundedCornerShape(16.dp)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shape)
                            .background(Palette.doneCard)
                            .border(2.dp, Palette.ink, shape)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(todo.title, style = Type.label.copy(color = Palette.ink))
                            val time = Instant.ofEpochMilli(todo.completedAt!!).atZone(ZoneId.systemDefault()).format(timeFormat)
                            Text(listOfNotNull(time, todo.durationMinutes?.let(s::duration)).joinToString(" · "), style = Type.hint.copy(color = Palette.muted))
                        }
                        // Tap the gold tick to put the item back on the To-do list.
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Palette.gold)
                                .border(2.dp, Palette.ink, CircleShape)
                                .clickable(role = Role.Button) { app.repository.setDone(todo, false) }
                                .semantics { contentDescription = "${s.markNotDone}: ${todo.title}" },
                        ) { Icon(Icons.Rounded.Check, null, tint = Palette.ink) }
                    }
                }
            }
        }
    }
}
