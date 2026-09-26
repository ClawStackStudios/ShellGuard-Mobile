package com.clawstack.shellguard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clawstack.shellguard.domain.models.CustomFieldType
import com.clawstack.shellguard.ui.theme.LocalShellGuardColors

@Composable
fun CustomFieldDisplayRow(
    label: String,
    value: String,
    type: CustomFieldType,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalShellGuardColors.current
    var isRevealed by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.bgElevated)
            .border(1.dp, colors.borderSubtle, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label.uppercase(),
                color = colors.textMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            when (type) {
                CustomFieldType.TEXT -> {
                    Text(text = value, color = colors.textMain, fontSize = 14.sp)
                }
                CustomFieldType.HIDDEN -> {
                    Text(
                        text = if (isRevealed) value else "••••••••••••",
                        color = colors.textMain,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    )
                }
                CustomFieldType.BOOLEAN -> {
                    val isChecked = value.toBooleanStrictOrNull() ?: false
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isChecked) colors.success.copy(alpha = 0.2f) else colors.bgBase)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isChecked) "☑ Enabled" else "☐ Disabled",
                            color = if (isChecked) colors.success else colors.textMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                CustomFieldType.LINKED -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "→ $value",
                            color = colors.secondaryAccent,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Action Icons
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (type == CustomFieldType.HIDDEN) {
                IconButton(onClick = { isRevealed = !isRevealed }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = if (isRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle Visibility",
                        tint = colors.textMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy Field",
                    tint = colors.textMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
