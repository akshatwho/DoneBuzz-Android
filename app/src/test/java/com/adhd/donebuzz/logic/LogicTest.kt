package com.adhd.donebuzz.logic

import com.adhd.donebuzz.i18n.AppLanguage
import com.adhd.donebuzz.i18n.Strings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

class CatProgressTest {
    private val day = LocalDate.of(2026, 11, 19)
    private fun start(done: Int, total: Int) = CatProgress.fresh(done, total, day)

    @Test fun addingItemsNeverMovesTheCat() {
        val cat = start(2, 5).update(2, 6, day)
        assertEquals(0.4, cat.position, 1e-9)
    }

    @Test fun addingSeveralAtOnceStaysPut() {
        assertEquals(0.4, start(2, 5).update(2, 8, day).position, 1e-9)
    }

    @Test fun finishingWalksAnEqualShareOfWhatIsLeft() {
        val cat = start(2, 6).update(3, 6, day)
        assertEquals(1.0 / 3 + (1 - 1.0 / 3) / 4, cat.position, 1e-9)
    }

    @Test fun lastItemReachesTheFlag() {
        var cat = start(0, 3)
        cat = cat.update(1, 3, day).update(1, 5, day).update(2, 5, day).update(3, 5, day).update(4, 5, day).update(5, 5, day)
        assertEquals(1.0, cat.position, 1e-9)
    }

    @Test fun untickingTakesBackExactlyThatShare() {
        val before = start(2, 6).update(2, 8, day)
        val after = before.update(3, 8, day).update(2, 8, day)
        assertEquals(before.position, after.position, 1e-9)
    }

    @Test fun deletingOpenItemStaysPut() {
        assertEquals(0.4, start(2, 5).update(2, 4, day).position, 1e-9)
    }

    @Test fun deletingFinishedItemStaysPut() {
        assertEquals(0.4, start(2, 5).update(1, 4, day).position, 1e-9)
    }

    @Test fun nothingDoneIsAtTheStart() {
        assertEquals(0.0, start(1, 3).update(0, 3, day).position, 1e-9)
    }

    @Test fun newDayStartsFresh() {
        val cat = start(3, 4).update(0, 2, day.plusDays(1))
        assertEquals(0.0, cat.position, 1e-9)
        assertEquals(day.plusDays(1), cat.day)
    }

    @Test fun encodesAndDecodes() {
        val cat = start(2, 7)
        assertEquals(cat, CatProgress.decode(cat.encode(), day))
        assertEquals(CatProgress.fresh(0, 0, day), CatProgress.decode("garbage", day))
        assertEquals(CatProgress.fresh(0, 0, day), CatProgress.decode(null, day))
    }
}

class StreakTest {
    private val today = LocalDate.of(2026, 11, 19)
    private fun days(vararg ago: Long) = ago.map { today.minusDays(it) }

    @Test fun emptyHasNoStreak() = assertEquals(StreakInfo.EMPTY, Streak.compute(emptyList(), today))
    @Test fun todayOnly() = assertEquals(StreakInfo(1, 1, true), Streak.compute(days(0), today))
    @Test fun yesterdayStillCountsBeforeTodayIsDone() = assertEquals(2, Streak.compute(days(1, 2), today).current)
    @Test fun oneRestDayDoesNotBreak() = assertEquals(3, Streak.compute(days(0, 2, 3), today).current)
    @Test fun twoEmptyDaysBreak() = assertEquals(1, Streak.compute(days(0, 3, 4), today).current)
    @Test fun lapsedStreakIsZero() = assertEquals(0, Streak.compute(days(3, 4), today).current)
    @Test fun bestRemembersOlderRuns() = assertEquals(4, Streak.compute(days(0, 10, 11, 12, 13), today).best)
    @Test fun manyCompletionsOnOneDayCountOnce() = assertEquals(1, Streak.compute(days(0, 0, 0), today).current)
    @Test fun doneTodayFlag() {
        assertTrue(Streak.compute(days(0), today).doneToday)
        assertFalse(Streak.compute(days(1), today).doneToday)
    }
}

class WidgetMoodTest {
    private val streak = StreakInfo(4, 4, false)

