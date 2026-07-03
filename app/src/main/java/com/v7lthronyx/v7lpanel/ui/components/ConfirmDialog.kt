package com.v7lthronyx.v7lpanel.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.v7lthronyx.v7lpanel.ui.theme.V7LColors

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String = "Delete",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor  = V7LColors.bg2,
        titleContentColor = V7LColors.t0,
        textContentColor  = V7LColors.t2,
        title   = { Text(title) },
        text    = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = V7LColors.red)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = V7LColors.t2)
            }
        }
    )
}
