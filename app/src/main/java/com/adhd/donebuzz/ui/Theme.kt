package com.adhd.donebuzz.ui

import android.graphics.Bitmap
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Side-scroller palette: sky, grass, dirt and gold coins, every shape outlined in dark ink.
 * One job per colour: gold = done / reward, orange = time, ink = text and outlines.
 */
object Palette {
    val sky = Color(0xFF8FD3FF)
    val skyLight = Color(0xFFC8EBFF)
    val ink = Color(0xFF14213D)
    val muted = Color(0xFF4A5568)
    val gold = Color(0xFFF5B301)
    val goldLight = Color(0xFFFFE27A)
    val grass = Color(0xFF5BBF5A)
    val grassDark = Color(0xFF4FAE57)
    val hill = Color(0xFF6CCB6A)
    val hillFar = Color(0xFF9FDB8E)
    val hillFarLight = Color(0xFFB6E6A6)
    val dirt = Color(0xFFC98B4F)
    val dirtDark = Color(0xFF9C6A3C)
    val card = Color(0xFFFFFDF6)
    val doneCard = Color(0xFFFFF6D6)
    val tagBackground = Color(0xFFFFE7C2)
    val tagText = Color(0xFF8A3E00)
    val recording = Color(0xFFE5484D)
}

/**
 * Easy-to-read type: heavy letters, large sizes and loose spacing so each line can be taken in at a
 * glance. Sizes are in sp, so they still follow the system font-size setting.
 */
object Type {
    val screenTitle = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Black, color = Palette.ink)
    val itemTitle = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.8.sp, lineHeight = 30.sp)
    val sheetTitle = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Palette.ink)
    val progress = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.8.sp, color = Palette.ink)
    val historyTitle = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
    val label = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold)
    val hint = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Palette.ink)
    val button = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Palette.ink)
    val pixelNumber = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, color = Palette.ink)
}

/** The chunky game look: a fill, a dark ink outline and a hard shadow straight below. */
fun Modifier.gameSurface(fill: Color, shape: Shape, outline: Dp = 3.dp, drop: Dp = 4.dp, shadow: Color = Palette.ink) = this
    .drawBehind {
        val o = shape.createOutline(size, layoutDirection, this)
        translate(top = drop.toPx()) { drawOutline(o, shadow) }
        drawOutline(o, fill)
    }
    .border(outline, Palette.ink, shape)

/** True when the system's animations are switched off (Remove animations / animator scale 0). */
@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

// MARK: Pixel art

/** Where the cat's tail is pointing; cycling left, middle, right, middle makes it wag. */
enum class Tail { LEFT, MIDDLE, RIGHT }

/** A sprite drawn from a grid of coloured rectangles, so pixel art stays crisp at any size. */
class Sprite(val columns: Int, val rows: Int, val blocks: List<Block>) {
    data class Block(val x: Int, val y: Int, val w: Int, val h: Int, val color: Color)

