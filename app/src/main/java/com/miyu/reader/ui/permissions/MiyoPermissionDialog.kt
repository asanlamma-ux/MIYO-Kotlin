package com.miyu.reader.ui.permissions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.miyu.reader.permissions.MiyoPermissionSnapshot
import com.miyu.reader.ui.core.theme.MiyoSpacing
import com.miyu.reader.ui.theme.LocalMIYUColors

@Composable
fun MiyoStoragePermissionDialog(
    snapshot: MiyoPermissionSnapshot,
    onConfigure: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalMIYUColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.cardBackground,
        tonalElevation = 6.dp,
        shape = RoundedCornerShape(28.dp),
        icon = {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = colors.accent.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.18f)),
            ) {
                Icon(
                    Icons.Outlined.Folder,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.padding(16.dp),
                )
            }
        },
        title = {
            Text(
                "Storage permission needed",
                color = colors.onBackground,
                fontWeight = FontWeight.Black,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MiyoSpacing.small)) {
                Text(
                    "Miyo needs Android storage access for watched folders, EPUB exports, and resilient background downloads.",
                    color = colors.secondaryText,
                    style = MaterialTheme.typography.bodyMedium,
                )
                PermissionLine("All files access", snapshot.storageManagerGranted)
                PermissionLine("Media cover access", snapshot.mediaImagesGranted)
                PermissionLine("Download notifications", snapshot.notificationGranted)
                PermissionLine("Background download allowance", snapshot.batteryOptimizationIgnored)
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MiyoSpacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Later")
                }
                Button(
                    onClick = onConfigure,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Configure", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            Spacer(Modifier.height(0.dp))
        },
    )
}

@Composable
private fun PermissionLine(
    label: String,
    granted: Boolean,
) {
    val colors = LocalMIYUColors.current
    Text(
        text = "${if (granted) "Granted" else "Missing"} - $label",
        color = if (granted) colors.accent else MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
    )
}
