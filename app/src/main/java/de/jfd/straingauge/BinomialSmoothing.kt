package de.jfd.straingauge

import java.time.LocalDate
import kotlin.math.sqrt

/** Calendar position may be a half day for an even-sized window. */
data class SmoothedPoint(val epochDay: Double, val mean: Double, val deviation: Double)

/** Centered binomial mean and population spread, using only complete daily windows.
 * Missing/null/non-finite values invalidate a window; zero remains a valid value.
 * The caller supplies the selected period, so no values outside it contribute.
 */
fun binomialSmoothing(
    days: List<DayEntry>,
    width: Int,
    valueOf: (DayEntry) -> Double?
): List<SmoothedPoint> {
    require(width == 3 || width == 7 || width == 10)
    val values = days.associate { day ->
        LocalDate.parse(day.date).toEpochDay() to valueOf(day)?.takeIf { it.isFinite() }
    }
    val weights = DoubleArray(width)
    weights[0] = 1.0
    for (i in 1 until width) weights[i] = weights[i - 1] * (width - i) / i
    val weightSum = weights.sum()

    return buildList {
        // Iterate recorded dates rather than all dates in potentially sparse archives.
        for (start in values.keys.sorted()) {
            val window = (0 until width).map { values[start + it] }
            if (window.any { it == null }) continue
            val complete = window.filterNotNull()
            val mean = complete.indices.sumOf { weights[it] * complete[it] } / weightSum
            val variance = complete.indices.sumOf {
                val delta = complete[it] - mean
                weights[it] * delta * delta
            } / weightSum
            add(SmoothedPoint(start + (width - 1) / 2.0, mean, sqrt(variance.coerceAtLeast(0.0))))
        }
    }
}
