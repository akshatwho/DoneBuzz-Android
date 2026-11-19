package com.adhd.donebuzz.logic

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

/** The quick-pick durations offered when adding an item, in minutes. */
object DurationOption {
    val all = listOf(5, 15, 25, 45, 60)
    /** One pomodoro. */
    const val POMODORO = 25
}

/** Decides whether the What's New sheet appears at launch. */
object WhatsNew {
    /** Bump this (and the list in Strings.whatsNewItems) with every release that has news worth showing. */
    const val CURRENT_RELEASE = 1

    enum class Action { SHOW, RECORD_SILENTLY, NOTHING }

    fun launchAction(seenRelease: Int, hasExistingData: Boolean, current: Int = CURRENT_RELEASE): Action = when {
        seenRelease >= current -> Action.NOTHING
        // A brand new user has nothing to compare against, so there is no "new" to show.
        !hasExistingData -> Action.RECORD_SILENTLY
        else -> Action.SHOW
    }
}

/** The cat's mood on the home screen widget, which picks the line it says. */
sealed interface WidgetMood {
    data object Empty : WidgetMood
    data class Start(val left: Int) : WidgetMood
    data class Going(val left: Int) : WidgetMood
    data object AllDone : WidgetMood
    data class SaveStreak(val days: Int) : WidgetMood

    companion object {
        /** From this hour on, an unbroken streak with nothing done today gets the gentle nudge. */
        const val EVENING_HOUR = 18

        fun of(done: Int, total: Int, streak: StreakInfo, hour: Int): WidgetMood {
            val left = total - done
            return when {
                total == 0 -> Empty
                left <= 0 -> AllDone
                done == 0 && streak.current > 0 && !streak.doneToday && hour >= EVENING_HOUR -> SaveStreak(streak.current)
                done == 0 -> Start(left)
                else -> Going(left)
            }
        }
    }
}

/** When the next daily reminder should fire. */
object ReminderTime {
    const val DEFAULT_MINUTES = 9 * 60

    fun next(minutesOfDay: Int, now: LocalDateTime): LocalDateTime {
        val time = LocalTime.of(minutesOfDay / 60 % 24, minutesOfDay % 60)
        val today = now.toLocalDate().atTime(time)
        return if (today.isAfter(now)) today else today.plusDays(1)
    }

    /** iOS numbering used by the message list: 1 = Sunday ... 7 = Saturday. */
    fun weekdayIndex(day: DayOfWeek): Int = day.value % 7 + 1
}
