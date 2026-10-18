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

private val trendDateFormat =
    DateTimeFormatter.ofPattern("dd.MM.yyyy")

private fun trendNumber(value: Double): String =
    String.format(Locale.GERMANY, "%.1f", value)

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
            "Zeit, gut auf dich zu achten"

        negativeCount == 2 ->
            "Zwei Entwicklungen im Blick"

        negativeCount == 1 ->
            "Ein kleiner Hinweis für dich"

        positiveCount == 1 ->
            "Eine erfreuliche Entwicklung"

        positiveCount > 1 ->
            "Erfreuliche Entwicklungen"

        assessment.assessedMetrics == 0 ->
            "Deine Trends brauchen noch etwas Zeit"

        else ->
            "Deine Trends im Blick"
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
                    "Für eine Trendbewertung fehlen noch Daten. " +
                            "Benötigt werden je Messgröße mindestens " +
                            "vier Tageswerte in jeder der letzten drei " +
                            "abgeschlossenen Wochen."
                )
            } else {
                if (negativeCount > 0) {
                    Text(
                        text = "Ungünstige Entwicklungen: $negativeCount",
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
                                text = "Das entwickelt sich günstig",
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
                        "Bei den auswertbaren Messgrößen zeigt sich " +
                                "nach den festgelegten Regeln aktuell " +
                                "kein ausgeprägter Trend."
                    )

                    Text(
                        "Auch ein Verlauf ohne klare Richtung darf " +
                                "einfach so stehen bleiben. Vielleicht " +
                                "möchtest du kurz wahrnehmen, was dich " +
                                "im Alltag trägt und was dich fordert."
                    )
                }

                if (
                    assessment.assessedMetrics < metricNames.size
                ) {
                    Text(
                        text =
                            "${assessment.assessedMetrics} von " +
                                    "${metricNames.size} Messgrößen " +
                                    "sind auswertbar. Für die übrigen " +
                                    "fehlen ausreichend Tageswerte.",
                        style =
                            MaterialTheme.typography.bodySmall
                    )
                }

                Text(
                    text = "Bewertet werden Veränderungen über drei " +
                            "abgeschlossene Wochen. Die Hinweise " +
                            "beziehen sich auf deine erfassten Werte.",
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
            0 -> "Anforderungen gehen zurück"
            1 -> "Anspannung nimmt ab"
            2 -> "Energie nimmt zu"
            else -> "Handlungsspielraum wächst"
        }
    } else {
        when (trend.metricIndex) {
            0 -> "Anforderungen steigen"
            1 -> "Anspannung nimmt zu"
            2 -> "Energie nimmt ab"
            else -> "Handlungsspielraum wird kleiner"
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
            text = "Wochenmittel: " +
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
            "Mehrere Bereiche bewegen sich gerade in eine " +
                    "belastendere Richtung. Du musst nicht alles " +
                    "auf einmal lösen. Vielleicht ist jetzt ein guter " +
                    "Moment, etwas kleiner zu planen, eine Grenze " +
                    "zu setzen oder dir Unterstützung zu holen."

        trends.size == 2 ->
            "Zwei Bereiche verdienen gerade etwas Aufmerksamkeit. " +
                    "Schau in Ruhe, was zusammenhängen könnte und " +
                    "wo eine kleine Entlastung möglich wäre. " +
                    "Ein überschaubarer Schritt genügt für den Anfang."

        else -> when (trends.single().metricIndex) {
            0 ->
                "Vielleicht lässt sich eine Aufgabe verkleinern, " +
                        "verschieben oder gemeinsam tragen. " +
                        "Du musst steigende Anforderungen nicht " +
                        "automatisch allein auffangen."

            1 ->
                "Vielleicht kannst du heute an einer kleinen " +
                        "Stelle Druck herausnehmen. Eine Pause oder " +
                        "eine klarere Grenze darf Platz haben."

            2 ->
                "Deine Erholung darf mit auf dem Plan stehen. " +
                        "Vielleicht lässt sich für die kommende Woche " +
                        "etwas mehr Freiraum schaffen."

            else ->
                "Wenn der Spielraum kleiner wird, kann es helfen, " +
                        "feste Vorgaben und eigene Möglichkeiten " +
                        "auseinanderzuhalten. Vielleicht gibt es eine " +
                        "kleine Entscheidung, die wieder bei dir " +
                        "liegen darf."
        }
    }
}

private fun positiveTrendMessage(
    trends: List<MetricTrend>
): String {
    if (trends.size > 1) {
        return "Deine Einträge zeigen in mehreren Bereichen eine " +
                "günstige Entwicklung. Das darfst du wahrnehmen und " +
                "würdigen. Vielleicht möchtest du festhalten, " +
                "was dir gerade guttut."
    }

    return when (trends.single().metricIndex) {
        0 ->
            "Etwas weniger Anforderungen können Raum schaffen. " +
                    "Schön, dass sich hier Entlastung abzeichnet. " +
                    "Du darfst den frei werdenden Platz auch frei lassen."

        1 ->
            "Schön, dass deine Anspannung zurückgeht. " +
                    "Vielleicht erkennst du etwas, das dir dabei " +
                    "guttut und das du beibehalten möchtest."

        2 ->
            "Deine Energie nimmt zu – eine erfreuliche Entwicklung. " +
                    "Du darfst dich darüber freuen, ohne sie gleich " +
                    "wieder vollständig zu verplanen."

        else ->
            "Mehr Handlungsspielraum zeichnet sich ab. " +
                    "Vielleicht möchtest du eine dieser Möglichkeiten " +
                    "bewusst für dich nutzen."
    }
}

