package com.example.ui.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.example.data.model.SensorReading
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DetailedTrendChart(
    readings: List<SensorReading>,
    title: String,
    unit: String,
    lineColor: Color,
    fillColor: Color,
    targetMin: Double,
    targetMax: Double,
    valueExtractor: (SensorReading) -> Double,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(220.dp)
) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    val textNativeColor = android.graphics.Color.rgb(100, 116, 139)

    Box(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                RoundedCornerShape(16.dp)
            )
            .padding(12.dp)
    ) {
        if (readings.size < 2) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Empty state or not enough points yet
            }
            return@Box
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            val leftPadding = 50f
            val rightPadding = 20f
            val topPadding = 20f
            val bottomPadding = 30f

            val chartWidth = width - leftPadding - rightPadding
            val chartHeight = height - topPadding - bottomPadding

            val values = readings.map(valueExtractor)
            val minReading = values.minOrNull() ?: targetMin
            val maxReading = values.maxOrNull() ?: targetMax

            val chartMin = (minReading.coerceAtMost(targetMin) - 0.5)
            val chartMax = (maxReading.coerceAtLeast(targetMax) + 0.5)
            val range = (chartMax - chartMin).coerceAtLeast(0.5)

            fun getY(value: Double): Float {
                val normalized = ((value - chartMin) / range).toFloat()
                return (topPadding + chartHeight - (normalized * chartHeight)).coerceIn(topPadding, topPadding + chartHeight)
            }

            // Draw target safe band
            val bandTop = getY(targetMax)
            val bandBottom = getY(targetMin)
            drawRect(
                color = Color(0xFF10B981).copy(alpha = 0.12f),
                topLeft = Offset(leftPadding, bandTop),
                size = Size(chartWidth, (bandBottom - bandTop).coerceAtLeast(4f))
            )

            // Draw grid lines (3 lines)
            val steps = 3
            for (i in 0..steps) {
                val stepVal = chartMin + (range * i / steps)
                val y = getY(stepVal)
                drawLine(
                    color = gridColor,
                    start = Offset(leftPadding, y),
                    end = Offset(width - rightPadding, y),
                    strokeWidth = 1f
                )

                // Draw Y axis label
                drawIntoCanvas { canvas ->
                    val paint = Paint().apply {
                        color = textNativeColor
                        textSize = 24f
                        isAntiAlias = true
                    }
                    val label = String.format(Locale.US, "%.1f", stepVal)
                    canvas.nativeCanvas.drawText(label, 4f, y + 8f, paint)
                }
            }

            // Draw Curve
            val stepX = chartWidth / (readings.size - 1)
            val points = readings.mapIndexed { index, reading ->
                val x = leftPadding + (index * stepX)
                val y = getY(valueExtractor(reading))
                Offset(x, y)
            }

            val strokePath = Path()
            val fillPath = Path()

            strokePath.moveTo(points.first().x, points.first().y)
            fillPath.moveTo(points.first().x, topPadding + chartHeight)
            fillPath.lineTo(points.first().x, points.first().y)

            for (i in 0 until points.size - 1) {
                val p0 = points[i]
                val p1 = points[i + 1]
                val cx = (p0.x + p1.x) / 2
                strokePath.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                fillPath.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
            }

            fillPath.lineTo(points.last().x, topPadding + chartHeight)
            fillPath.close()

            // Fill
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(fillColor.copy(alpha = 0.35f), Color.Transparent),
                    startY = topPadding,
                    endY = topPadding + chartHeight
                )
            )

            // Stroke
            drawPath(
                path = strokePath,
                color = lineColor,
                style = Stroke(width = 3f, cap = StrokeCap.Round)
            )

            // Draw first and last time label on X axis
            drawIntoCanvas { canvas ->
                val paint = Paint().apply {
                    color = textNativeColor
                    textSize = 22f
                    isAntiAlias = true
                }
                val firstTime = timeFormat.format(Date(readings.first().timestamp))
                val lastTime = timeFormat.format(Date(readings.last().timestamp))
                canvas.nativeCanvas.drawText(firstTime, leftPadding, height - 6f, paint)
                canvas.nativeCanvas.drawText(lastTime, width - rightPadding - 60f, height - 6f, paint)
            }

            // Draw current point pulse
            val currentPoint = points.last()
            drawCircle(color = lineColor, radius = 5f, center = currentPoint)
            drawCircle(color = Color.White, radius = 2f, center = currentPoint)
        }
    }
}
