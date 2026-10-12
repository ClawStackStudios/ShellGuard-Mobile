package com.clawstack.shellguard.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clawstack.shellguard.ui.theme.BorderMedium
import com.clawstack.shellguard.ui.theme.BorderSubtle
import com.clawstack.shellguard.ui.theme.BrandClawCyan
import com.clawstack.shellguard.ui.theme.ReefPink
import com.clawstack.shellguard.ui.theme.StatusError
import com.clawstack.shellguard.ui.theme.SurfaceContainerDark
import com.clawstack.shellguard.ui.theme.SurfaceDark
import com.clawstack.shellguard.ui.theme.TextMuted
import com.clawstack.shellguard.ui.theme.TextPrimary
import kotlin.math.roundToInt

@Composable
fun PinPadView(
    currentPin: String,
    onPinChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    pinLength: Int = 6,
    isError: Boolean = false,
    errorMessage: String? = null,
    onPinComplete: (String) -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(isError) {
        if (isError) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 350
                    0f at 0
                    -20f at 50
                    20f at 100
                    -15f at 150
                    15f at 200
                    -8f at 250
                    8f at 300
                    0f at 350
                }
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Dot Visualizer Row ──────────────────────────────────────────────
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            for (i in 0 until pinLength) {
                val isFilled = i < currentPin.length
                val dotBorderColor = when {
                    isError -> StatusError
                    isFilled -> ReefPink
                    else -> BorderMedium
                }
                val dotBgColor = when {
                    isError -> StatusError.copy(alpha = 0.3f)
                    isFilled -> ReefPink
                    else -> Color.Transparent
                }

                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(dotBgColor)
                        .border(1.5.dp, dotBorderColor, CircleShape)
                )
            }
        }

        if (errorMessage != null && isError) {
            Text(
                text = errorMessage,
                color = StatusError,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
        } else {
            Spacer(modifier = Modifier.height(16.dp))
        }

        // ── Numeric Keypad Matrix ───────────────────────────────────────────
        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("C", "0", "DEL")
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            rows.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    row.forEach { key ->
                        when (key) {
                            "C" -> {
                                PinKeyButton(
                                    content = {
                                        Text(
                                            text = "C",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextMuted
                                        )
                                    },
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onPinChange("")
                                    }
                                )
                            }
                            "DEL" -> {
                                PinKeyButton(
                                    content = {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                                            contentDescription = "Delete",
                                            tint = TextMuted,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    },
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        if (currentPin.isNotEmpty()) {
                                            val updated = currentPin.dropLast(1)
                                            onPinChange(updated)
                                        }
                                    }
                                )
                            }
                            else -> {
                                PinKeyButton(
                                    content = {
                                        Text(
                                            text = key,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary
                                        )
                                    },
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        if (currentPin.length < pinLength) {
                                            val updated = currentPin + key
                                            onPinChange(updated)
                                            if (updated.length == pinLength) {
                                                onPinComplete(updated)
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PinKeyButton(
    content: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(62.dp),
        shape = CircleShape,
        color = SurfaceContainerDark,
        border = BorderStroke(1.dp, BorderSubtle),
        interactionSource = remember { MutableInteractionSource() }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(62.dp)
        ) {
            content()
        }
    }
}
