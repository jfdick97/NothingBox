package de.jfd.straingauge

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

// Regeln für die Trenderkennung:
private const val MIN_DAYS_PER_WEEK = 4
private const val MIN_WEEKLY_CHANGE = 0.25
private const val MIN_TOTAL_CHANGE = 1.0

/**
 * Beschreibende Varianz der vorhandenen Werte.
 * Bei weniger als zwei Werten zeigen wir keine Varianz an.
 */
fun recordedVariance(values: List<Double>): Double? {
    if (values.size < 2) return null

    val mean = values.average()

    return values.sumOf { value ->
        val difference = value - mean
        difference * difference
    } / values.size
}

data class TrendWarning(
    val metricIndex: Int,
    val weeklyMeans: List<Double>
)

data class TrendAssessment(
    val start: LocalDate,
    val end: LocalDate,
    val assessedMetrics: Int,
    val warnings: List<TrendWarning>
)

fun assessTrends(
    days: List<DayEntry>,
    today: LocalDate = LocalDate.now()
): TrendAssessment {
    val currentMonday = today.with(
        TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
    )

    // Von der ältesten zur jüngsten abgeschlossenen Woche.
    val weekStarts = listOf(3L, 2L, 1L).map {
        currentMonday.minusWeeks(it)
    }

    val daysByDate = days.associateBy {
        LocalDate.parse(it.date)
    }

    val warnings = mutableListOf<TrendWarning>()
    var assessedMetrics = 0

    metricNames.indices.forEach { metric ->
        val availableMeans = weekStarts.map { weekStart ->
            val values = (0L..6L).mapNotNull { offset ->
                daysByDate[weekStart.plusDays(offset)]
                    ?.values()
                    ?.get(metric)
            }

            if (values.size >= MIN_DAYS_PER_WEEK) {
                values.average()
            } else {
                null
            }
        }

        // Keine fehlende Woche überspringen.
        if (availableMeans.any { it == null }) {
            return@forEach
        }

        val means = availableMeans.filterNotNull()
        assessedMetrics++

        // Anforderungen und Anspannung: steigend ungünstig.
        // Energie und Handlungsspielraum: fallend ungünstig.
        val direction = if (metric < 2) 1.0 else -1.0

        val weeklyChanges = means.zipWithNext { before, after ->
            direction * (after - before)
        }

        val totalChange =
            direction * (means.last() - means.first())

        val unfavorableTrend =
            weeklyChanges.all { it >= MIN_WEEKLY_CHANGE } &&
                    totalChange >= MIN_TOTAL_CHANGE

        if (unfavorableTrend) {
            warnings += TrendWarning(
                metricIndex = metric,
                weeklyMeans = means
            )
        }
    }

    return TrendAssessment(
        start = weekStarts.first(),
        end = currentMonday.minusDays(1),
        assessedMetrics = assessedMetrics,
        warnings = warnings
    )
}