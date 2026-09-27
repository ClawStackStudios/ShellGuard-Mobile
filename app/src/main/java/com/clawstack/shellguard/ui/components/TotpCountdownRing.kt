package com.clawstack.shellguard.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clawstack.shellguard.ui.theme.BorderSubtle
import com.clawstack.shellguard.ui.theme.BrandClawCyan
import com.clawstack.shellguard.ui.theme.StatusError
import com.clawstack.shellguard.ui.theme.WarningText

@Composable
fun TotpCountdownRing(
    progress: Float,
    remainingSeconds: Int,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    strokeWidth: Dp = 3.dp
) {
    val ringColor by animateColorAsState(
        targetValue = when {
            remainingSeconds > 10 -> BrandClawCyan
            remainingSeconds > 5 -> WarningText
            else -> StatusError
        },
        label = "totp_ring_color"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()
            val diameter = size.toPx() - strokePx
            val topLeft = Offset(strokePx / 2f, strokePx / 2f)
            val arcSize = Size(diameter, diameter)

            // Background circle track
            drawArc(
                color = BorderSubtle,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx)
            )

            // Depleting countdown arc
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }

        Text(
            text = "$remainingSeconds",
            color = ringColor,
            fontSize = (size.value * 0.35f).sp,
            fontWeight = FontWeight.Bold
        )
    }
}
