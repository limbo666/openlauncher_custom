package com.openlauncher.app.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.openlauncher.app.ui.theme.LocalDayMode

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String = "Confirm",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val isDayMode = LocalDayMode.current
    val containerColor = if (isDayMode) Color(0xFFFFFFFF) else Color(0xFF1A1A1A)
    val titleContentColor = if (isDayMode) Color(0xFF111111) else Color.White
    val textContentColor = if (isDayMode) Color(0xFF495057) else Color(0xFFCCCCCC)
    val cancelColor = if (isDayMode) Color(0xFF666666) else Color(0xFFAAAAAA)
    val confirmColor = if (isDayMode) Color(0xFFE03131) else Color(0xFFFF5252)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text  = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = confirmColor)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = cancelColor)
            }
        },
        containerColor    = containerColor,
        titleContentColor = titleContentColor,
        textContentColor  = textContentColor
    )
}
