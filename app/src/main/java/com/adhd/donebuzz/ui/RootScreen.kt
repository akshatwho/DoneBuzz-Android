package com.adhd.donebuzz.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adhd.donebuzz.app
import com.adhd.donebuzz.data.Todo
import com.adhd.donebuzz.feedback.Feedback
import com.adhd.donebuzz.i18n.LocalStrings
import com.adhd.donebuzz.logic.WhatsNew
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Two pages (To do, Done) you can swipe between, a floating tab bar whose highlight follows your finger,
 * and the + on the right where the thumb rests.
 */
@Composable
fun RootScreen(addRequests: Int) {
    val context = LocalContext.current
    val app = context.app
    val settings by app.settingsStore.settings.collectAsStateWithLifecycle(null)
    val current = settings ?: run {
        SkyBackground()
        return
    }
    val strings = remember(current.language) { current.strings }
    val feedback = remember { Feedback(context) }
    val scope = rememberCoroutineScope()
    val pager = rememberPagerState { 2 }

    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Todo?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var showWhatsNew by remember { mutableStateOf(false) }
    var confetti by remember { mutableIntStateOf(0) }

    // The widget's + opens the add sheet.
    LaunchedEffect(addRequests) {
        if (addRequests > 0) {
            showSettings = false
            showWhatsNew = false
            pager.animateScrollToPage(0)
            adding = true
        }
    }
    // What's New: shown once after an update, never on a fresh install.
    LaunchedEffect(Unit) {
        val hasData = app.database.todos().count() > 0
        when (WhatsNew.launchAction(current.whatsNewSeenRelease, hasData)) {
            WhatsNew.Action.SHOW -> {
                delay(600)
                showWhatsNew = true
            }
            WhatsNew.Action.RECORD_SILENTLY -> app.settingsStore.setWhatsNewSeen(WhatsNew.CURRENT_RELEASE)
            WhatsNew.Action.NOTHING -> Unit
        }
    }

    CompositionLocalProvider(LocalStrings provides strings) {
        val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val contentPadding = PaddingValues(top = top, bottom = bottom + 90.dp)

        Box(Modifier.fillMaxSize()) {
            SkyBackground()
            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize(), beyondViewportPageCount = 1) { page ->
                when (page) {
                    0 -> TodayScreen(
                        settings = current,
                        feedback = feedback,
                        onSettings = { showSettings = true },
                        onEdit = { editing = it },
                        onCelebrate = { confetti++ },
                        contentPadding = contentPadding,
                    )
                    else -> HistoryScreen(contentPadding)
                }
            }
            BottomBar(
                pager = pager,
                onAdd = { adding = true },
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 10.dp),
            )
            Confetti(confetti)
        }

        if (adding) {
            EditorSheet(todo = null, onDismiss = { adding = false }) { title, minutes ->
                app.repository.add(title, minutes)
                scope.launch { pager.animateScrollToPage(0) }
            }
        }
        editing?.let { todo ->
            EditorSheet(todo = todo, onDismiss = { editing = null }) { title, minutes ->
                app.repository.update(todo.copy(title = title, durationMinutes = minutes))
            }
        }
        if (showSettings) {
            SettingsSheet(current, onDismiss = { showSettings = false }, onWhatsNew = {
                showSettings = false
                showWhatsNew = true
            })
        }
        if (showWhatsNew) {
            WhatsNewSheet(onDismiss = {
                showWhatsNew = false
                scope.launch { app.settingsStore.setWhatsNewSeen(WhatsNew.CURRENT_RELEASE) }
            })
        }
    }
}

@Composable
private fun BottomBar(pager: PagerState, onAdd: () -> Unit, modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var lastPage by remember { mutableIntStateOf(pager.currentPage) }
    LaunchedEffect(pager.currentPage) {
        if (pager.currentPage != lastPage) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
        lastPage = pager.currentPage
    }

    Row(modifier.fillMaxWidth().height(64.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        BoxWithConstraints(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.72f))
                .border(2.dp, Palette.ink.copy(alpha = 0.15f), CircleShape),
        ) {
            val tabWidth: Dp = (maxWidth - 12.dp) / 2
            val pageFraction = (pager.currentPage + pager.currentPageOffsetFraction).coerceIn(0f, 1f)
            // Dragging along the bar slides the highlight and the pages with it.
            val drag = rememberDraggableState { delta ->
                val pageWidth = pager.layoutInfo.viewportSize.width.toFloat()
                val tabPx = constraints.maxWidth / 2f
                pager.dispatchRawDelta(-(-delta) * pageWidth / tabPx)
            }
            Box(
                Modifier
                    .padding(6.dp)
                    .offset(x = tabWidth * pageFraction)
                    .width(tabWidth)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(Palette.goldLight),
            )
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(6.dp)
                    .draggable(drag, Orientation.Horizontal, onDragStopped = { velocity ->
                        val target = if (velocity > 300) 1 else if (velocity < -300) 0 else pageFraction.roundToInt()
                        scope.launch { pager.animateScrollToPage(target) }
                    }),
            ) {
                listOf(Icons.Rounded.Checklist to s.todoTab, Icons.Rounded.CheckCircle to s.doneTab).forEachIndexed { index, (icon, label) ->
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Tab) {
                                scope.launch { pager.animateScrollToPage(index) }
                            }
                            .semantics { selected = pager.currentPage == index },
                    ) {
                        Icon(icon, null, tint = Palette.ink, modifier = Modifier.size(24.dp))
                        Text("  $label", style = Type.label.copy(color = Palette.ink))
                    }
                }
            }
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(64.dp)
                .gameSurface(Palette.gold, CircleShape)
                .clip(CircleShape)
                .clickable(role = Role.Button) {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    onAdd()
                }
                .semantics { contentDescription = s.addItem },
        ) { Icon(Icons.Rounded.Add, null, tint = Palette.ink, modifier = Modifier.size(34.dp)) }
    }
}
