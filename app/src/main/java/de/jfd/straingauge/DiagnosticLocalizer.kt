package de.jfd.straingauge

import android.content.res.Configuration
import java.util.Locale

/** Translates recognised diagnostic snapshots at display time without rewriting
 * backups or user-entered factor names. Unknown/third-party notes stay verbatim. */
internal object DiagnosticLocalizer {
    private data class Pattern(
        val resource: Int,
        val regex: Regex,
        val argumentPositions: List<Int>,
        val labels: Map<String, Int>
    )

    private val placeholder = Regex("""%(\d+)\${'$'}s""")

    private val diagnosticResources = listOf(
        R.string.scan_please_check_stress_factor,
        R.string.scan_more_than_three_stress_factors,
        R.string.scan_unclear_mark_please_check,
        R.string.scan_scale_field_could_not_be_recognised_reliably,
        R.string.scan_mark_is_clipped_at_the_search_boundary_please,
        R.string.scan_multiple_dots_please_choose_one,
        R.string.scan_weak_or_unclear_mark_please_check,
        R.string.scan_legacy_frame_warning
    )

    private val labelResources = listOf(
        R.string.metric_demands, R.string.metric_tension,
        R.string.metric_energy, R.string.metric_agency,
        R.string.day_mon, R.string.day_tue, R.string.day_wed,
        R.string.day_thu, R.string.day_fri, R.string.day_sat,
        R.string.day_sun
    )

    private val patterns: List<Pattern> by lazy {
        val context = checkNotNull(AppStrings.context)
        listOf(Locale.GERMAN, Locale.ENGLISH).flatMap { locale ->
            val configuration = Configuration(context.resources.configuration)
            configuration.setLocale(locale)
            val source = context.createConfigurationContext(configuration)
            val labels = labelResources.associateBy { source.getString(it) }
            diagnosticResources.map { id ->
                val template = source.getString(id)
                val positions = mutableListOf<Int>()
                val expression = StringBuilder("^")
                var offset = 0
                placeholder.findAll(template).forEach { match ->
                    expression.append(Regex.escape(template.substring(offset, match.range.first)))
                    expression.append("(.*?)")
                    positions += match.groupValues[1].toInt() - 1
                    offset = match.range.last + 1
                }
                expression.append(Regex.escape(template.substring(offset)))
                expression.append("$")
                Pattern(id, Regex(expression.toString(), RegexOption.DOT_MATCHES_ALL), positions, labels)
            }
        }
    }

    fun display(message: String): String {
        if (AppStrings.context == null) return message
        patterns.forEach { pattern ->
            val match = pattern.regex.matchEntire(message) ?: return@forEach
            val count = (pattern.argumentPositions.maxOrNull() ?: -1) + 1
            val args = arrayOfNulls<Any>(count)
            pattern.argumentPositions.forEachIndexed { index, position ->
                val value = match.groupValues[index + 1]
                args[position] = pattern.labels[value]?.let { tr(it) } ?: value
            }
            return tr(pattern.resource, *args)
        }
        return message
    }
}
