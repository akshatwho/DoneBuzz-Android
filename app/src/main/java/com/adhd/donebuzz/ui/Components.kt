package com.adhd.donebuzz.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.adhd.donebuzz.i18n.LocalStrings
import com.adhd.donebuzz.logic.StreakInfo
import kotlin.random.Random

/** The sky, rolling hills and clouds behind every screen. */
@Composable
fun SkyBackground() {
    BoxWithConstraints(Modifier.fillMaxSize().background(Palette.sky)) {
        PixelSprite(Sprite.cloud, Modifier.size(60.dp, 24.dp).offset(x = maxWidth * 0.58f - 30.dp, y = 62.dp))
        PixelSprite(Sprite.cloud, Modifier.size(44.dp, 18.dp).offset(x = 24.dp, y = maxHeight * 0.78f))
        Canvas(Modifier.fillMaxSize()) {
            val outline = androidx.compose.ui.graphics.drawscope.Stroke(3.dp.toPx())
            val left = Offset(90.dp.toPx() - 150.dp.toPx(), size.height + 20.dp.toPx() - 130.dp.toPx())
            val leftSize = Size(300.dp.toPx(), 260.dp.toPx())
            drawOval(Palette.hill, left, leftSize)
            drawOval(Palette.ink, left, leftSize, style = outline)
            val right = Offset(size.width - 80.dp.toPx() - 170.dp.toPx(), size.height + 30.dp.toPx() - 140.dp.toPx())
            val rightSize = Size(340.dp.toPx(), 280.dp.toPx())
            drawOval(Palette.grassDark, right, rightSize)
            drawOval(Palette.ink, right, rightSize, style = outline)
        }
    }
}

/** Today's coins. Bounces when the count changes, timed to land as the stage's coin peaks. */
@Composable
fun CoinCounter(count: Int) {
    val s = LocalStrings.current
    val scale = remember { Animatable(1f) }
    LaunchedEffect(count) {
        if (count > 0) {
            scale.animateTo(1f, keyframes {
                durationMillis = 1000
                1f at 550
                1.25f at 700
                1f at 1000
            })
        }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .gameSurface(Color.White, CircleShape)
            .padding(start = 8.dp, end = 14.dp, top = 6.dp, bottom = 6.dp)
            .clearAndSetSemantics { contentDescription = s.coins(count) },
    ) {
        PixelSprite(Sprite.coin, Modifier.size(24.dp))
        Text("×$count", style = Type.pixelNumber)
    }
}

@Composable
fun DurationBadge(minutes: Int) {
    val s = LocalStrings.current
    Text(
        "⏱ " + s.duration(minutes),
        style = Type.label.copy(color = Palette.tagText),
        modifier = Modifier
            .background(Palette.tagBackground, CircleShape)
            .border(2.dp, Palette.ink, CircleShape)
            .padding(horizontal = 12.dp, vertical = 5.dp),
    )
}

/** 🔥 and the current streak, next to today's progress. */
@Composable
fun StreakBadge(info: StreakInfo) {
    val s = LocalStrings.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .gameSurface(if (info.doneToday) Palette.goldLight else Color.White, CircleShape)
            .padding(horizontal = 12.dp, vertical = 5.dp)
            .clearAndSetSemantics { contentDescription = s.streakDays(info.current) },
    ) {
        Text("🔥", style = Type.label)
        Text("${info.current}", style = Type.pixelNumber)
    }
}

/** The streak and best run, at the top of the Done screen. */
@Composable
fun StreakBanner(info: StreakInfo) {
    val s = LocalStrings.current
    val shape = RoundedCornerShape(18.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .gameSurface(Palette.card, shape)
            .padding(16.dp),
    ) {
        Text("🔥", style = Type.screenTitle)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (info.current > 0) {
                Text(s.streakDays(info.current), style = Type.progress)
                Text("${s.bestStreak(info.best)} · ${s.streakRestHint}", style = Type.hint.copy(color = Palette.muted))
            } else {
                Text(s.streakStart, style = Type.hint)
                if (info.best > 0) Text(s.bestStreak(info.best), style = Type.hint.copy(color = Palette.muted))
            }
        }
    }
}

/** Confetti raining from the top of the screen, played once per [bursts] bump. */
@Composable
fun Confetti(bursts: Int, modifier: Modifier = Modifier) {
    val reduceMotion = rememberReduceMotion()
    val progress = remember { Animatable(1f) }
    val pieces = remember(bursts) {
        val random = Random(bursts)
        val colors = listOf(Palette.gold, Palette.grass, Color(0xFFE5577A), Color(0xFF3D7BFF), Palette.sky, Color(0xFFF08A24))
        List(70) {
            Piece(
                x = random.nextFloat(),
                drift = random.nextFloat() * 0.3f - 0.15f,
                speed = 0.7f + random.nextFloat() * 0.6f,
                delay = random.nextFloat() * 0.25f,
                spin = random.nextFloat() * 720f - 360f,
                color = colors[random.nextInt(colors.size)],
            )
        }
    }
    LaunchedEffect(bursts) {
        if (bursts > 0 && !reduceMotion) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(1900, easing = LinearEasing))
        }
    }
    if (progress.value >= 1f) return
    Canvas(modifier.fillMaxSize()) {
        val p = progress.value
        for (piece in pieces) {
            val local = ((p - piece.delay) / (1 - piece.delay)).coerceIn(0f, 1f)
            if (local <= 0f) continue
            val y = -20f + local * piece.speed * size.height * 1.1f
            val x = (piece.x + piece.drift * local) * size.width
            val alpha = if (local > 0.8f) (1 - local) / 0.2f else 1f
            rotate(piece.spin * local, Offset(x, y)) {
                drawRect(piece.color.copy(alpha = alpha), Offset(x - 5f, y - 8f), Size(10.dp.toPx() / 1.6f, 16.dp.toPx() / 1.6f))
            }
        }
    }
}

private data class Piece(val x: Float, val drift: Float, val speed: Float, val delay: Float, val spin: Float, val color: Color)
