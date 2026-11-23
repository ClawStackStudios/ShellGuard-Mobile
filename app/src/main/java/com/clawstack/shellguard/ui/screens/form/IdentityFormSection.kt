package com.clawstack.shellguard.ui.screens.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clawstack.shellguard.ui.theme.TextMuted

@Composable
fun IdentityFormSection(
    uiState: ItemFormUiState,
    viewModel: ItemFormViewModel,
    fieldColors: TextFieldColors
) {
    var showSensitive by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        SectionLabel("PERSONAL DETAILS")
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IdentityField(
                value = uiState.honorificTitle,
                onValueChange = viewModel::updateHonorificTitle,
                label = "Title",
                placeholder = "Mr / Ms",
                fieldColors = fieldColors,
                modifier = Modifier.weight(0.32f)
            )
            IdentityField(
                value = uiState.firstName,
                onValueChange = viewModel::updateFirstName,
                label = "First Name",
                placeholder = "Jane",
                fieldColors = fieldColors,
                modifier = Modifier.weight(0.68f)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IdentityField(
                value = uiState.middleName,
                onValueChange = viewModel::updateMiddleName,
                label = "Middle Name",
                placeholder = "A.",
                fieldColors = fieldColors,
                modifier = Modifier.weight(0.45f)
            )
            IdentityField(
                value = uiState.lastName,
                onValueChange = viewModel::updateLastName,
                label = "Last Name",
                placeholder = "Doe",
                fieldColors = fieldColors,
                modifier = Modifier.weight(0.55f)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IdentityField(
                value = uiState.username,
                onValueChange = viewModel::updateUsername,
                label = "Username",
                placeholder = "jdoe",
                fieldColors = fieldColors,
                modifier = Modifier.weight(1f)
            )
            IdentityField(
                value = uiState.company,
                onValueChange = viewModel::updateCompany,
                label = "Company",
                placeholder = "ClawStack",
                fieldColors = fieldColors,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))
        SectionLabel("CONTACT")
        Spacer(modifier = Modifier.height(8.dp))

        IdentityField(
            value = uiState.email,
            onValueChange = viewModel::updateEmail,
            label = "Email",
            placeholder = "jane@example.com",
            keyboardType = KeyboardType.Email,
            fieldColors = fieldColors,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))

        IdentityField(
            value = uiState.phone,
            onValueChange = viewModel::updatePhone,
            label = "Phone",
            placeholder = "+1 555 0199",
            keyboardType = KeyboardType.Phone,
            fieldColors = fieldColors,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(18.dp))
        SectionLabel("ADDRESS")
        Spacer(modifier = Modifier.height(8.dp))

        IdentityField(
            value = uiState.address1,
            onValueChange = viewModel::updateAddress1,
            label = "Address 1",
            placeholder = "123 Reef Ave",
            fieldColors = fieldColors,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))
        IdentityField(
            value = uiState.address2,
            onValueChange = viewModel::updateAddress2,
            label = "Address 2",
            placeholder = "Apt / Suite",
            fieldColors = fieldColors,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))
        IdentityField(
            value = uiState.address3,
            onValueChange = viewModel::updateAddress3,
            label = "Address 3",
            placeholder = "Building / District",
            fieldColors = fieldColors,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IdentityField(
                value = uiState.city,
                onValueChange = viewModel::updateCity,
                label = "City",
                placeholder = "Seattle",
                fieldColors = fieldColors,
                modifier = Modifier.weight(1f)
            )
            IdentityField(
                value = uiState.state,
                onValueChange = viewModel::updateState,
                label = "State / Province",
                placeholder = "WA",
                fieldColors = fieldColors,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IdentityField(
                value = uiState.postalCode,
                onValueChange = viewModel::updatePostalCode,
                label = "Postal Code",
                placeholder = "98101",
                fieldColors = fieldColors,
                modifier = Modifier.weight(0.45f)
            )
            IdentityField(
                value = uiState.country,
                onValueChange = viewModel::updateCountry,
                label = "Country",
                placeholder = "United States",
                fieldColors = fieldColors,
                modifier = Modifier.weight(0.55f)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))
        SectionLabel("IDENTIFICATION")
        Spacer(modifier = Modifier.height(8.dp))

        SensitiveIdentityField(
            value = uiState.ssn,
            onValueChange = viewModel::updateSsn,
            label = "SSN",
            revealed = showSensitive,
            onToggleReveal = { showSensitive = !showSensitive },
            fieldColors = fieldColors
        )
        Spacer(modifier = Modifier.height(10.dp))
        SensitiveIdentityField(
            value = uiState.passportNumber,
            onValueChange = viewModel::updatePassportNumber,
            label = "Passport Number",
            revealed = showSensitive,
            onToggleReveal = { showSensitive = !showSensitive },
            fieldColors = fieldColors
        )
        Spacer(modifier = Modifier.height(10.dp))
        SensitiveIdentityField(
            value = uiState.licenseNumber,
            onValueChange = viewModel::updateLicenseNumber,
            label = "License Number",
            revealed = showSensitive,
            onToggleReveal = { showSensitive = !showSensitive },
            fieldColors = fieldColors
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = uiState.notes,
            onValueChange = viewModel::updateNotes,
            label = { Text("Notes", fontSize = 12.sp) },
            placeholder = { Text("Additional identity notes...", color = TextMuted) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            minLines = 3,
            maxLines = 6,
            keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
            colors = fieldColors
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = TextMuted,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp
    )
}

@Composable
private fun IdentityField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    fieldColors: TextFieldColors,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 12.sp) },
        placeholder = { Text(placeholder, color = TextMuted) },
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, autoCorrectEnabled = false),
        colors = fieldColors
    )
}

@Composable
private fun SensitiveIdentityField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    revealed: Boolean,
    onToggleReveal: () -> Unit,
    fieldColors: TextFieldColors
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 12.sp) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        singleLine = true,
        visualTransformation = if (revealed) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
        trailingIcon = {
            IconButton(onClick = onToggleReveal) {
                Icon(
                    imageVector = if (revealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (revealed) "Hide $label" else "Show $label",
                    tint = TextMuted
                )
            }
        },
        colors = fieldColors
    )
}
