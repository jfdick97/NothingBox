package de.jfd.straingauge

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import java.time.format.DateTimeFormatter
import java.util.Locale

private val trendDateFormat get() =
    DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM)
        .withLocale(AppStrings.locale)

private fun trendNumber(value: Double): String =
    String.format(AppStrings.locale, "%.1f", value)

@Composable
fun TrendCard(assessment: TrendAssessment) {
    val negativeCount = assessment.warnings.size
    val positiveCount = assessment.improvements.size

    val dark =
        MaterialTheme.colorScheme.background.luminance() < 0.5f

    val positiveBackground = if (dark) {
        Color(0xFF213B30)
    } else {
        Color(0xFFDDEDE5)
    }

    val positiveForeground = if (dark) {
        Color(0xFFD5EBDD)
    } else {
        Color(0xFF183C2B)
    }

    val (containerColor, contentColor) = when {
        negativeCount >= 3 -> {
            if (dark) {
                Color(0xFF442F2E) to Color(0xFFF3DCD6)
            } else {
                Color(0xFFF1DFDA) to Color(0xFF573A34)
            }
        }

        negativeCount == 2 -> {
            if (dark) {
                Color(0xFF433122) to Color(0xFFF5DDC5)
            } else {
                Color(0xFFF9E4CB) to Color(0xFF56371C)
            }
        }

        negativeCount == 1 -> {
            if (dark) {
                Color(0xFF3B3220) to Color(0xFFF1DFC0)
            } else {
                Color(0xFFFFF1D6) to Color(0xFF57401B)
            }
        }

        positiveCount > 0 -> {
            positiveBackground to positiveForeground
        }

        else -> {
            MaterialTheme.colorScheme.surfaceContainerLow to
                    MaterialTheme.colorScheme.onSurface
        }
    }

    val title = when {
        negativeCount >= 3 ->
            tr(R.string.trend_time_to_take_good_care_of_yourself)

        negativeCount == 2 ->
            tr(R.string.trend_two_developments_to_keep_an_eye_on)

        negativeCount == 1 ->
            tr(R.string.trend_a_gentle_reminder_for_you)

        positiveCount == 1 ->
            tr(R.string.trend_an_encouraging_development)

        positiveCount > 1 ->
            tr(R.string.trend_encouraging_developments)

        assessment.assessedMetrics == 0 ->
            tr(R.string.trend_your_trends_need_a_little_more_time)

        else ->
            tr(R.string.trend_your_trends_at_a_glance)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                fontFamily = Garamond,
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text =
                    "${assessment.start.format(trendDateFormat)} – " +
                            assessment.end.format(trendDateFormat),
                style = MaterialTheme.typography.labelMedium
            )

            if (assessment.assessedMetrics == 0) {
                Text(
                    tr(R.string.trend_there_is_not_enough_data_for_a_trend)
                )
            } else {
                if (negativeCount > 0) {
                    Text(
                        text = tr(R.string.trend_unfavourable_developments, negativeCount),
                        style = MaterialTheme.typography.titleSmall
                    )

                    assessment.warnings.forEach { trend ->
                        TrendDetails(
                            trend = trend,
                            positive = false
                        )
                    }

                    Text(
                        text = negativeTrendMessage(
                            assessment.warnings
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (positiveCount > 0) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = positiveBackground,
                        contentColor = positiveForeground
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement =
                                Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = tr(R.string.trend_things_moving_in_a_helpful_direction),
                                style =
                                    MaterialTheme.typography.titleMedium
                            )

                            assessment.improvements.forEach { trend ->
                                TrendDetails(
                                    trend = trend,
                                    positive = true
                                )
                            }

                            Text(
                                text = positiveTrendMessage(
                                    assessment.improvements
                                ),
                                style =
                                    MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                if (
                    negativeCount == 0 &&
                    positiveCount == 0
                ) {
                    Text(
                        tr(R.string.trend_the_available_metrics_currently_show_no_pronounced_trend)
                    )

                    Text(
                        tr(R.string.trend_a_pattern_without_a_clear_direction_can_simply)
                    )
                }

                if (
                    assessment.assessedMetrics < metricNames.size
                ) {
                    Text(
                        text =
                            tr(R.string.trend_of_metrics_can_be_assessed_there_are_not, assessment.assessedMetrics, metricNames.size),
                        style =
                            MaterialTheme.typography.bodySmall
                    )
                }

                Text(
                    text = tr(R.string.trend_this_assessment_looks_at_changes_across_three_completed),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun TrendDetails(
    trend: MetricTrend,
    positive: Boolean
) {
    val title = if (positive) {
        when (trend.metricIndex) {
            0 -> tr(R.string.trend_demands_are_decreasing)
            1 -> tr(R.string.trend_tension_is_easing)
            2 -> tr(R.string.trend_energy_is_increasing)
            else -> tr(R.string.trend_room_to_act_is_growing)
        }
    } else {
        when (trend.metricIndex) {
            0 -> tr(R.string.trend_demands_are_increasing)
            1 -> tr(R.string.trend_tension_is_increasing)
            2 -> tr(R.string.trend_energy_is_decreasing)
            else -> tr(R.string.trend_room_to_act_is_shrinking)
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall
        )

        Text(
            text = tr(R.string.trend_weekly_means) +
                    trend.weeklyMeans.joinToString(" → ") {
                        trendNumber(it)
                    },
            style = MaterialTheme.typography.bodySmall
        )
    }
}

private fun negativeTrendMessage(
    trends: List<MetricTrend>
): String {
    return when {
        trends.size >= 3 ->
            tr(R.string.trend_several_areas_are_moving_in_a_more_demanding)

        trends.size == 2 ->
            tr(R.string.trend_two_areas_could_use_a_little_attention_take)

        else -> when (trends.single().metricIndex) {
            0 ->
                tr(R.string.trend_perhaps_a_task_could_be_made_smaller_postponed)

            1 ->
                tr(R.string.trend_perhaps_there_is_one_small_place_where_you)

            2 ->
                tr(R.string.trend_recovery_deserves_a_place_in_your_plans_perhaps)

            else ->
                tr(R.string.trend_when_your_room_to_act_shrinks_it_can)
        }
    }
}

private fun positiveTrendMessage(
    trends: List<MetricTrend>
): String {
    if (trends.size > 1) {
        return tr(R.string.trend_your_entries_show_encouraging_changes_in_several_areas)
    }

    return when (trends.single().metricIndex) {
        0 ->
            tr(R.string.trend_fewer_demands_can_create_space_it_is_encouraging)

        1 ->
            tr(R.string.trend_it_is_good_to_see_your_tension_easing)

        2 ->
            tr(R.string.trend_your_energy_is_increasing_an_encouraging_development_you)

        else ->
            tr(R.string.trend_more_room_to_act_is_emerging_perhaps_you)
    }
}

