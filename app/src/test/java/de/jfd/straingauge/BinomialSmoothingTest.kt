package de.jfd.straingauge

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class BinomialSmoothingTest {
    private val start = LocalDate.of(2026, 11, 1)
    private fun entries(values: List<Double?>) = values.mapIndexed { index, value ->
        DayEntry(start.plusDays(index.toLong()).toString(), value, value, value, value)
    }
    private fun smooth(values: List<Double?>, width: Int) =
        binomialSmoothing(entries(values), width) { it.demands }

    @Test fun threeDayKernelAndPopulationSpread() {
        val point = smooth(listOf(2.0, 8.0, 2.0), 3).single()
        assertEquals(5.0, point.mean, 1e-12)
        assertEquals(3.0, point.deviation, 1e-12)
        assertEquals(start.toEpochDay() + 1.0, point.epochDay, 0.0)
    }

    @Test fun constantsIncludingZeroHaveNoSpreadForEveryWidth() {
        for (width in listOf(3, 7, 10)) {
            for (value in listOf(0.0, 5.0, 10.0)) {
                val points = smooth(List(14) { value }, width)
                assertEquals(14 - width + 1, points.size)
                points.forEach {
                    assertEquals(value, it.mean, 1e-12)
                    assertEquals(0.0, it.deviation, 1e-12)
                }
            }
        }
    }

    @Test fun sevenDayImpulseUsesPascalWeights() {
        val values = listOf(0.0, 0.0, 0.0, 10.0, 0.0, 0.0, 0.0)
        assertEquals(10.0 * 20.0 / 64.0, smooth(values, 7).single().mean, 1e-12)
    }

    @Test fun evenWindowIsCenteredBetweenDays() {
        val point = smooth((0..9).map { it.toDouble() }, 10).single()
        assertEquals(start.toEpochDay() + 4.5, point.epochDay, 0.0)
        assertEquals(4.5, point.mean, 1e-12)
        assertEquals(1.5, point.deviation, 1e-12)
    }

    @Test fun missingDatesAndNullValuesInvalidateSameWindows() {
        val all = entries(List(9) { 5.0 })
        val missingDate = binomialSmoothing(all.filterIndexed { i, _ -> i != 4 }, 3) { it.demands }
        val missingValue = smooth(List(9) { if (it == 4) null else 5.0 }, 3)
        assertEquals(missingDate, missingValue)
        assertEquals(listOf(1.0, 2.0, 6.0, 7.0), missingDate.map { it.epochDay - start.toEpochDay() })
    }

    @Test fun emptyShortAndNonFiniteWindowsAreExcluded() {
        assertTrue(smooth(emptyList(), 3).isEmpty())
        assertTrue(smooth(listOf(1.0, 2.0), 3).isEmpty())
        assertTrue(smooth(listOf(1.0, Double.NaN, 2.0), 3).isEmpty())
        assertTrue(smooth(listOf(1.0, Double.POSITIVE_INFINITY, 2.0), 3).isEmpty())
    }

    @Test fun inputOrderDoesNotChangeResults() {
        val days = entries(List(15) { it.toDouble() / 2 })
        assertEquals(binomialSmoothing(days, 7) { it.demands },
            binomialSmoothing(days.reversed(), 7) { it.demands })
    }

    @Test fun stressIndexRequiresAllFourValues() {
        val days = entries(List(3) { 5.0 }).mapIndexed { i, day ->
            if (i == 1) day.copy(energy = null) else day
        }
        assertTrue(binomialSmoothing(days, 3) { it.stressIndex() }.isEmpty())
        assertEquals(1, binomialSmoothing(days, 3) { it.demands }.size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun unsupportedWidthsAreRejected() { smooth(List(5) { 5.0 }, 5) }
}
