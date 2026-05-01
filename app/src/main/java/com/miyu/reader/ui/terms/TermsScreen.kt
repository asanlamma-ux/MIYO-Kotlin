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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miyu.reader.domain.model.TermGroup
import com.miyu.reader.ui.theme.LocalMIYUColors
import com.miyu.reader.viewmodel.TermsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsScreen(
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
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        // ── Header ──────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Terms",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = colors.onBackground,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Manage translation corrections for your novels",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.secondaryText,
                )
            }
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
            placeholder = { Text("Search groups or terms…") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
        )

        Spacer(Modifier.height(16.dp))

        // ── Create input ────────────────────────────────────────────
        AnimatedVisibility(visible = uiState.showCreateInput) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    OutlinedTextField(
                        value = newGroupName,
                        onValueChange = { newGroupName = it },
                        placeholder = { Text("New group name…") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
                            Spacer(Modifier.width(6.dp))
                            Text("Create", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        // ── Empty state ─────────────────────────────────────────────
        if (filteredGroups.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 60.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = colors.accent.copy(alpha = 0.12f),
                        modifier = Modifier.size(90.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.Translate,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(40.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(
                        if (uiState.searchQuery.isNotBlank()) "No Results" else "No Term Groups Yet",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = colors.onBackground,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (uiState.searchQuery.isNotBlank()) "Try a different search query."
                        else "Create a term group to start correcting MTL translations.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.secondaryText,
                    )
                    if (uiState.searchQuery.isBlank()) {
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = { viewModel.toggleCreateInput() },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Create First Group", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        } else {
            // ── Group cards ─────────────────────────────────────────
            filteredGroups.forEach { group ->
                TermGroupCard(
                    group = group,
                    onClick = { viewModel.setSelectedGroupId(group.id) },
                    onDelete = { groupToDelete = group },
                    accentColor = colors.accent,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                )
            }

            // Footer
            Spacer(Modifier.height(16.dp))
            Text(
                "${uiState.termGroups.size} group${if (uiState.termGroups.size != 1) "s" else ""} · ${uiState.termGroups.sumOf { it.terms.size }} total terms",
                style = MaterialTheme.typography.labelSmall,
                color = colors.secondaryText,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
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
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
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
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = original,
                    onValueChange = { original = it },
                    label = { Text("Original text") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = replacement,
                    onValueChange = { replacement = it },
                    label = { Text("Replacement") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        viewModel.addTerm(group.id, original, replacement)
                        original = ""
                        replacement = ""
                    },
                    enabled = original.isNotBlank() && replacement.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add term", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(14.dp))
                group.terms.forEach { term ->
                    ListItem(
                        headlineContent = { Text(term.originalText, fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text(term.correctedText) },
                        leadingContent = { Icon(Icons.Outlined.Translate, contentDescription = null, tint = colors.accent) },
                    )
                }
                Spacer(Modifier.height(24.dp))
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
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = accentColor.copy(alpha = 0.12f),
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Translate, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    group.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.onBackground,
                )
                Text(
                    "${group.terms.size} terms",
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
