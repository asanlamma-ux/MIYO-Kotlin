package com.miyu.reader.ui.reader.components

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.miyu.reader.domain.model.TermGroup
import com.miyu.reader.ui.core.theme.MiyoSpacing
import com.miyu.reader.ui.theme.ReaderThemeColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTermBottomSheet(
    selectedText: String,
    groups: List<TermGroup>,
    readerTheme: ReaderThemeColors,
    onDismiss: () -> Unit,
    onSaveToGroup: (
        originalText: String,
        correctedText: String,
        translationText: String?,
        context: String?,
        groupId: String,
        imageUri: String?,
    ) -> Unit,
    onCreateGroupAndSave: (
        groupName: String,
        originalText: String,
        correctedText: String,
        translationText: String?,
        context: String?,
        imageUri: String?,
    ) -> Unit,
) {
    val context = LocalContext.current
    var originalText by remember(selectedText) { mutableStateOf(selectedText) }
    var correctedText by remember(selectedText) { mutableStateOf("") }
    var translationText by remember(selectedText) { mutableStateOf("") }
    var contextText by remember(selectedText) { mutableStateOf("") }
    var imageUri by remember(selectedText) { mutableStateOf<String?>(null) }
    var selectedGroupId by remember(groups) { mutableStateOf(groups.firstOrNull()?.id) }
    var groupMenuOpen by remember { mutableStateOf(false) }
    var createNewGroup by remember(groups) { mutableStateOf(groups.isEmpty()) }
    var newGroupName by remember { mutableStateOf("") }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        imageUri = uri.toString()
    }

    val canSave = originalText.isNotBlank() &&
        correctedText.isNotBlank() &&
        ((createNewGroup && newGroupName.isNotBlank()) || (!createNewGroup && selectedGroupId != null))

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = readerTheme.cardBackground,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MiyoSpacing.large, vertical = MiyoSpacing.medium),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(MiyoSpacing.medium)) {
                Icon(Icons.Outlined.Bookmarks, contentDescription = null, tint = readerTheme.accent)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Add term",
                        color = readerTheme.text,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    )
                    Text(
                        "Correct the selected MTL text and apply the group to this book.",
                        color = readerTheme.secondaryText,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Spacer(Modifier.height(MiyoSpacing.large))

            TermField(
                label = "Original text",
                value = originalText,
                onValueChange = { originalText = it },
                readerTheme = readerTheme,
                minLines = 2,
            )
            TermField(
                label = "Replacement",
                value = correctedText,
                onValueChange = { correctedText = it },
                readerTheme = readerTheme,
                minLines = 2,
            )
            TermField(
                label = "Translation note (optional)",
                value = translationText,
                onValueChange = { translationText = it },
                readerTheme = readerTheme,
            )
            TermField(
                label = "Context (optional)",
                value = contextText,
                onValueChange = { contextText = it },
                readerTheme = readerTheme,
            )

            OutlinedButton(
                onClick = { imagePicker.launch(arrayOf("image/*")) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.Image, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (imageUri == null) "Attach image to term" else "Image attached",
                    color = readerTheme.accent,
                )
            }

            Spacer(Modifier.height(8.dp))

            if (createNewGroup) {
                TermField(
                    label = "New group name",
                    value = newGroupName,
                    onValueChange = { newGroupName = it },
                    readerTheme = readerTheme,
                    singleLine = true,
                )
                if (groups.isNotEmpty()) {
                    TextButton(onClick = { createNewGroup = false }) {
                        Text("Use existing group", color = readerTheme.accent)
                    }
                }
            } else {
                ExposedDropdownMenuBox(
                    expanded = groupMenuOpen,
                    onExpandedChange = { groupMenuOpen = !groupMenuOpen },
                ) {
                    val selectedName = groups.firstOrNull { it.id == selectedGroupId }?.name ?: "Choose group"
                    OutlinedTextField(
                        value = selectedName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Term group") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupMenuOpen) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = readerTheme.accent,
                            focusedTextColor = readerTheme.text,
                            unfocusedTextColor = readerTheme.text,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                    )
                    ExposedDropdownMenu(
                        expanded = groupMenuOpen,
                        onDismissRequest = { groupMenuOpen = false },
                    ) {
                        groups.forEach { group ->
                            DropdownMenuItem(
                                text = { Text(group.name) },
                                onClick = {
                                    selectedGroupId = group.id
                                    groupMenuOpen = false
                                },
                            )
                        }
                    }
                }
                TextButton(onClick = { createNewGroup = true }) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Spacer(Modifier.width(MiyoSpacing.small))
                    Text("Create new group", color = readerTheme.accent)
                }
            }

            Spacer(Modifier.height(MiyoSpacing.medium))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = readerTheme.secondaryText)
                }
                Spacer(Modifier.width(MiyoSpacing.small))
                Button(
                    enabled = canSave,
                    colors = ButtonDefaults.buttonColors(containerColor = readerTheme.accent),
                    shape = RoundedCornerShape(12.dp),
                    onClick = {
                        if (createNewGroup) {
                            onCreateGroupAndSave(newGroupName, originalText, correctedText, translationText, contextText, imageUri)
                        } else {
                            onSaveToGroup(originalText, correctedText, translationText, contextText, selectedGroupId ?: return@Button, imageUri)
                        }
                    },
                ) {
                    Text("Save term", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TermField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    readerTheme: ReaderThemeColors,
    minLines: Int = 1,
    singleLine: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        minLines = minLines,
        singleLine = singleLine,
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedBorderColor = readerTheme.accent,
            unfocusedBorderColor = readerTheme.secondaryText.copy(alpha = 0.35f),
            focusedTextColor = readerTheme.text,
            unfocusedTextColor = readerTheme.text,
            focusedLabelColor = readerTheme.accent,
            unfocusedLabelColor = readerTheme.secondaryText,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = MiyoSpacing.medium),
    )
}
