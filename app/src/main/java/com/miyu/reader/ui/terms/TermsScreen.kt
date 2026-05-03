package com.miyu.reader.ui.terms

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miyu.reader.domain.model.TermGroup
import com.miyu.reader.ui.core.components.MiyoEmptyScreen
import com.miyu.reader.ui.core.components.MiyoScreenHeader
import com.miyu.reader.ui.core.components.MiyoWorkspaceExitButton
import com.miyu.reader.ui.core.theme.MiyoSpacing
import com.miyu.reader.ui.theme.LocalMIYUColors
import com.miyu.reader.viewmodel.TermsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsScreen(
    onBack: (() -> Unit)? = null,
    viewModel: TermsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filteredGroups by viewModel.filteredGroups.collectAsStateWithLifecycle()
    val colors = LocalMIYUColors.current

    var newGroupName by remember { mutableStateOf("") }
    var groupToDelete by remember { mutableStateOf<TermGroup?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background.copy(alpha = 0.94f))
            .verticalScroll(rememberScrollState())
            .padding(bottom = MiyoSpacing.extraLarge),
    ) {
        onBack?.let { back ->
            Box(modifier = Modifier.padding(start = MiyoSpacing.extraLarge, top = MiyoSpacing.extraLarge)) {
                MiyoWorkspaceExitButton(
                    label = "Exit term groups",
                    onClick = back,
                )
            }
        }
        MiyoScreenHeader(
            title = "Terms",
            subtitle = "Manage translation corrections for your novels",
        ) {
            FilledIconButton(
                onClick = { viewModel.toggleCreateInput() },
                shape = RoundedCornerShape(13.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = colors.accent),
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
        // ── Search ──────────────────────────────────────────────────
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = viewModel::setSearchQuery,
            placeholder = { Text("Search groups or saved terms…") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MiyoSpacing.extraLarge)
                .padding(top = MiyoSpacing.small),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
        )

        Spacer(Modifier.height(MiyoSpacing.large))

        // ── Create input ────────────────────────────────────────────
        AnimatedVisibility(visible = uiState.showCreateInput) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MiyoSpacing.extraLarge),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
            ) {
                Column(modifier = Modifier.padding(MiyoSpacing.large)) {
                    Text(
                        text = "Create group",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.onBackground,
                    )
                    Spacer(Modifier.height(MiyoSpacing.extraSmall))
                    Text(
                        text = "Group related corrections together so they can be reused across books.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.secondaryText,
                    )
                    Spacer(Modifier.height(MiyoSpacing.medium))
                    OutlinedTextField(
                        value = newGroupName,
                        onValueChange = { newGroupName = it },
                        placeholder = { Text("New group name…") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                    )
                    Spacer(Modifier.height(MiyoSpacing.medium))
                    Row(horizontalArrangement = Arrangement.spacedBy(MiyoSpacing.small)) {
                        OutlinedButton(
                            onClick = {
                                viewModel.toggleCreateInput()
                                newGroupName = ""
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                if (newGroupName.isNotBlank()) {
                                    viewModel.createGroup(newGroupName.trim())
                                    newGroupName = ""
                                }
                            },
                            modifier = Modifier.weight(2f),
                            enabled = newGroupName.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(MiyoSpacing.extraSmall))
                            Text("Create", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(Modifier.height(MiyoSpacing.medium))
        }

        // ── Empty state ─────────────────────────────────────────────
        if (filteredGroups.isEmpty()) {
            MiyoEmptyScreen(
                icon = Icons.Outlined.Translate,
                title = if (uiState.searchQuery.isNotBlank()) "No Results" else "No Term Groups Yet",
                message = if (uiState.searchQuery.isNotBlank()) {
                    "Try a different search query."
                } else {
                    "Create a term group to start saving translation corrections."
                },
                actionLabel = if (uiState.searchQuery.isBlank()) "Create First Group" else null,
                onAction = if (uiState.searchQuery.isBlank()) viewModel::toggleCreateInput else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 60.dp),
            )
        } else {
            // ── Group cards ─────────────────────────────────────────
            filteredGroups.forEach { group ->
                TermGroupCard(
                    group = group,
                    onClick = { viewModel.setSelectedGroupId(group.id) },
                    onDelete = { groupToDelete = group },
                    accentColor = colors.accent,
                    modifier = Modifier.padding(horizontal = MiyoSpacing.extraLarge, vertical = MiyoSpacing.extraSmall),
                )
            }

            // Footer
            Spacer(Modifier.height(MiyoSpacing.large))
            Text(
                "${uiState.termGroups.size} group${if (uiState.termGroups.size != 1) "s" else ""} · ${uiState.termGroups.sumOf { it.terms.size }} total terms",
                style = MaterialTheme.typography.labelSmall,
                color = colors.secondaryText,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MiyoSpacing.extraLarge),
            )
        }
    }

    // Delete confirmation dialog
    groupToDelete?.let { group ->
        AlertDialog(
            onDismissRequest = { groupToDelete = null },
            title = { Text("Delete Group") },
            text = { Text("Remove \"${group.name}\" and all its terms? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteGroup(group.id)
                        groupToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { groupToDelete = null }) { Text("Cancel") }
            },
        )
    }

    uiState.termGroups.firstOrNull { it.id == uiState.selectedGroupId }?.let { group ->
        var original by remember(group.id) { mutableStateOf("") }
        var replacement by remember(group.id) { mutableStateOf("") }
        ModalBottomSheet(onDismissRequest = { viewModel.setSelectedGroupId(null) }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = MiyoSpacing.extraLarge, vertical = MiyoSpacing.small),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MiyoSpacing.medium),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = colors.accent.copy(alpha = 0.12f),
                        modifier = Modifier.size(52.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.Translate,
                                contentDescription = null,
                                tint = colors.accent,
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            group.name,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = colors.onBackground,
                        )
                        Text(
                            "${group.terms.size} terms · applied to ${group.appliedToBooks.size} books",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.secondaryText,
                        )
                    }
                }
                Spacer(Modifier.height(MiyoSpacing.large))
                TermsPanel(
                    title = "Add or replace correction",
                    subtitle = "Adding the same original text again replaces the existing correction in this group.",
                ) {
                    OutlinedTextField(
                        value = original,
                        onValueChange = { original = it },
                        label = { Text("Original text") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        shape = RoundedCornerShape(14.dp),
                    )
                    Spacer(Modifier.height(MiyoSpacing.small))
                    OutlinedTextField(
                        value = replacement,
                        onValueChange = { replacement = it },
                        label = { Text("Replacement") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        shape = RoundedCornerShape(14.dp),
                    )
                    Spacer(Modifier.height(MiyoSpacing.medium))
                    Button(
                        onClick = {
                            viewModel.addTerm(group.id, original, replacement)
                            original = ""
                            replacement = ""
                        },
                        enabled = original.isNotBlank() && replacement.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(MiyoSpacing.small))
                        Text("Save term", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(MiyoSpacing.medium))
                TermsPanel(
                    title = "Saved terms",
                    subtitle = if (group.terms.isEmpty()) {
                        "No saved corrections in this group yet."
                    } else {
                        "${group.terms.size} saved correction${if (group.terms.size == 1) "" else "s"}."
                    },
                ) {
                    if (group.terms.isEmpty()) {
                        Text(
                            text = "Add a correction above to populate this group.",
                            color = colors.secondaryText,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        group.terms.forEach { term ->
                            TermRow(term.originalText, term.correctedText)
                        }
                    }
                }
                Spacer(Modifier.height(MiyoSpacing.extraLarge))
            }
        }
    }
}

