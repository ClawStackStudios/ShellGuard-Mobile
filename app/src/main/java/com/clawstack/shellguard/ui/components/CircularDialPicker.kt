package com.clawstack.shellguard.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clawstack.shellguard.ui.theme.BorderSubtle
import com.clawstack.shellguard.ui.theme.ReefPink
import com.clawstack.shellguard.ui.theme.StatusError
import com.clawstack.shellguard.ui.theme.SurfaceDark
import com.clawstack.shellguard.ui.theme.TextMuted
import com.clawstack.shellguard.ui.theme.TextPrimary
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Clock-face circular dial time picker for configuring the panic purge countdown.
 * Clamps duration between [minSeconds] (default 5s) and [maxSeconds] (default 60s).
 */
@Composable
fun CircularDialPicker(
    currentSeconds: Int,
    onSecondsChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    dialSize: Dp = 190.dp,
    minSeconds: Int = 5,
    maxSeconds: Int = 60
) {
    val clampedSeconds = currentSeconds.coerceIn(minSeconds, maxSeconds)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(dialSize),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .size(dialSize)
                    .pointerInput(minSeconds, maxSeconds) {
                        fun updateFromOffset(offset: Offset, size: androidx.compose.ui.unit.IntSize) {
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val dx = offset.x - center.x
                            val dy = offset.y - center.y
                            // Angle in radians: -PI to +PI
                            val rad = atan2(dy, dx)
                            // Convert to degrees with 12 o'clock as 0 deg
                            var deg = Math.toDegrees(rad.toDouble()) + 90.0
                            if (deg < 0) deg += 360.0
                            // Map 0..360 deg to 0..60 seconds
                            var seconds = ((deg / 360.0) * 60.0).roundToInt()
                            if (seconds <= 0) seconds = 60
                            val clamped = seconds.coerceIn(minSeconds, maxSeconds)
                            onSecondsChanged(clamped)
                        }

                        detectTapGestures { offset ->
                            updateFromOffset(offset, size)
                        }
                    }
                    .pointerInput(minSeconds, maxSeconds) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val dx = change.position.x - center.x
                            val dy = change.position.y - center.y
                            val rad = atan2(dy, dx)
                            var deg = Math.toDegrees(rad.toDouble()) + 90.0
                            if (deg < 0) deg += 360.0
                            var seconds = ((deg / 360.0) * 60.0).roundToInt()
                            if (seconds <= 0) seconds = 60
                            val clamped = seconds.coerceIn(minSeconds, maxSeconds)
                            onSecondsChanged(clamped)
                        }
                    }
            ) {
                val strokeWidth = 8.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2f
                val center = Offset(size.width / 2f, size.height / 2f)

                // 1. Inactive background track
                drawCircle(
                    color = BorderSubtle.copy(alpha = 0.5f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = strokeWidth)
                )

                // 2. Clock-face tick marks (12 ticks for 5s intervals)
                for (i in 1..12) {
                    val tickAngle = (i * 30.0 - 90.0) * (PI / 180.0)
                    val isMajor = (i % 3 == 0) // 15s, 30s, 45s, 60s
                    val tickInnerRadius = if (isMajor) radius - 14.dp.toPx() else radius - 8.dp.toPx()
                    val tickOuterRadius = radius - 3.dp.toPx()

                    val startX = center.x + tickInnerRadius * cos(tickAngle).toFloat()
                    val startY = center.y + tickInnerRadius * sin(tickAngle).toFloat()
                    val endX = center.x + tickOuterRadius * cos(tickAngle).toFloat()
                    val endY = center.y + tickOuterRadius * sin(tickAngle).toFloat()

                    drawLine(
                        color = if (isMajor) TextPrimary.copy(alpha = 0.6f) else BorderSubtle,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = if (isMajor) 2.5.dp.toPx() else 1.5.dp.toPx()
                    )
                }

                // 3. Active sweep arc (starting at 12 o'clock = -90 deg)
                val sweepFraction = (clampedSeconds / 60f).coerceIn(0f, 1f)
                val sweepAngle = sweepFraction * 360f

                drawArc(
                    color = StatusError,
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // 4. Draggable thumb indicator at the tip of the sweep
                val thumbAngleRad = (sweepAngle - 90f) * (PI.toFloat() / 180f)
                val thumbCenter = Offset(
                    x = center.x + radius * cos(thumbAngleRad),
                    y = center.y + radius * sin(thumbAngleRad)
                )

                // Outer glow ring
                drawCircle(
                    color = StatusError.copy(alpha = 0.3f),
                    radius = 12.dp.toPx(),
                    center = thumbCenter
                )
                // Solid thumb circle
                drawCircle(
                    color = Color.White,
                    radius = 6.dp.toPx(),
                    center = thumbCenter
                )
                drawCircle(
                    color = StatusError,
                    radius = 6.dp.toPx(),
                    center = thumbCenter,
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // Center readout display
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${clampedSeconds}s",
                    color = TextPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "PURGE TIMER",
                    color = StatusError,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Quick Stepper Buttons (-5s, Reset 15s, +5s)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    val next = (clampedSeconds - 5).coerceIn(minSeconds, maxSeconds)
                    onSecondsChanged(next)
                },
                enabled = clampedSeconds > minSeconds,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(SurfaceDark)
                    .border(1.dp, BorderSubtle, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Decrease 5 seconds",
                    tint = if (clampedSeconds > minSeconds) TextPrimary else TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceDark)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Default: 15s",
                    color = if (clampedSeconds == 15) StatusError else TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            IconButton(
                onClick = {
                    val next = (clampedSeconds + 5).coerceIn(minSeconds, maxSeconds)
                    onSecondsChanged(next)
                },
                enabled = clampedSeconds < maxSeconds,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(SurfaceDark)
                    .border(1.dp, BorderSubtle, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increase 5 seconds",
                    tint = if (clampedSeconds < maxSeconds) TextPrimary else TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
