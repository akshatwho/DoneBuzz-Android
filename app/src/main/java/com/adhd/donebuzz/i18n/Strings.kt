package com.adhd.donebuzz.i18n

import androidx.compose.runtime.staticCompositionLocalOf
import com.adhd.donebuzz.logic.DurationOption
import com.adhd.donebuzz.logic.WidgetMood
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** The language picked in Settings. */
enum class AppLanguage(val code: String) {
    CHINESE("zh-Hans"), ENGLISH("en"), SYSTEM("system");

    /** The language actually shown. SYSTEM follows the phone (Chinese if it is any kind of Chinese). */
    fun resolved(deviceLanguage: String = Locale.getDefault().language): AppLanguage =
        if (this != SYSTEM) this else if (deviceLanguage.startsWith("zh")) CHINESE else ENGLISH

    companion object {
        fun from(code: String?) = entries.firstOrNull { it.code == code } ?: CHINESE
    }
}

data class WhatsNewItem(val icon: String, val title: String, val body: String)

enum class SpeechProblem { PERMISSION_DENIED, UNAVAILABLE, NO_MATCH, FAILED }

/** Every piece of on-screen text, in Chinese and English. Screens read it from [LocalStrings]. */
class Strings(language: AppLanguage) {
    val language: AppLanguage = language.resolved()
    val isChinese: Boolean get() = language != AppLanguage.ENGLISH
    val locale: Locale get() = if (isChinese) Locale.SIMPLIFIED_CHINESE else Locale.ENGLISH
    /** BCP-47 tag for voice input. */
    val speechTag: String get() = if (isChinese) "zh-CN" else "en-US"

    private fun pick(zh: String, en: String) = if (isChinese) zh else en

    // Tabs and screens
    val todoTab get() = pick("待办", "To do")
    val doneTab get() = pick("已完成", "Done")
    val addItem get() = pick("添加待办", "Add item")
    val settings get() = pick("设置", "Settings")

    fun progress(done: Int, left: Int) = when {
        done == 0 -> pick("今天还有 $left 项要做", "$left to do today")
        left == 0 -> pick("今天的 $done 项全部完成 🎉", "All $done done for today 🎉")
        else -> pick("今天已完成 $done 项，还剩 $left 项", "$done done, $left to go")
    }

    fun stageCaption(done: Int, total: Int) =
        if (total == 0) pick("加一项待办，出发！", "Add an item to get going!")
        else pick("进度 $done / $total · 跑到终点就收工", "$done / $total · Reach the flag to finish")

    fun coins(count: Int) = pick("今天的金币 $count 枚", if (count == 1) "1 coin today" else "$count coins today")

    val emptyTitle get() = pick("暂无待办", "Nothing to do")
    val emptyHint get() = pick("点右下角的 + 添加第一条待办。", "Tap + at the bottom right to add your first item.")
    val markDone get() = pick("标记为完成", "Mark as done")
    val markNotDone get() = pick("标记为未完成", "Mark as not done")
    val delete get() = pick("删除", "Delete")
    val edit get() = pick("编辑", "Edit")

    // History
    val noneFinishedTitle get() = pick("还没有已完成的事项", "Nothing finished yet")
    val noneFinishedHint get() = pick("完成的事项会保存在这里。", "Items you finish are kept here.")

    fun dayPrefix(daysAgo: Long): String? = when (daysAgo) {
        0L -> pick("今天", "Today")
        1L -> pick("昨天", "Yesterday")
        2L -> if (isChinese) "前天" else null
        else -> null
    }

    fun dayTitle(day: LocalDate, today: LocalDate): String {
        val date = day.format(DateTimeFormatter.ofPattern(pick("M月d日 EEE", "EEE, MMM d"), locale))
        val prefix = dayPrefix(java.time.temporal.ChronoUnit.DAYS.between(day, today))
        return if (prefix != null) "$prefix · $date" else date
    }

    fun daySummary(count: Int, minutes: Int): String {
        val items = pick("$count 项", if (count == 1) "1 item" else "$count items")
        if (minutes <= 0) return items
        return pick("$items · 共 ${duration(minutes)}", "$items · ${duration(minutes)} total")
    }

    // Durations
    fun duration(minutes: Int): String {
        if (minutes < 60) return pick("$minutes 分钟", "$minutes min")
        val hours = minutes / 60
        val rest = minutes % 60
        val hourText = pick("$hours 小时", "$hours hr")
        return if (rest == 0) hourText else "$hourText ${duration(rest)}"
    }

    fun chipDuration(minutes: Int) = if (minutes == DurationOption.POMODORO) "🍅 " + duration(minutes) else duration(minutes)

    // Add / edit sheet
    val newItem get() = pick("新建待办", "New item")
    val editItem get() = pick("编辑待办", "Edit item")
    val titlePlaceholder get() = pick("要做什么？", "What do you need to do?")
    val listening get() = pick("正在聆听… 说完后再点一下麦克风", "Listening… tap the mic again when you're done")
    val howLong get() = pick("需要多久？（可选）", "How long? (optional)")
    val save get() = pick("保存", "Save")
    val add get() = pick("添加", "Add")
    val voiceInput get() = pick("语音输入", "Voice input")
    val stopVoiceInput get() = pick("停止语音输入", "Stop voice input")

    fun message(problem: SpeechProblem) = when (problem) {
        SpeechProblem.PERMISSION_DENIED -> pick("请在系统设置中允许使用麦克风", "Allow microphone access in system settings")
        SpeechProblem.UNAVAILABLE -> pick("语音识别暂时不可用", "Speech recognition isn't available right now")
        SpeechProblem.NO_MATCH -> pick("没听清，再说一次吧", "Didn't catch that. Try again")
        SpeechProblem.FAILED -> pick("无法开始录音", "Couldn't start recording")
    }