    /** For places that need a bitmap, like the home screen widget. */
    fun toBitmap(cellPx: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(columns * cellPx, rows * cellPx, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint()
        for (b in blocks) {
            paint.color = b.color.toArgb()
            canvas.drawRect(
                (b.x * cellPx).toFloat(), (b.y * cellPx).toFloat(),
                ((b.x + b.w) * cellPx).toFloat(), ((b.y + b.h) * cellPx).toFloat(), paint,
            )
        }
        return bitmap
    }

    companion object {
        private fun b(x: Int, y: Int, w: Int, h: Int, c: Color) = Block(x, y, w, h, c)

        /** Builds a sprite from rows of characters, one cell per character; unknown characters are see-through. */
        fun fromArt(art: List<String>, palette: Map<Char, Color>): Sprite {
            val blocks = art.flatMapIndexed { y, row ->
                row.mapIndexedNotNull { x, c -> palette[c]?.let { b(x, y, 1, 1, it) } }
            }
            return Sprite(art.maxOf { it.length }, art.size, blocks)
        }

        private val catPalette = mapOf(
            'O' to Color(0xFFF08A24), // fur
            'S' to Color(0xFFC9661A), // stripes, paws and tail tip
            'P' to Color(0xFFFFB3C1), // inner ears
            'C' to Color(0xFFFFF1DE), // muzzle and belly
            'N' to Color(0xFFE5577A), // nose
            'R' to Color(0xFFFF9FB0), // cheeks
            'B' to Color(0xFF3D7BFF), // scarf
            'K' to Palette.ink, // eyes
            'W' to Color.White, // eye sparkle
        )

        private val catArt = listOf(
            ".............",
            ".O......O....",
            ".OO....OO....",
            ".OPOSSOPO....",
            "OOOOSSOOOO...",
            "OOOOOOOOOO...",
            "OKWOOOOKWO...",
            "OKKONNOKKO...",
            "ORROCCORRO...",
            ".OOOOOOOO....",
            "..BBBBBB.....",
            "..OCCCCO.....",
            "..SS..SS.....",
        )

        /** Tail cells for each pose, from the base at the cat's hip out to the darker tip. */
        private val tails = mapOf(
            Tail.LEFT to listOf(8 to 11, 9 to 11, 10 to 10, 10 to 9, 11 to 8, 11 to 7),
            Tail.MIDDLE to listOf(8 to 11, 9 to 11, 10 to 10, 11 to 9, 11 to 8, 12 to 7),
            Tail.RIGHT to listOf(8 to 11, 9 to 11, 10 to 11, 11 to 10, 12 to 9, 12 to 8),
        )

        /** The hero: a little orange cat in a blue scarf, with big sparkly eyes and pink cheeks. */
        fun cat(tail: Tail = Tail.MIDDLE, blinking: Boolean = false): Sprite {
            val art = catArt.map { it.toCharArray() }.toMutableList()
            if (blinking) art[6] = "OOOOOOOOOO...".toCharArray()
            val cells = tails.getValue(tail)
            cells.forEachIndexed { i, (x, y) -> art[y][x] = if (i == cells.lastIndex) 'S' else 'O' }
            return fromArt(art.map { String(it) }, catPalette)
        }

        val cat: Sprite = cat()

        /** All the cat's poses, built once so animating it allocates nothing. */
        val catPoses: Map<Pair<Tail, Boolean>, Sprite> =
            Tail.entries.flatMap { t -> listOf(false, true).map { (t to it) to cat(t, it) } }.toMap()

        val coin: Sprite = run {
            val rim = Color(0xFFA66F00)
            val slot = Color(0xFFC98A00)
            Sprite(
                8, 8,
                listOf(
                    b(2, 0, 4, 1, rim), b(1, 1, 1, 1, rim), b(6, 1, 1, 1, rim),
                    b(0, 2, 1, 4, rim), b(7, 2, 1, 4, rim),
                    b(1, 6, 1, 1, rim), b(6, 6, 1, 1, rim), b(2, 7, 4, 1, rim),
                    b(2, 1, 4, 1, Palette.gold), b(1, 2, 6, 4, Palette.gold), b(2, 6, 4, 1, Palette.gold),
                    b(2, 2, 1, 2, Palette.goldLight),
                    b(4, 2, 1, 4, slot),
                ),
            )
        }

        val cloud = Sprite(10, 4, listOf(b(2, 0, 4, 1, Color.White), b(1, 1, 7, 1, Color.White), b(0, 2, 10, 2, Color.White)))
    }
}

@Composable
fun PixelSprite(sprite: Sprite, modifier: Modifier = Modifier) {
    Canvas(modifier.aspectRatio(sprite.columns.toFloat() / sprite.rows)) {
        val cell = minOf(size.width / sprite.columns, size.height / sprite.rows)
        for (b in sprite.blocks) {
            // A hair of overlap hides the seams between neighbouring blocks.
            drawRect(b.color, Offset(b.x * cell, b.y * cell), Size(b.w * cell + 0.5f, b.h * cell + 0.5f))
        }
    }
}
