package com.miyu.reader.ui.reader.components

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.Surface
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
                        "Save a reusable correction and apply its group to this book.",
                        color = readerTheme.secondaryText,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Spacer(Modifier.height(MiyoSpacing.large))

            TermSectionCard(
                readerTheme = readerTheme,
                title = "Correction",
                subtitle = "Saving the same original phrase to the same group replaces the older correction.",
            ) {
                Surface(
                    color = readerTheme.background.copy(alpha = 0.46f),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(MiyoSpacing.large),
                    ) {
                        Text(
                            text = selectedText,
                            color = readerTheme.text,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        )
                        Spacer(Modifier.height(MiyoSpacing.extraSmall))
                        Text(
                            text = "Selected from the current chapter.",
                            color = readerTheme.secondaryText,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                Spacer(Modifier.height(MiyoSpacing.medium))
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
            }

            Spacer(Modifier.height(MiyoSpacing.medium))

            TermSectionCard(
                readerTheme = readerTheme,
                title = "Save destination",
                subtitle = "Groups let you reuse the same correction set across multiple books.",
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MiyoSpacing.small),
                    verticalArrangement = Arrangement.spacedBy(MiyoSpacing.small),
                ) {
                    GroupModeButton(
                        label = "Existing group",
                        selected = !createNewGroup,
                        enabled = groups.isNotEmpty(),
                        readerTheme = readerTheme,
                        onClick = { createNewGroup = false },
                    )
                    GroupModeButton(
                        label = "New group",
                        selected = createNewGroup,
                        enabled = true,
                        readerTheme = readerTheme,
                        onClick = { createNewGroup = true },
                    )
                }
                Spacer(Modifier.height(MiyoSpacing.medium))

                if (createNewGroup) {
                    TermField(
                        label = "New group name",
                        value = newGroupName,
                        onValueChange = { newGroupName = it },
                        readerTheme = readerTheme,
                        singleLine = true,
                    )
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
                                unfocusedBorderColor = readerTheme.secondaryText.copy(alpha = 0.35f),
                                focusedLabelColor = readerTheme.accent,
                                unfocusedLabelColor = readerTheme.secondaryText,
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
                    Spacer(Modifier.height(MiyoSpacing.extraSmall))
                    Text(
                        text = "${groups.size} group${if (groups.size == 1) "" else "s"} available",
                        color = readerTheme.secondaryText,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Spacer(Modifier.height(MiyoSpacing.medium))

            TermSectionCard(
                readerTheme = readerTheme,
                title = "Reference image",
                subtitle = "Optional. Attach a visual cue to make the correction easier to review later.",
            ) {
                OutlinedButton(
                    onClick = { imagePicker.launch(arrayOf("image/*")) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.Image, contentDescription = null)
                    Spacer(Modifier.width(MiyoSpacing.small))
                    Text(
                        if (imageUri == null) "Attach image" else "Replace attached image",
                        color = readerTheme.accent,
                    )
                }
                if (imageUri != null) {
                    Spacer(Modifier.height(MiyoSpacing.extraSmall))
                    Text(
                        text = "Image attached",
                        color = readerTheme.secondaryText,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Spacer(Modifier.height(MiyoSpacing.large))

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
                    shape = RoundedCornerShape(14.dp),
                    onClick = {
                        if (createNewGroup) {
                            onCreateGroupAndSave(newGroupName, originalText, correctedText, translationText, contextText, imageUri)
                        } else {
                            onSaveToGroup(originalText, correctedText, translationText, contextText, selectedGroupId ?: return@Button, imageUri)
                        }
                    },
                ) {
                    Icon(Icons.Outlined.Translate, contentDescription = null)
                    Spacer(Modifier.width(MiyoSpacing.small))
                    Text("Save correction", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TermSectionCard(
    readerTheme: ReaderThemeColors,
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = readerTheme.background.copy(alpha = 0.56f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MiyoSpacing.large),
        ) {
            Text(
                text = title,
                color = readerTheme.text,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            subtitle?.let {
                Spacer(Modifier.height(MiyoSpacing.extraSmall))
                Text(
                    text = it,
                    color = readerTheme.secondaryText,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(MiyoSpacing.medium))
            }
            content()
        }
    }
}

@Composable
private fun GroupModeButton(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    readerTheme: ReaderThemeColors,
    onClick: () -> Unit,
) {
    if (selected) {
        Button(
            onClick = onClick,
            enabled = enabled,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = readerTheme.accent,
            ),
        ) {
            Text(label, fontWeight = FontWeight.SemiBold)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            shape = RoundedCornerShape(14.dp),
        ) {
            Text(label, color = readerTheme.secondaryText)
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
            cursorColor = readerTheme.accent,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = MiyoSpacing.medium),
    )
}
