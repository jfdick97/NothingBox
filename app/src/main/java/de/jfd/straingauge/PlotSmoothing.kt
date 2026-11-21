package de.jfd.straingauge

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp
import java.time.LocalDate

/** Draw each consecutive run independently, so neither line nor band bridges gaps. */
fun DrawScope.drawSmoothedSeries(
    points: List<SmoothedPoint>,
    start: LocalDate,
    end: LocalDate,
    color: Color
) {
    val startDay = start.toEpochDay().toDouble()
    val span = (end.toEpochDay() - start.toEpochDay()).coerceAtLeast(1L).toDouble()
    fun position(day: Double, value: Double) = Offset(
        ((day - startDay) / span * size.width).toFloat(),
        ((1.0 - value / 10.0) * size.height).toFloat()
    )
    val segments = mutableListOf<MutableList<SmoothedPoint>>()
    points.forEach { point ->
        val previous = segments.lastOrNull()?.lastOrNull()
        if (previous == null || point.epochDay - previous.epochDay > 1.000001) {
            segments.add(mutableListOf(point))
        } else segments.last().add(point)
    }
    clipRect {
        segments.forEach { segment ->
            if (segment.size >= 2) {
                val band = Path()
                segment.forEachIndexed { index, point ->
                    val upper = position(point.epochDay, point.mean + point.deviation)
                    if (index == 0) band.moveTo(upper.x, upper.y)
                    else band.lineTo(upper.x, upper.y)
                }
                segment.asReversed().forEach { point ->
                    val lower = position(point.epochDay, point.mean - point.deviation)
                    band.lineTo(lower.x, lower.y)
                }
                band.close()
                drawPath(band, color.copy(alpha = 0.16f))
            }
            segment.zipWithNext().forEach { (before, after) ->
                drawLine(color, position(before.epochDay, before.mean),
                    position(after.epochDay, after.mean), 3.dp.toPx())
            }
            if (segment.size == 1) {
                val point = segment.single()
                drawLine(color.copy(alpha = 0.3f),
                    position(point.epochDay, point.mean - point.deviation),
                    position(point.epochDay, point.mean + point.deviation), 2.dp.toPx())
                drawCircle(color, 3.dp.toPx(), position(point.epochDay, point.mean))
            }
        }
    }
}
