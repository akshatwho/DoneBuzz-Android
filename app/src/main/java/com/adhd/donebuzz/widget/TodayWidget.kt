package com.adhd.donebuzz.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.adhd.donebuzz.MainActivity
import com.adhd.donebuzz.app
import com.adhd.donebuzz.data.Days
import com.adhd.donebuzz.i18n.Strings
import com.adhd.donebuzz.logic.Streak
import com.adhd.donebuzz.logic.StreakInfo
import com.adhd.donebuzz.logic.WidgetMood
import com.adhd.donebuzz.ui.Palette
import com.adhd.donebuzz.ui.Sprite
import kotlinx.coroutines.flow.first
import java.time.LocalTime

/** What the widget shows, read straight from the database (the widget runs in the app's own process). */
data class WidgetData(
    val strings: Strings,
    val done: Int,
    val total: Int,
    val openTitles: List<String>,
    val streak: StreakInfo,
    val mood: WidgetMood,
) {
    companion object {
        suspend fun load(context: Context): WidgetData {
            val app = context.app
            val today = Days.today()
            val todos = app.database.todos().today(Days.startMillis(today)).first()
                .filter { it.completedAt == null || Days.of(it.completedAt) == today }
            val completions = app.database.todos().completionsSince(Days.startMillis(today.minusDays(400))).first()
            val streak = Streak.compute(completions.map(Days::of), today)
            val done = todos.count { it.isDone }
            return WidgetData(
                strings = app.settingsStore.current().strings,
                done = done,
                total = todos.size,
                openTitles = todos.filter { !it.isDone }.map { it.title },
                streak = streak,
                mood = WidgetMood.of(done, todos.size, streak, LocalTime.now().hour),
            )
        }
    }
}

class TodayWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(SMALL, MEDIUM))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetData.load(context)
        val cat = Sprite.cat.toBitmap(12)
        val coin = Sprite.coin.toBitmap(8)
        provideContent { Content(context, data, ImageProvider(cat), ImageProvider(coin)) }
    }

    companion object {
        val SMALL = DpSize(110.dp, 110.dp)
        val MEDIUM = DpSize(250.dp, 110.dp)
    }
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()
}

private fun fixed(color: Color) = ColorProvider(color, color)

@Composable
private fun Content(context: Context, data: WidgetData, cat: ImageProvider, coin: ImageProvider) {
    val s = data.strings
    val wide = LocalSize.current.width >= TodayWidget.MEDIUM.width
    val open = GlanceModifier.clickable(actionStartActivity(Intent(context, MainActivity::class.java)))
    val add = actionStartActivity(Intent(context, MainActivity::class.java).setAction(MainActivity.ACTION_ADD))

    Box(GlanceModifier.fillMaxSize().background(fixed(Palette.sky)).then(open), contentAlignment = Alignment.BottomStart) {
        // Grass and dirt along the bottom, like the app.
        Column(GlanceModifier.fillMaxWidth()) {
            Spacer(GlanceModifier.fillMaxWidth().height(2.dp).background(fixed(Palette.ink)))
            Spacer(GlanceModifier.fillMaxWidth().height(7.dp).background(fixed(Palette.grass)))
            Spacer(GlanceModifier.fillMaxWidth().height(16.dp).background(fixed(Palette.dirt)))
        }
        Row(GlanceModifier.fillMaxSize().padding(12.dp)) {
            Column(GlanceModifier.defaultWeight().fillMaxHeight()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Pill("🔥 ${data.streak.current}")
                    if (data.total > 0) {
                        Spacer(GlanceModifier.width(4.dp))
                        Pill("${data.done}/${data.total}", coin)
                    }
                }
                Spacer(GlanceModifier.height(6.dp))
                Bubble(s.widgetLine(data.mood))
                Spacer(GlanceModifier.defaultWeight())
                Row(verticalAlignment = Alignment.Bottom, modifier = GlanceModifier.padding(bottom = 12.dp)) {
                    Image(cat, contentDescription = null, modifier = GlanceModifier.size(40.dp))
                    if (data.mood == WidgetMood.AllDone) Image(coin, contentDescription = null, modifier = GlanceModifier.size(16.dp))
                    if (data.total > 0) {
                        Spacer(GlanceModifier.width(6.dp))
                        Image(
                            ImageProvider(progressBar(data.done, data.total)),
                            contentDescription = null,
                            modifier = GlanceModifier.defaultWeight().height(14.dp).padding(bottom = 4.dp),
                        )
                    }
                }
            }
            if (wide) {
                Spacer(GlanceModifier.width(10.dp))
                NextUp(data, add)
            }
        }
    }
}

