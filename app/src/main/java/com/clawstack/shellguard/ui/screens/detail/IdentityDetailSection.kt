package com.clawstack.shellguard.ui.screens.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clawstack.shellguard.domain.models.IdentityDetail
import com.clawstack.shellguard.ui.theme.BorderSubtle
import com.clawstack.shellguard.ui.theme.ReefPink
import com.clawstack.shellguard.ui.theme.TextMuted
import com.clawstack.shellguard.ui.theme.TextPrimary
import com.clawstack.shellguard.ui.theme.TextSecondary

@Composable
fun IdentityDetailSection(
    identity: IdentityDetail,
    isSecretRevealed: Boolean,
    onToggleReveal: () -> Unit,
    onCopy: (label: String, value: String, isSensitive: Boolean) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val displayFullName = listOf(identity.honorificTitle.trim(), identity.fullName.trim())
            .filter { it.isNotEmpty() }
            .joinToString(" ")

        if (displayFullName.isNotEmpty()) {
            IdentityValueRow(
                label = "Full Name",
                value = displayFullName,
                onCopy = { onCopy("Full Name", displayFullName, false) }
            )
        }

        if (identity.username.isNotBlank()) {
            IdentityValueRow(
                label = "Username",
                value = identity.username,
                onCopy = { onCopy("Username", identity.username, false) }
            )
        }

        if (identity.company.isNotBlank()) {
            IdentityValueRow(
                label = "Company",
                value = identity.company,
                onCopy = { onCopy("Company", identity.company, false) }
            )
        }

        if (identity.email.isNotBlank()) {
            IdentityValueRow(
                label = "Email",
                value = identity.email,
                onCopy = { onCopy("Email", identity.email, false) }
            )
        }

        if (identity.phone.isNotBlank()) {
            IdentityValueRow(
                label = "Phone",
                value = identity.phone,
                onCopy = { onCopy("Phone", identity.phone, false) }
            )
        }

        val fullAddr = identity.fullAddress
        if (fullAddr.isNotBlank()) {
            HorizontalDivider(color = BorderSubtle.copy(alpha = 0.5f))
            IdentityValueRow(
                label = "Address",
                value = fullAddr,
                onCopy = { onCopy("Address", fullAddr, false) }
            )
        }

        val hasIdentification = identity.ssn.isNotBlank() ||
            identity.passportNumber.isNotBlank() ||
            identity.licenseNumber.isNotBlank()

        if (hasIdentification) {
            HorizontalDivider(color = BorderSubtle.copy(alpha = 0.5f))

            if (identity.ssn.isNotBlank()) {
                IdentitySensitiveRow(
                    label = "SSN",
                    secret = identity.ssn,
                    isRevealed = isSecretRevealed,
                    onToggleReveal = onToggleReveal,
                    onCopy = { onCopy("SSN", identity.ssn, true) }
                )
            }

            if (identity.passportNumber.isNotBlank()) {
                IdentitySensitiveRow(
                    label = "Passport Number",
                    secret = identity.passportNumber,
                    isRevealed = isSecretRevealed,
                    onToggleReveal = onToggleReveal,
                    onCopy = { onCopy("Passport Number", identity.passportNumber, true) }
                )
            }

            if (identity.licenseNumber.isNotBlank()) {
                IdentitySensitiveRow(
                    label = "License Number",
                    secret = identity.licenseNumber,
                    isRevealed = isSecretRevealed,
                    onToggleReveal = onToggleReveal,
                    onCopy = { onCopy("License Number", identity.licenseNumber, true) }
                )
            }
        }

        if (identity.notes.isNotBlank()) {
            HorizontalDivider(color = BorderSubtle.copy(alpha = 0.5f))
            IdentityValueRow(
                label = "Notes",
                value = identity.notes,
                onCopy = { onCopy("Notes", identity.notes, false) }
            )
        }
    }
}

@Composable
private fun IdentityValueRow(
    label: String,
    value: String,
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label.uppercase(),
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = TextPrimary,
                fontSize = 15.sp
            )
        }
        IconButton(onClick = onCopy, modifier = Modifier.size(48.dp)) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy $label",
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun IdentitySensitiveRow(
    label: String,
    secret: String,
    isRevealed: Boolean,
    onToggleReveal: () -> Unit,
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label.uppercase(),
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isRevealed) secret else "••••••••••••",
                color = if (isRevealed) ReefPink else TextPrimary,
                fontSize = 15.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onToggleReveal, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = if (isRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (isRevealed) "Hide $label" else "Show $label",
                    tint = if (isRevealed) ReefPink else TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onCopy, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy $label",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
