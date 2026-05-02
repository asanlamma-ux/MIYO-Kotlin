package com.miyu.reader.ui.core.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.miyu.reader.ui.core.theme.MiyoSpacing
import com.miyu.reader.ui.theme.LocalMIYUColors

@Composable
fun MiyoTopSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    actions: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MiyoSpacing.medium, vertical = MiyoSpacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MiyoSpacing.small),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            enabled = enabled,
            placeholder = { Text(placeholder) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
        )
        actions()
    }
}

@Composable
fun MiyoMainOverflowMenu(
    onOpenSettings: () -> Unit,
    onOpenThemePicker: () -> Unit,
    onExportData: () -> Unit,
    onAbout: () -> Unit,
    modifier: Modifier = Modifier,
    extraItems: (@Composable ColumnScope.(dismiss: () -> Unit) -> Unit)? = null,
) {
    val colors = LocalMIYUColors.current
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "More options", tint = colors.onBackground)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            val dismiss = { expanded = false }
            extraItems?.invoke(this, dismiss)
            DropdownMenuItem(
                text = { Text("App Settings") },
                onClick = {
                    dismiss()
                    onOpenSettings()
                },
                leadingIcon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
            )
            DropdownMenuItem(
                text = { Text("Theme") },
                onClick = {
                    dismiss()
                    onOpenThemePicker()
                },
                leadingIcon = { Icon(Icons.Outlined.Palette, contentDescription = null) },
            )
            DropdownMenuItem(
                text = { Text("Save & Export") },
                onClick = {
                    dismiss()
                    onExportData()
                },
                leadingIcon = { Icon(Icons.Outlined.Backup, contentDescription = null) },
            )
            DropdownMenuItem(
                text = { Text("About / More Info") },
                onClick = {
                    dismiss()
                    onAbout()
                },
                leadingIcon = { Icon(Icons.Outlined.Info, contentDescription = null) },
            )
        }
    }
}

@Composable
fun MiyoBackupChoiceDialog(
    onDismiss: () -> Unit,
    onExportData: () -> Unit,
    onImportData: () -> Unit,
) {
    val colors = LocalMIYUColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Backup, contentDescription = null, tint = colors.accent) },
        title = { Text("Save & Export") },
        text = {
            Text(
                "Create or restore a .miyo archive containing local books, covers, settings, and database files.",
                color = colors.secondaryText,
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    onExportData()
                },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text("Export Data", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Spacer(Modifier.width(4.dp))
                TextButton(
                    onClick = {
                        onDismiss()
                        onImportData()
                    },
                ) {
                    Text("Import Data")
                }
            }
        },
    )
}

@Composable
fun MiyoAboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("MIYO Reader") },
        text = {
            Text(
                "A local-first reader for novels, web fiction, fanfiction, EPUB libraries, and translated serials.",
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
    )
}
