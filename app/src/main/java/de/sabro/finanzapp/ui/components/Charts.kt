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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.sabro.finanzapp.domain.SavingsPoint
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
 * Sparuebersicht in zwei uebereinanderliegenden Feldern mit gemeinsamer
 * Monatsachse: oben die Sparrate je Monat, darunter der kumulierte Stand.
 *
 * Bewusst nicht in einem Feld: die Sparrate bewegt sich in Hundertern, der
 * Gesamtstand in Tausendern – auf einer gemeinsamen Skala waeren die Balken
 * nicht mehr ablesbar, zwei Achsen in einem Feld waeren irrefuehrend.
 */
@Composable
fun SavingsChart(
    monthlyNet: List<Long>,
    balancePoints: List<SavingsPoint>,
    barColor: Color,
    negativeBarColor: Color,
    lineColor: Color,
    modifier: Modifier = Modifier,
    height: Dp = 240.dp
) {
    val measurer = rememberTextMeasurer()
    val axisColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelStyle = TextStyle(fontSize = 9.sp, color = labelColor)
    val captionStyle = TextStyle(fontSize = 9.sp, color = labelColor, fontWeight = FontWeight.SemiBold)
    val areaColor = lineColor.copy(alpha = 0.12f)
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerLow

    Canvas(
        modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val padL = 42.dp.toPx()
        val padT = 26.dp.toPx()
        val labelsH = 16.dp.toPx()
        val gapH = 30.dp.toPx()

        val fieldH = (size.height - padT - labelsH - gapH) / 2f
        val plotW = size.width - padL
        if (fieldH <= 0f || plotW <= 0f) return@Canvas

        val topOfRate = padT
        val topOfBalance = padT + fieldH + gapH

        // ---- Feld 1: Sparrate je Monat
        val nMax = maxOf(monthlyNet.maxOrNull() ?: 0L, 0L)
        val nMin = minOf(monthlyNet.minOrNull() ?: 0L, 0L)
        val nSpan = (nMax - nMin).coerceAtLeast(1L).toFloat()
        fun yRate(v: Long): Float = topOfRate + fieldH - ((v - nMin) / nSpan) * fieldH
        val zeroY = yRate(0L)

        // ---- Feld 2: kumulierter Gesamtstand
        val balances = balancePoints.map { it.balanceCents }
        val bMax = maxOf(balances.maxOrNull() ?: 0L, 0L)
        val bMin = minOf(balances.minOrNull() ?: 0L, 0L)
        val bSpan = (bMax - bMin).coerceAtLeast(1L).toFloat()
        fun yBal(v: Long): Float = topOfBalance + fieldH - ((v - bMin) / bSpan) * fieldH

        val slotWidth = plotW / 12f
        val barWidth = (slotWidth * 0.46f).coerceAtMost(14.dp.toPx())
        val radius = androidx.compose.ui.geometry.CornerRadius(barWidth / 4f, barWidth / 4f)

        // Feld 1 zeichnen
        drawText(textMeasurer = measurer, text = "SPARRATE", style = captionStyle, topLeft = Offset(0f, topOfRate - 15.dp.toPx()))
        drawText(textMeasurer = measurer, text = formatMoneyCompact(nMax), style = labelStyle, topLeft = Offset(0f, yRate(nMax) - 5.dp.toPx()))
        if (nMin < 0L) {
            drawText(textMeasurer = measurer, text = formatMoneyCompact(nMin), style = labelStyle, topLeft = Offset(0f, yRate(nMin) - 5.dp.toPx()))
        }
        drawLine(axisColor, Offset(padL, zeroY), Offset(size.width, zeroY), strokeWidth = 1.dp.toPx())

        for (i in 0 until 12) {
            val centerX = padL + slotWidth * i + slotWidth / 2f
            val value = monthlyNet.getOrElse(i) { 0L }
            if (value == 0L) continue
            val valueY = yRate(value)
            drawRoundRect(
                color = if (value > 0) barColor else negativeBarColor,
                topLeft = Offset(centerX - barWidth / 2f, minOf(valueY, zeroY)),
                size = Size(barWidth, abs(valueY - zeroY)),
                cornerRadius = radius
            )
        }

        // Feld 2 zeichnen
        drawText(textMeasurer = measurer, text = "GESAMTSTAND", style = captionStyle, topLeft = Offset(0f, topOfBalance - 15.dp.toPx()))
        drawText(textMeasurer = measurer, text = formatMoneyCompact(bMax), style = labelStyle, topLeft = Offset(0f, yBal(bMax) - 5.dp.toPx()))
        drawLine(axisColor, Offset(padL, yBal(bMax)), Offset(size.width, yBal(bMax)), strokeWidth = 1.dp.toPx())
        drawLine(axisColor, Offset(padL, yBal(bMin)), Offset(size.width, yBal(bMin)), strokeWidth = 1.dp.toPx())

        if (balancePoints.isNotEmpty()) {
            val points = balancePoints.mapIndexed { i, p ->
                Offset(padL + slotWidth * i + slotWidth / 2f, yBal(p.balanceCents))
            }
            // Erfasste Monate durchgezogen, hochgerechnete gestrichelt.
            val lastActual = balancePoints.indexOfLast { !it.forecast }.coerceAtLeast(0)
            val actual = points.take(lastActual + 1)
            val projected = if (balancePoints.any { it.forecast }) points.drop(lastActual) else emptyList()

            if (actual.size > 1) {
                val area = Path().apply {
                    moveTo(actual.first().x, yBal(bMin))
                    actual.forEach { lineTo(it.x, it.y) }
                    lineTo(actual.last().x, yBal(bMin))
                    close()
                }
                drawPath(area, areaColor)

                val line = Path().apply {
                    actual.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }
                }
                drawPath(line, lineColor, style = Stroke(width = 2.dp.toPx()))
            }

            if (projected.size > 1) {
                val dashed = Path().apply {
                    projected.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }
                }
                drawPath(
                    path = dashed,
                    color = lineColor,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(4.dp.toPx(), 3.dp.toPx())
                        )
                    )
                )
            }

            points.forEachIndexed { i, p ->
                if (balancePoints[i].forecast) {
                    drawCircle(surfaceColor, radius = 2.6.dp.toPx(), center = p)
                    drawCircle(lineColor, radius = 2.6.dp.toPx(), center = p, style = Stroke(width = 1.6.dp.toPx()))
                } else {
                    drawCircle(lineColor, radius = 3.dp.toPx(), center = p)
                }
            }
        }

        // Gemeinsame Monatsachse
        for (i in 0 until 12 step 2) {
            val centerX = padL + slotWidth * i + slotWidth / 2f
            drawCenteredText(measurer, Period.MONTH_SHORT[i], labelStyle, centerX, size.height - labelsH + 2.dp.toPx())
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