@Composable
private fun TermGroupCard(
    group: TermGroup,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    accentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMIYUColors.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(MiyoSpacing.large),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = accentColor.copy(alpha = 0.12f),
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Translate, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.width(MiyoSpacing.medium))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    group.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.onBackground,
                )
                Spacer(Modifier.height(MiyoSpacing.extraSmall))
                Text(
                    "${group.terms.size} terms · ${group.appliedToBooks.size} book${if (group.appliedToBooks.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.secondaryText,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = colors.secondaryText, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun TermsPanel(
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalMIYUColors.current
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MiyoSpacing.large),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colors.onBackground,
            )
            subtitle?.let {
                Spacer(Modifier.height(MiyoSpacing.extraSmall))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.secondaryText,
                )
            }
            Spacer(Modifier.height(MiyoSpacing.medium))
            content()
        }
    }
}

@Composable
private fun TermRow(
    originalText: String,
    correctedText: String,
) {
    val colors = LocalMIYUColors.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.background.copy(alpha = 0.35f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = MiyoSpacing.small),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MiyoSpacing.medium),
        ) {
            Text(
                text = originalText,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = colors.onBackground,
            )
            Spacer(Modifier.height(MiyoSpacing.extraSmall))
            Text(
                text = correctedText,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.secondaryText,
            )
        }
    }
}
