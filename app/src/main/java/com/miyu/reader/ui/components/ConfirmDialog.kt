package com.miyu.reader.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.miyu.reader.ui.theme.LocalMIYUColors

/**
 * A reusable Material3 confirmation dialog.
 *
 * Supports a destructive variant (red confirm button), an optional leading icon,
 * and configurable button labels.
 *
 * @param title Dialog title text.
 * @param message Dialog body message.
 * @param confirmText Label for the confirm button. Defaults to "Confirm".
 * @param dismissText Label for the dismiss button. Defaults to "Cancel".
 * @param onConfirm Callback when the confirm button is clicked.
 * @param onDismiss Callback when the dialog is dismissed.
 * @param destructive When true, the confirm button uses error (red) coloring.
 * @param icon Optional icon displayed above the title.
 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "Confirm",
    dismissText: String = "Cancel",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
    icon: ImageVector? = null,
) {
    val colors = LocalMIYUColors.current
    val confirmColor = if (destructive) MaterialTheme.colorScheme.error else colors.primary

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = if (icon != null) {
            {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (destructive) MaterialTheme.colorScheme.error else colors.primary,
                )
            }
        } else {
            null
        },
        title = {
            Text(text = title)
        },
        text = {
            Text(text = message)
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = confirmText,
                    color = confirmColor,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = dismissText,
                    color = colors.secondaryText,
                )
            }
        },
    )
}

/**
 * Convenience composable for a destructive (delete/remove) confirmation dialog.
 *
 * The confirm button is red and a warning icon is shown by default.
 */
@Composable
fun DestructiveConfirmDialog(
    title: String = "Confirm Deletion",
    message: String = "Are you sure you want to delete this item? This action cannot be undone.",
    confirmText: String = "Delete",
    dismissText: String = "Cancel",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmDialog(
        title = title,
        message = message,
        confirmText = confirmText,
        dismissText = dismissText,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        destructive = true,
        icon = Icons.Default.Warning,
    )
}