    // Streaks
    fun streakDays(days: Int) = pick("连续 $days 天", if (days == 1) "1-day streak" else "$days-day streak")
    fun bestStreak(days: Int) = pick("最长 $days 天", "Best $days")
    val streakRestHint get() = pick("休息一天不会断。", "One rest day won't break it.")
    val streakStart get() = pick("今天完成一项，开始你的连续记录。", "Finish one thing today to start a streak.")

    // Reminders
    val reminderTitle get() = pick("每日提醒", "Daily reminder")
    val reminderTime get() = pick("提醒时间", "Time")
    val reminderFootnote get() = pick("每天在这个时间轻轻提醒你挑一件小事。", "A gentle nudge each day to pick one small thing.")
    val reminderDenied get() = pick("通知已关闭。请在系统设置中打开通知。", "Notifications are off. Turn them on in system settings.")
    val openSystemSettings get() = pick("打开设置", "Open settings")
    val reminderChannel get() = pick("每日提醒", "Daily reminders")
    val focusChannel get() = pick("专注计时", "Focus timer")

    /** Wording for each weekday's notification (1 = Sunday ... 7 = Saturday). */
    fun reminderMessage(weekday: Int): String {
        val zh = listOf(
            "新的一周，先挑一件最小的事开始吧。",
            "只做一件小事就好，小猫在等你。",
            "5 分钟也算数，现在就开始吧。",
            "今天想先完成哪一件？",
            "挑一件小事，让小猫拿到今天的金币。",
            "快到周末了，再完成一件小事吧。",
            "周末也可以很轻松，只做一件就好。",
        )
        val en = listOf(
            "A new week. Start with the smallest thing.",
            "Just one small thing. The cat is waiting.",
            "Five minutes counts. Start now.",
            "Which one do you want to finish first today?",
            "Pick one small thing and earn today's coin.",
            "Almost the weekend. One more small thing?",
            "Weekends can be easy. Just do one.",
        )
        val index = weekday.coerceIn(1, 7) - 1
        return pick(zh[index], en[index])
    }

    // Focus timer
    val focusStart get() = pick("开始", "Start")
    val focusStop get() = pick("停止", "Stop")
    val focusing get() = pick("专注中", "Focusing")
    val timesUp get() = pick("时间到", "Time's up")
    fun focusEnded(title: String) = pick("“$title”的专注时间结束了。", "Your focus time for \"$title\" is over.")
    fun startFocus(title: String) = pick("开始专注：$title", "Start focus timer for $title")
    fun stopFocus(title: String) = pick("停止专注：$title", "Stop focus timer for $title")

    // What's new
    val whatsNewTitle get() = pick("新功能", "What's new")
    val whatsNewSubtitle get() = pick("这次更新带来了这些变化。", "Here's what changed in this update.")
    val whatsNewSettingsRow get() = pick("新功能介绍", "What's new")
    val gotIt get() = pick("知道了", "Got it")

    /** The features announced for WhatsNew.CURRENT_RELEASE. Replace this list when you bump the release. */
    val whatsNewItems: List<WhatsNewItem>
        get() = listOf(
            WhatsNewItem("🔥", pick("连续记录", "Streaks"), pick("每天完成一件就能延续火苗，休息一天也不会断。", "Finish one thing a day to keep the flame going. One rest day won't break it.")),
            WhatsNewItem("🔔", pick("每日提醒", "Daily reminders"), pick("在设置里选一个时间，每天轻轻提醒你。", "Pick a time in Settings for a gentle daily nudge.")),
            WhatsNewItem("⏱️", pick("专注计时", "Focus timer"), pick("点任务上的“开始”，倒计时会显示在状态栏。", "Tap Start on a task; the countdown shows in the status bar.")),
            WhatsNewItem("🐱", pick("桌面小组件", "Home screen widget"), pick("把小猫放到桌面上，随时看到今天的进度。", "Put the cat on your home screen to see today's progress at a glance.")),
        )

    // Settings
    val languageTitle get() = pick("语言", "Language")
    val languageFootnote get() = pick("语音输入也会跟着切换语言。", "Voice input switches language too.")
    val close get() = pick("完成", "Done")
    fun name(option: AppLanguage) = when (option) {
        AppLanguage.CHINESE -> "中文"
        AppLanguage.ENGLISH -> "English"
        AppLanguage.SYSTEM -> pick("跟随系统", "Same as phone")
    }

    // Widget
    fun widgetLine(mood: WidgetMood) = when (mood) {
        WidgetMood.Empty -> pick("加一件小事吧", "Add one tiny thing")
        is WidgetMood.Start -> pick("先做一件就好！", "Just start one!")
        is WidgetMood.Going -> pick("还剩 ${mood.left} 件，冲！", "${mood.left} to go, keep rolling!")
        WidgetMood.AllDone -> pick("今天全搞定！🎉", "All done today! 🎉")
        is WidgetMood.SaveStreak -> pick("别让火苗熄灭 🔥", "Keep your 🔥 alive!")
    }
    val nextUp get() = pick("接下来", "Next up")
    val restTime get() = pick("可以休息啦 ☕️", "Time to rest ☕️")
    val nothingOpen get() = pick("还没有待办，点 + 加一件", "Nothing yet. Tap + to add one")
    fun more(count: Int) = pick("还有 $count 件", "+$count more")
}

val LocalStrings = staticCompositionLocalOf { Strings(AppLanguage.CHINESE) }
