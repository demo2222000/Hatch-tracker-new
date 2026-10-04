package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.data.model.SensorReading

@Composable
fun MiniTrendChart(
    readings: List<SensorReading>,
    lineColor: Color,
    fillColor: Color,
    valueExtractor: (SensorReading) -> Double,
    minTarget: Double,
    maxTarget: Double,
    modifier: Modifier = Modifier.height(60.dp)
) {
    if (readings.isEmpty()) return

    val values = readings.takeLast(30).map(valueExtractor)
    if (values.size < 2) return

    val minVal = (values.minOrNull() ?: minTarget).coerceAtMost(minTarget - 0.5)
    val maxVal = (values.maxOrNull() ?: maxTarget).coerceAtLeast(maxTarget + 0.5)
    val range = (maxVal - minVal).coerceAtLeast(0.1)

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val stepX = width / (values.size - 1)

        val strokePath = Path()
        val fillPath = Path()

        val points = values.mapIndexed { index, value ->
            val x = index * stepX
            val normalized = ((value - minVal) / range).toFloat().coerceIn(0f, 1f)
            val y = height - (normalized * (height - 12f)) - 6f
            Offset(x, y)
        }

        strokePath.moveTo(points.first().x, points.first().y)
        fillPath.moveTo(points.first().x, height)
        fillPath.lineTo(points.first().x, points.first().y)

        for (i in 0 until points.size - 1) {
            val p0 = points[i]
            val p1 = points[i + 1]
            val cx = (p0.x + p1.x) / 2
            strokePath.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
            fillPath.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
        }

        fillPath.lineTo(points.last().x, height)
        fillPath.close()

        // Draw gradient fill under curve
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(fillColor.copy(alpha = 0.35f), Color.Transparent),
                startY = 0f,
                endY = height
            )
        )

        // Draw stroke line
        drawPath(
            path = strokePath,
            color = lineColor,
            style = Stroke(width = 2.5f, cap = StrokeCap.Round)
        )

        // Draw latest point dot
        val lastPoint = points.last()
        drawCircle(
            color = lineColor,
            radius = 3.5f,
            center = lastPoint
        )
    }
}