    @Test fun moods() {
        assertEquals(WidgetMood.Empty, WidgetMood.of(0, 0, StreakInfo.EMPTY, 10))
        assertEquals(WidgetMood.Start(2), WidgetMood.of(0, 2, StreakInfo.EMPTY, 10))
        assertEquals(WidgetMood.Going(1), WidgetMood.of(2, 3, StreakInfo.EMPTY, 10))
        assertEquals(WidgetMood.AllDone, WidgetMood.of(3, 3, StreakInfo.EMPTY, 10))
    }

    @Test fun eveningNudgeOnlyWhenStreakAtRisk() {
        assertEquals(WidgetMood.SaveStreak(4), WidgetMood.of(0, 1, streak, 19))
        assertEquals(WidgetMood.Start(1), WidgetMood.of(0, 1, streak, 9))
        assertEquals(WidgetMood.Start(1), WidgetMood.of(0, 1, StreakInfo.EMPTY, 20))
        assertEquals(WidgetMood.Start(1), WidgetMood.of(0, 1, streak.copy(doneToday = true), 20))
    }
}

class WhatsNewTest {
    @Test fun showsForExistingUsers() = assertEquals(WhatsNew.Action.SHOW, WhatsNew.launchAction(0, true, 1))
    @Test fun silentForFreshInstall() = assertEquals(WhatsNew.Action.RECORD_SILENTLY, WhatsNew.launchAction(0, false, 1))
    @Test fun nothingOnceSeen() = assertEquals(WhatsNew.Action.NOTHING, WhatsNew.launchAction(1, true, 1))
}

class ReminderTimeTest {
    @Test fun laterTodayWhenStillAhead() {
        val now = LocalDateTime.of(2026, 11, 19, 8, 0)
        assertEquals(LocalDateTime.of(2026, 11, 19, 9, 0), ReminderTime.next(540, now))
    }

    @Test fun tomorrowWhenPassed() {
        val now = LocalDateTime.of(2026, 11, 19, 9, 0)
        assertEquals(LocalDateTime.of(2026, 11, 20, 9, 0), ReminderTime.next(540, now))
    }

    @Test fun weekdayNumberingMatchesIos() {
        assertEquals(1, ReminderTime.weekdayIndex(DayOfWeek.SUNDAY))
        assertEquals(2, ReminderTime.weekdayIndex(DayOfWeek.MONDAY))
        assertEquals(7, ReminderTime.weekdayIndex(DayOfWeek.SATURDAY))
    }
}

class StringsTest {
    private val zh = Strings(AppLanguage.CHINESE)
    private val en = Strings(AppLanguage.ENGLISH)

    @Test fun durations() {
        assertEquals("25 分钟", zh.duration(25))
        assertEquals("1 hr 15 min", en.duration(75))
        assertEquals("🍅 25 min", en.chipDuration(25))
    }

    @Test fun progress() {
        assertEquals("3 to do today", en.progress(0, 3))
        assertEquals("今天已完成 1 项，还剩 2 项", zh.progress(1, 2))
        assertEquals("All 2 done for today 🎉", en.progress(2, 0))
    }

    @Test fun dayTitles() {
        val today = LocalDate.of(2026, 11, 19)
        assertTrue(zh.dayTitle(today.minusDays(2), today).startsWith("前天 · "))
        assertFalse(en.dayTitle(today.minusDays(2), today).contains("·"))
        assertTrue(en.dayTitle(today, today).startsWith("Today · "))
    }

    @Test fun systemLanguageResolves() {
        assertEquals(AppLanguage.CHINESE, AppLanguage.SYSTEM.resolved("zh"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.SYSTEM.resolved("fr"))
        assertEquals(AppLanguage.CHINESE, AppLanguage.from("bogus"))
    }

    @Test fun widgetLines() {
        assertEquals("2 to go, keep rolling!", en.widgetLine(WidgetMood.Going(2)))
        assertEquals("还剩 2 件，冲！", zh.widgetLine(WidgetMood.Going(2)))
    }
}

class CatPoseTest {
    @Test fun wagsThenRests() {
        val tails = (0 until 20).map { com.adhd.donebuzz.ui.catPose(it).first }
        assertEquals(com.adhd.donebuzz.ui.Tail.LEFT, tails[0])
        assertEquals(com.adhd.donebuzz.ui.Tail.RIGHT, tails[2])
        assertTrue(tails.drop(8).all { it == com.adhd.donebuzz.ui.Tail.MIDDLE })
    }

    @Test fun blinksOncePerCycle() {
        assertEquals(1, (0 until 31).count { com.adhd.donebuzz.ui.catPose(it).second })
    }
}
