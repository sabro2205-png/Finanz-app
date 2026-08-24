package de.sabro.finanzapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.sabro.finanzapp.util.Period
import de.sabro.finanzapp.util.formatMoneyCompact
import kotlin.math.abs
import kotlin.math.max

/**
 * Zwei Balken pro Monat (z. B. Einnahmen vs. Ausgaben) fuer ein ganzes Jahr.
 * [seriesA] und [seriesB] enthalten je 12 Werte in Cent.
 */
@Composable
fun GroupedYearBarChart(
    seriesA: List<Long>,
    seriesB: List<Long>,
    colorA: Color,
    colorB: Color,
    modifier: Modifier = Modifier,
    height: Dp = 180.dp
) {
    val measurer = rememberTextMeasurer()
    val axisColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelStyle = TextStyle(fontSize = 9.sp, color = labelColor)

    Canvas(
        modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val bottomAxis = 16.dp.toPx()
        val topPadding = 6.dp.toPx()
        val chartHeight = size.height - bottomAxis - topPadding
        if (chartHeight <= 0f) return@Canvas

        val maxValue = max(
            seriesA.maxOfOrNull { it } ?: 0L,
            seriesB.maxOfOrNull { it } ?: 0L
        ).coerceAtLeast(1L)

        // Grundlinie
        val baseY = topPadding + chartHeight
        drawLine(
            color = axisColor,
            start = Offset(0f, baseY),
            end = Offset(size.width, baseY),
            strokeWidth = 1.dp.toPx()
        )

        val slotWidth = size.width / 12f
        val barWidth = (slotWidth * 0.30f).coerceAtMost(14.dp.toPx())
        val gap = 2.dp.toPx()
        val radius = androidx.compose.ui.geometry.CornerRadius(barWidth / 3f, barWidth / 3f)

        for (i in 0 until 12) {
            val centerX = slotWidth * i + slotWidth / 2f
            val aHeight = (seriesA.getOrElse(i) { 0L }.toFloat() / maxValue) * chartHeight
            val bHeight = (seriesB.getOrElse(i) { 0L }.toFloat() / maxValue) * chartHeight

            drawRoundRect(
                color = colorA,
                topLeft = Offset(centerX - barWidth - gap / 2f, baseY - aHeight),
                size = Size(barWidth, aHeight.coerceAtLeast(0f)),
                cornerRadius = radius
            )
            drawRoundRect(
                color = colorB,
                topLeft = Offset(centerX + gap / 2f, baseY - bHeight),
                size = Size(barWidth, bHeight.coerceAtLeast(0f)),
                cornerRadius = radius
            )

            // Monatskuerzel nur jeden zweiten Monat, damit nichts ueberlappt
            if (i % 2 == 0) {
                drawCenteredText(measurer, Period.MONTH_SHORT[i], labelStyle, centerX, baseY + 3.dp.toPx())
            }
        }
    }
}

/**
 * Sparuebersicht: Balken je Monat (positiv/negativ moeglich) plus die
 * kumulierte Entwicklung als Linie.
 */
@Composable
fun SavingsChart(
    monthlyNet: List<Long>,
    runningBalance: List<Long>,
    barColor: Color,
    negativeBarColor: Color,
    lineColor: Color,
    modifier: Modifier = Modifier,
    height: Dp = 200.dp
) {
    val measurer = rememberTextMeasurer()
    val axisColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelStyle = TextStyle(fontSize = 9.sp, color = labelColor)

    Canvas(
        modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val bottomAxis = 16.dp.toPx()
        val topPadding = 14.dp.toPx()
        val leftPadding = 34.dp.toPx()
        val chartHeight = size.height - bottomAxis - topPadding
        val chartWidth = size.width - leftPadding
        if (chartHeight <= 0f || chartWidth <= 0f) return@Canvas

        // Gemeinsame Skala fuer Balken und Linie
        val maxValue = maxOf(
            monthlyNet.maxOfOrNull { it } ?: 0L,
            runningBalance.maxOfOrNull { it } ?: 0L,
            0L
        )
        val minValue = minOf(
            monthlyNet.minOfOrNull { it } ?: 0L,
            runningBalance.minOfOrNull { it } ?: 0L,
            0L
        )
        val span = (maxValue - minValue).coerceAtLeast(1L).toFloat()

        fun yOf(value: Long): Float =
            topPadding + chartHeight - ((value - minValue) / span) * chartHeight

        val zeroY = yOf(0L)

        // Nulllinie + Beschriftung der Extremwerte
        drawLine(
            color = axisColor,
            start = Offset(leftPadding, zeroY),
            end = Offset(size.width, zeroY),
            strokeWidth = 1.dp.toPx()
        )
        drawText(
            textMeasurer = measurer,
            text = formatMoneyCompact(maxValue),
            style = labelStyle,
            topLeft = Offset(0f, topPadding - 4.dp.toPx())
        )
        if (minValue < 0L) {
            drawText(
                textMeasurer = measurer,
                text = formatMoneyCompact(minValue),
                style = labelStyle,
                topLeft = Offset(0f, topPadding + chartHeight - 8.dp.toPx())
            )
        }

        val slotWidth = chartWidth / 12f
        val barWidth = (slotWidth * 0.45f).coerceAtMost(20.dp.toPx())
        val radius = androidx.compose.ui.geometry.CornerRadius(barWidth / 4f, barWidth / 4f)

        for (i in 0 until 12) {
            val centerX = leftPadding + slotWidth * i + slotWidth / 2f
            val value = monthlyNet.getOrElse(i) { 0L }
            val valueY = yOf(value)
            val top = minOf(valueY, zeroY)
            val barHeight = abs(valueY - zeroY)

            if (value != 0L) {
                drawRoundRect(
                    color = if (value >= 0) barColor else negativeBarColor,
                    topLeft = Offset(centerX - barWidth / 2f, top),
                    size = Size(barWidth, barHeight),
                    cornerRadius = radius
                )
            }

            if (i % 2 == 0) {
                drawCenteredText(measurer, Period.MONTH_SHORT[i], labelStyle, centerX, topPadding + chartHeight + 3.dp.toPx())
            }
        }

        // Kumulierte Entwicklung
        if (runningBalance.isNotEmpty()) {
            val path = Path()
            runningBalance.forEachIndexed { i, value ->
                val x = leftPadding + slotWidth * i + slotWidth / 2f
                val y = yOf(value)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 2.dp.toPx())
            )
            runningBalance.forEachIndexed { i, value ->
                val x = leftPadding + slotWidth * i + slotWidth / 2f
                drawCircle(lineColor, radius = 2.5f.dp.toPx(), center = Offset(x, yOf(value)))
            }
        }
    }
}

private fun DrawScope.drawCenteredText(
    measurer: TextMeasurer,
    text: String,
    style: TextStyle,
    centerX: Float,
    top: Float
) {
    val layout = measurer.measure(text, style)
    drawText(
        textLayoutResult = layout,
        topLeft = Offset(centerX - layout.size.width / 2f, top)
    )
}
