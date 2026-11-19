package com.adhd.donebuzz.logic

import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class StreakInfo(
    /** Days with something finished in the current run (a single rest day inside it is fine). */
    val current: Int,
    /** The longest such run on record. */
    val best: Int,
    val doneToday: Boolean,
) {
    companion object {
        val EMPTY = StreakInfo(0, 0, false)
    }
}

/**
 * A forgiving streak: it counts days on which something was finished, and one day off in a row
 * does not break it. Two empty days in a row do.
 */
object Streak {
    fun compute(completionDays: Collection<LocalDate>, today: LocalDate = LocalDate.now()): StreakInfo {
        val days = completionDays.toSortedSet()
        val oldest = days.firstOrNull() ?: return StreakInfo.EMPTY
        val doneToday = today in days

        // Current run. Today does not count against you until it is over, so start from yesterday.
        var current = if (doneToday) 1 else 0
        var missedInARow = 0
        var cursor = today.minusDays(1)
        while (missedInARow < 2 && !cursor.isBefore(oldest)) {
            if (cursor in days) {
                current += 1
                missedInARow = 0
            } else {
                missedInARow += 1
            }
            cursor = cursor.minusDays(1)
        }

        // Longest run: active days at most two days apart.
        var best = 0
        var run = 0
        var previous: LocalDate? = null
        for (day in days) {
            run = if (previous != null && ChronoUnit.DAYS.between(previous, day) <= 2) run + 1 else 1
            best = maxOf(best, run)
            previous = day
        }

        return StreakInfo(current, maxOf(best, current), doneToday)
    }
}
