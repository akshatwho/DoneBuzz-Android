package com.adhd.donebuzz.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.adhd.donebuzz.data.Todo
import com.adhd.donebuzz.i18n.LocalStrings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.border

/** The running focus timer, if any: which task and when it ends. */
data class FocusState(val todoId: Long?, val endMillis: Long)

/**
 * One item drawn as a little grass-topped platform, with the check button on the right where the
 * thumb rests.
 */
@Composable
fun TodoRow(
    todo: Todo,
    focus: FocusState,
    onEdit: () -> Unit,
    onToggle: () -> Unit,
    onFocus: (start: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = LocalStrings.current
    val shape = RoundedCornerShape(18.dp)
    val done = todo.isDone

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxWidth()
            // The dirt underneath the platform, then the card with grass along its top edge.
            .drawBehind {
                val o = shape.createOutline(size, layoutDirection, this)
                translate(top = 6.dp.toPx()) { drawOutline(o, Palette.dirtDark) }
            }
            .clip(shape)
            .background(if (done) Palette.doneCard else Palette.card)
            .drawBehind { drawRect(Palette.grass.copy(alpha = if (done) 0.5f else 1f), size = Size(size.width, 9.dp.toPx())) }
            .border(3.dp, Palette.ink, shape)
            .clickable(enabled = !done, onClick = onEdit)
            .semantics { customActions = listOf(CustomAccessibilityAction(s.edit) { if (!done) onEdit(); true }) }
            .padding(start = 20.dp, end = 16.dp, top = 22.dp, bottom = 18.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // No strike-through: a crossed-out line is hard to re-read. The gold card and button say "done".
            Text(todo.title, style = Type.itemTitle.copy(color = if (done) Palette.muted else Palette.ink))
            todo.durationMinutes?.let { minutes ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    DurationBadge(minutes)
                    if (!done) FocusButton(todo, focus, onFocus)
                }
            }
        }
        CheckButton(todo, onToggle)
    }
}

@Composable
private fun FocusButton(todo: Todo, focus: FocusState, onFocus: (Boolean) -> Unit) {
    val s = LocalStrings.current
    val running = focus.todoId == todo.id && focus.endMillis > System.currentTimeMillis()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(running) {
        while (running) {
            now = System.currentTimeMillis()
            delay(1000 - now % 1000)
        }
    }
    val left = ((focus.endMillis - now) / 1000).coerceAtLeast(0)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier
            .heightIn(min = 34.dp)
            .clip(CircleShape)
            .background(if (running) Palette.goldLight else Color.White)
            .border(2.dp, Palette.ink, CircleShape)
            .clickable { onFocus(!running) }
            .semantics { contentDescription = if (running) s.stopFocus(todo.title) else s.startFocus(todo.title) }
            .padding(horizontal = 12.dp),
    ) {
        Icon(if (running) Icons.Rounded.Stop else Icons.Rounded.PlayArrow, null, tint = Palette.ink, modifier = Modifier.size(18.dp))
        Text(
            if (running) "%d:%02d".format(left / 60, left % 60) else s.focusStart,
            style = Type.label.copy(color = Palette.ink, fontFeatureSettings = "tnum"),
        )
    }
}

@Composable
private fun CheckButton(todo: Todo, onToggle: () -> Unit) {
    val s = LocalStrings.current
    val done = todo.isDone
    val scope = rememberCoroutineScope()
    val squish = remember { Animatable(1f) }
    val coin = remember { Animatable(1f) }
    var pops by remember { mutableIntStateOf(0) }

    Box(contentAlignment = Alignment.TopCenter) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(54.dp)
            .graphicsLayer { scaleX = squish.value; scaleY = squish.value }
            .drawBehind { translate(top = 4.dp.toPx()) { drawCircle(Palette.ink) } }
            .clip(CircleShape)
            .background(if (done) Palette.gold else Color.White)
            .border(3.dp, Palette.ink, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                scope.launch {
                    squish.animateTo(1f, keyframes {
                        durationMillis = 420
                        0.8f at 100
                        1.14f at 220
                    })
                }
                if (!done) {
                    pops++
                    scope.launch {
                        coin.snapTo(0f)
                        coin.animateTo(1f, tween(600, easing = LinearEasing))
                    }
                }
                onToggle()
            }
            .semantics { contentDescription = "${if (done) s.markNotDone else s.markDone}: ${todo.title}" },
    ) {
        AnimatedVisibility(done, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
            Icon(Icons.Rounded.Check, null, tint = Palette.ink, modifier = Modifier.size(32.dp))
        }
    }
    // A small coin pops out of the button when the item is completed.
    if (pops > 0 && coin.value < 1f) {
        PixelSprite(
            Sprite.coin,
            Modifier
                .size(22.dp)
                .graphicsLayer {
                    val t = coin.value * 600
                    translationY = track(t, 0 to -4f, 600 to -30f).dp.toPx()
                    rotationY = t / 600 * 720
                    alpha = track(t, 0 to 1f, 330 to 1f, 600 to 0f)
                },
        )
    }
    }
}
