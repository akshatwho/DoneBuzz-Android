package com.adhd.donebuzz.logic

import java.time.LocalDate

/**
 * How far the cat has walked toward the flag today, from 0 (start) to 1 (flag).
 *
 * The cat only ever walks backwards when you un-tick an item. Adding or deleting items never moves it.
 * Each finished item covers an equal share of the road that is left, so the cat still reaches the flag
 * exactly when the last open item is ticked off, however many items were added along the way.
 */
data class CatProgress(
    /** The day this belongs to; a new day starts the cat from scratch. */
    val day: LocalDate,
    val position: Double,
    /** The counts [position] was last worked out for, so the next change can be told apart. */
    val done: Int,
    val total: Int,
) {
    /** Brings the position up to date with the day's current counts. */
    fun update(done: Int, total: Int, today: LocalDate = LocalDate.now()): CatProgress {
        if (day != today) return fresh(done, total, today)

        val doneChange = done - this.done
        val totalChange = total - this.total
        if (doneChange == 0 && totalChange == 0) return this

        val open = total - done
        val openBefore = this.total - this.done
        var p = position

        when {
            // Items were added, or open items deleted: nothing was finished, so the cat stays put.
            doneChange == 0 -> Unit
            // One item finished: walk an equal share of the road that is left.
            doneChange == 1 && totalChange == 0 -> p += (1 - p) / maxOf(openBefore, 1)
            // One item un-ticked: take back exactly that share.
            doneChange == -1 && totalChange == 0 ->
                p = if (open > 1) (p * open - 1) / (open - 1) else ratio(done, total)
            // Only finished items were deleted: the road ahead is unchanged, so stay put.
            doneChange < 0 && totalChange == doneChange -> Unit
            // Anything else (several kinds of change at once): fall back to the plain share done.
            else -> p = ratio(done, total)
        }

        if (total <= 0 || done <= 0) p = 0.0 else if (open <= 0) p = 1.0

        return CatProgress(today, p.coerceIn(0.0, 1.0), done, total)
    }

    /** Compact text form for storage: `epochDay|position|done|total`. */
    fun encode(): String = "${day.toEpochDay()}|$position|$done|$total"

    companion object {
        fun fresh(done: Int, total: Int, day: LocalDate) = CatProgress(day, ratio(done, total), done, total)

        fun ratio(done: Int, total: Int): Double =
            if (total <= 0) 0.0 else (done.toDouble() / total).coerceIn(0.0, 1.0)

        fun decode(text: String?, today: LocalDate = LocalDate.now()): CatProgress {
            val parts = text?.split("|")
            if (parts == null || parts.size != 4) return fresh(0, 0, today)
            return runCatching {
                CatProgress(LocalDate.ofEpochDay(parts[0].toLong()), parts[1].toDouble(), parts[2].toInt(), parts[3].toInt())
            }.getOrElse { fresh(0, 0, today) }
        }
    }
}
