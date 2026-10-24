package de.jfd.straingauge

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

private const val MIN_DAYS_PER_WEEK = 4
private const val MIN_WEEKLY_CHANGE = 0.25
private const val MIN_TOTAL_CHANGE = 1.0
private const val EPSILON = 0.000000001

fun recordedVariance(values: List<Double>): Double? {
    if (values.size < 2) return null

    val mean = values.average()

    return values.sumOf { value ->
        val difference = value - mean
        difference * difference
    } / values.size
}

data class MetricTrend(
    val metricIndex: Int,
    val weeklyMeans: List<Double>
)

data class TrendAssessment(
    val start: LocalDate,
    val end: LocalDate,
    val assessedMetrics: Int,
    val warnings: List<MetricTrend>,
    val improvements: List<MetricTrend>
)

/**
 * Prüft einen durchgängigen Trend in der angegebenen Richtung.
 *
 * direction = 1.0: steigende Werte
 * direction = -1.0: fallende Werte
 */
private fun followsDirection(
    means: List<Double>,
    direction: Double
): Boolean {
    if (means.size < 3) return false

    val weeklyChanges = means.zipWithNext { before, after ->
        direction * (after - before)
    }

    val totalChange =
        direction * (means.last() - means.first())

    return weeklyChanges.all {
        it >= MIN_WEEKLY_CHANGE - EPSILON
    } && totalChange >= MIN_TOTAL_CHANGE - EPSILON
}

fun assessTrends(
    days: List<DayEntry>,
    today: LocalDate = LocalDate.now()
): TrendAssessment {
    val currentMonday = today.with(
        TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
    )

    val weekStarts = listOf(3L, 2L, 1L).map {
        currentMonday.minusWeeks(it)
    }

    val daysByDate = days.associateBy {
        LocalDate.parse(it.date)
    }

    val warnings = mutableListOf<MetricTrend>()
    val improvements = mutableListOf<MetricTrend>()

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

        if (availableMeans.any { it == null }) {
            return@forEach
        }

        val means = availableMeans.filterNotNull()
        assessedMetrics++

        // Anforderungen und Anspannung:
        // steigende Werte sind ungünstig.
        //
        // Energie und Handlungsspielraum:
        // fallende Werte sind ungünstig.
        val unfavorableDirection =
            if (metric < 2) 1.0 else -1.0

        if (followsDirection(means, unfavorableDirection)) {
            warnings += MetricTrend(
                metricIndex = metric,
                weeklyMeans = means
            )
        } else if (
            followsDirection(means, -unfavorableDirection)
        ) {
            improvements += MetricTrend(
                metricIndex = metric,
                weeklyMeans = means
            )
        }
    }

    return TrendAssessment(
        start = weekStarts.first(),
        end = currentMonday.minusDays(1),
        assessedMetrics = assessedMetrics,
        warnings = warnings,
        improvements = improvements
    )
}

fun DayEntry.stressIndex(): Double? {
    val demandValue = demands ?: return null
    val tensionValue = tension ?: return null
    val energyValue = energy ?: return null
    val agencyValue = agency ?: return null

    return (
            demandValue +
                    tensionValue +
                    (10.0 - energyValue) +
                    (10.0 - agencyValue)
            ) / 4.0
}