@Composable
private fun Pill(text: String, icon: ImageProvider? = null) {
    Box(GlanceModifier.background(fixed(Palette.ink)).cornerRadius(14.dp).padding(2.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = GlanceModifier.background(fixed(Color.White)).cornerRadius(12.dp).padding(horizontal = 8.dp, vertical = 2.dp),
        ) {
            if (icon != null) {
                Image(icon, contentDescription = null, modifier = GlanceModifier.size(14.dp))
                Spacer(GlanceModifier.width(4.dp))
            }
            Text(text, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = fixed(Palette.ink)))
        }
    }
}

@Composable
private fun Bubble(text: String) {
    Box(GlanceModifier.background(fixed(Palette.ink)).cornerRadius(13.dp).padding(2.dp)) {
        Text(
            text,
            maxLines = 2,
            style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = fixed(Palette.ink)),
            modifier = GlanceModifier.background(fixed(Color.White)).cornerRadius(11.dp).padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun androidx.glance.layout.RowScope.NextUp(data: WidgetData, add: androidx.glance.action.Action) {
    val s = data.strings
    Box(GlanceModifier.defaultWeight().fillMaxHeight().padding(bottom = 18.dp)) {
        Box(GlanceModifier.fillMaxSize().background(fixed(Palette.ink)).cornerRadius(14.dp).padding(2.dp)) {
            Column(GlanceModifier.fillMaxSize().background(fixed(Palette.card)).cornerRadius(12.dp).padding(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth()) {
                    Text(s.nextUp, style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = fixed(Palette.muted)), modifier = GlanceModifier.defaultWeight())
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = GlanceModifier.size(28.dp).background(fixed(Palette.gold)).cornerRadius(14.dp).clickable(add)
                            .semantics { contentDescription = s.add },
                    ) { Text("+", style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = fixed(Palette.ink))) }
                }
                Spacer(GlanceModifier.height(4.dp))
                if (data.openTitles.isEmpty()) {
                    Text(if (data.total > 0) s.restTime else s.nothingOpen, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = fixed(Palette.ink)))
                } else {
                    data.openTitles.take(3).forEach { title ->
                        Text("○  $title", maxLines = 1, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = fixed(Palette.ink)))
                    }
                    if (data.openTitles.size > 3) {
                        Text(s.more(data.openTitles.size - 3), style = TextStyle(fontSize = 12.sp, color = fixed(Palette.muted)))
                    }
                }
            }
        }
    }
}

/** A chunky ink-outlined bar that fills with gold, drawn as a bitmap so it looks the same on every launcher. */
private fun progressBar(done: Int, total: Int): android.graphics.Bitmap {
    val w = 400f
    val h = 40f
    val bitmap = android.graphics.Bitmap.createBitmap(w.toInt(), h.toInt(), android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    val r = h / 2
    paint.color = android.graphics.Color.WHITE
    canvas.drawRoundRect(0f, 0f, w, h, r, r, paint)
    val fraction = if (total > 0) (done.toFloat() / total).coerceIn(0f, 1f) else 0f
    if (fraction > 0f) {
        paint.color = 0xFFF5B301.toInt()
        canvas.drawRoundRect(0f, 0f, maxOf(h, w * fraction), h, r, r, paint)
    }
    paint.style = android.graphics.Paint.Style.STROKE
    paint.strokeWidth = 6f
    paint.color = 0xFF14213D.toInt()
    canvas.drawRoundRect(3f, 3f, w - 3f, h - 3f, r - 3f, r - 3f, paint)
    return bitmap
}
