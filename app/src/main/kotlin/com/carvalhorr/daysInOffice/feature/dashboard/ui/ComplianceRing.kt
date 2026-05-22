package com.carvalhorr.daysInOffice.feature.dashboard.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

internal val colorOfficeGreen = Color(0xFF2E7D32)
internal val colorAmber = Color(0xFFF57F17)
internal val colorRed = Color(0xFFD32F2F)
private val colorTrack = Color(0xFFEDE7F6)

fun complianceRingColor(currentPercentage: Float, targetPercentage: Float): Color = when {
    currentPercentage >= targetPercentage -> colorOfficeGreen
    currentPercentage >= targetPercentage - 0.10f -> colorAmber
    else -> colorRed
}

@Composable
fun ComplianceRing(
    currentPercentage: Float,
    targetPercentage: Float,
    modifier: Modifier = Modifier
) {
    val ringColor = complianceRingColor(currentPercentage, targetPercentage)
    val sweepAngle = (currentPercentage * 360f).coerceIn(0f, 360f)

    Box(
        modifier = modifier.size(190.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 18.dp.toPx()
            val arcDiameter = size.minDimension - strokeWidth
            val arcTopLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
            val arcSize = Size(arcDiameter, arcDiameter)

            drawArc(
                color = colorTrack,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth)
            )

            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${(currentPercentage * 100).roundToInt()}%",
                style = MaterialTheme.typography.headlineLarge,
                color = ringColor
            )
            Text(
                text = "this period",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
