package com.adhd.donebuzz.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.adhd.donebuzz.i18n.LocalStrings

private const val HOP_MS = 1220f

/** Piecewise keyframes with a smooth step between each pair, sampled at [t] milliseconds. */
internal fun track(t: Float, vararg points: Pair<Int, Float>): Float {
    if (t <= points.first().first) return points.first().second
    for (i in 1 until points.size) {
        val (t0, v0) = points[i - 1]
        val (t1, v1) = points[i]
        if (t <= t1) {
            val f = if (t1 == t0) 1f else (t - t0) / (t1 - t0)
            return v0 + (v1 - v0) * (f * f * (3 - 2 * f))
        }
    }
    return points.last().second
}

/**
 * The little side-scrolling scene at the top of the To-do screen. The cat walks toward the finish flag
 * as the day's items get done, and hops to knock out a coin each time one is ticked off.
 */
@Composable
fun Stage(done: Int, total: Int, position: Float, hops: Int, modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    val reduceMotion = rememberReduceMotion()
    val shape = RoundedCornerShape(22.dp)
    val ground = 26.dp
    val sprite = 48.dp

    // Starts walking once the hop has landed.
    val walked by animateFloatAsState(
        targetValue = position,
        animationSpec = if (reduceMotion) tween(0) else tween(600, delayMillis = 700, easing = FastOutSlowInEasing),
        label = "walk",
    )
    val clock = remember { Animatable(HOP_MS) }
    LaunchedEffect(hops) {
        if (hops > 0 && !reduceMotion) {
            clock.snapTo(0f)
            clock.animateTo(HOP_MS, tween(HOP_MS.toInt(), easing = LinearEasing))
        }
    }

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(156.dp)
            .clip(shape)
            .border(3.dp, Palette.ink, shape)
            .clearAndSetSemantics { contentDescription = s.stageCaption(done, total) },
    ) {
        val density = LocalDensity.current
        Canvas(Modifier.fillMaxSize()) {
            val g = ground.toPx()
            drawRect(Palette.skyLight)
            drawOval(Palette.hillFar, Offset(-30.dp.toPx(), size.height - 10.dp.toPx() - 90.dp.toPx()), Size(170.dp.toPx(), 90.dp.toPx()))
            drawOval(Palette.hillFarLight, Offset(150.dp.toPx(), size.height - 10.dp.toPx() - 70.dp.toPx()), Size(200.dp.toPx(), 70.dp.toPx()))
            drawRect(Palette.grassDark, Offset(0f, size.height - g), Size(size.width, 6.dp.toPx()))
            drawRect(Palette.dirt, Offset(0f, size.height - g + 6.dp.toPx()), Size(size.width, g - 6.dp.toPx()))
            for (x in listOf(40, 120, 220)) {
                drawRect(Palette.dirtDark, Offset(x.dp.toPx(), size.height - (5 + x % 7).dp.toPx() - 4.dp.toPx()), Size(4.dp.toPx(), 4.dp.toPx()))
            }
        }

        Text(s.stageCaption(done, total), style = Type.hint, modifier = Modifier.padding(start = 14.dp, top = 12.dp))

        // The finish flag.
        Box(Modifier.align(Alignment.BottomEnd).padding(end = 14.dp, bottom = ground).size(32.dp, 76.dp)) {
            Box(Modifier.size(4.dp, 76.dp).background(Palette.ink))
            CheckeredFlag(Modifier.offset(4.dp, 2.dp))
        }

        // The runner: cat, coin and "+1".
        val travel = (maxWidth - sprite - 16.dp - 80.dp).coerceAtLeast(0.dp)
        val t = clock.value
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = ground)
                .offset(x = 16.dp + travel * walked)
                .size(sprite),
        ) {
            val coinY = track(t, 0 to 0f, 180 to -50f, 600 to -112f, 960 to -128f)
            PixelSprite(
                Sprite.coin,
                Modifier
                    .size(28.dp)
                    .align(Alignment.BottomStart)
                    .graphicsLayer {
                        translationX = with(density) { 10.dp.toPx() }
                        translationY = with(density) { coinY.dp.toPx() }
                        rotationY = track(t, 0 to 0f, 180 to 0f, 960 to 1080f)
                        alpha = track(t, 0 to 0f, 180 to 0f, 260 to 1f, 660 to 1f, 960 to 0f)
                        cameraDistance = 12 * density.density
                    },
            )
            Text(
                "+1",
                style = Type.pixelNumber.copy(color = Color.White, shadow = Shadow(Palette.ink, Offset(4f, 4f), 0f)),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .graphicsLayer {
                        translationX = with(density) { 46.dp.toPx() }
                        translationY = with(density) { track(t, 0 to -92f, 320 to -92f, 1220 to -128f).dp.toPx() }
                        alpha = track(t, 0 to 0f, 320 to 0f, 470 to 1f, 1220 to 0f)
                    },
            )
            // Crouch, jump, hang at the top, land with a squash, settle.
            WaggingCat(
                animated = !reduceMotion,
                modifier = Modifier
                    .size(sprite)
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(0.5f, 1f)
                        translationY = with(density) { track(t, 0 to 0f, 80 to 0f, 280 to -50f, 380 to -50f, 550 to 0f).dp.toPx() }
                        scaleX = track(t, 0 to 1f, 80 to 1.16f, 280 to 0.9f, 380 to 1f, 550 to 1.14f, 700 to 1f)
                        scaleY = track(t, 0 to 1f, 80 to 0.8f, 280 to 1.1f, 380 to 1f, 550 to 0.86f, 700 to 1f)
                        rotationZ = track(t, 0 to 0f, 80 to 0f, 280 to -8f, 550 to 0f)
                    },
            )
        }
    }
}

/** Black-and-white finish flag. */
@Composable
fun CheckeredFlag(modifier: Modifier = Modifier) {
    Canvas(modifier.size(28.dp, 20.dp).border(2.dp, Palette.ink)) {
        val w = size.width / 3
        val h = size.height / 2
        for (row in 0 until 2) for (column in 0 until 3) {
            drawRect(if ((row + column) % 2 == 0) Palette.ink else Color.White, Offset(column * w, row * h), Size(w, h))
        }
    }
}

/** One pixel-art frame per tick of the idle animation. */
const val CAT_TICK_MS = 150L

/** The cat's pose for a given tick: two quick wags, a rest, and an occasional blink. */
fun catPose(tick: Int): Pair<Tail, Boolean> {
    val wag = listOf(Tail.LEFT, Tail.MIDDLE, Tail.RIGHT, Tail.MIDDLE, Tail.LEFT, Tail.MIDDLE, Tail.RIGHT, Tail.MIDDLE)
    val step = tick % 20
    return (wag.getOrNull(step) ?: Tail.MIDDLE) to (tick % 31 == 14)
}

/** The cat, idly wagging its tail in little bursts and blinking now and then. */
@Composable
fun WaggingCat(animated: Boolean, modifier: Modifier = Modifier) {
    var tick by remember { mutableIntStateOf(0) }
    if (animated) {
        LaunchedEffect(Unit) {
            while (true) {
                delay(CAT_TICK_MS)
                tick++
            }
        }
    }
    PixelSprite(Sprite.catPoses.getValue(catPose(tick)), modifier)
}
