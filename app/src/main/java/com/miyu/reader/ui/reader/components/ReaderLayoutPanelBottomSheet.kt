package com.miyu.reader.ui.reader.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miyu.reader.domain.model.*
import com.miyu.reader.ui.theme.ReaderThemeColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderLayoutPanelBottomSheet(
    settings: ReadingSettings,
    readerTheme: ReaderThemeColors,
    onSettingsChanged: (ReadingSettings) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = readerTheme.cardBackground,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.78f)
                .padding(bottom = 24.dp)
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(readerTheme.secondaryText.copy(alpha = 0.4f))
                    .align(Alignment.CenterHorizontally)
            )

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.ViewColumn, contentDescription = null, tint = readerTheme.accent, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "Layout & display",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = readerTheme.text,
                        )
                        Text(
                            "Similar to Koodo-style reading controls",
                            color = readerTheme.secondaryText,
                            fontSize = 12.sp,
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = readerTheme.secondaryText)
                }
            }

            // Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                // Margins
                SectionHeader("MARGINS", Icons.Outlined.AlignHorizontalCenter, readerTheme)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    MarginPreset.entries.forEach { preset ->
                        val active = settings.marginPreset == preset
                        Chip(
                            label = preset.name.lowercase().replaceFirstChar { it.uppercase() },
                            active = active,
                            readerTheme = readerTheme,
                            onClick = { onSettingsChanged(settings.copy(marginPreset = preset)) }
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))

                // Column Width
                SectionHeader("COLUMN WIDTH", Icons.Outlined.VerticalSplit, readerTheme)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    val columnOptions = listOf(
                        null to "Full",
                        560 to "Compact",
                        640 to "Standard",
                        720 to "Comfort",
                        840 to "Wide"
                    )
                    columnOptions.forEach { (value, label) ->
                        val active = settings.contentColumnWidth == value
                        Chip(
                            label = label,
                            active = active,
                            readerTheme = readerTheme,
                            onClick = { onSettingsChanged(settings.copy(contentColumnWidth = value)) }
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))

                // Reading Flow
                SectionHeader("READING FLOW", Icons.Outlined.Menu, readerTheme, "Scroll for web-novel style, or paged for chapter focus.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("scroll", "paged").forEach { mode ->
                        val active = (settings.pageAnimation == PageAnimation.NONE && mode == "scroll") || 
                                     (settings.pageAnimation != PageAnimation.NONE && mode == "paged")
                        Chip(
                            label = mode.replaceFirstChar { it.uppercase() },
                            active = active,
                            readerTheme = readerTheme,
                            onClick = { 
                                val anim = if (mode == "scroll") PageAnimation.NONE else PageAnimation.SLIDE
                                onSettingsChanged(settings.copy(pageAnimation = anim)) 
                            }
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))

                // Text Columns
                SectionHeader("TEXT COLUMNS", Icons.Outlined.ViewArray, readerTheme, "Magazine-style layout; phones stay single column.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ReaderColumnLayout.entries.forEach { layout ->
                        val active = settings.readerColumnLayout == layout
                        Chip(
                            label = layout.name.lowercase().replaceFirstChar { it.uppercase() },
                            active = active,
                            readerTheme = readerTheme,
                            onClick = { onSettingsChanged(settings.copy(readerColumnLayout = layout)) }
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))

                // Auto Scroll
                SectionHeader("AUTO SCROLL", Icons.Outlined.KeyboardDoubleArrowDown, readerTheme, "Hands-free scrolling while you read.")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp), 
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    val scrollSpeeds = listOf(0f to "Off", 1f to "Slow", 2f to "Calm", 3f to "Med", 4f to "Fast", 5f to "Max")
                    scrollSpeeds.take(3).forEach { (value, label) ->
                        val active = settings.autoScrollSpeed == value
                        Chip(label, active, readerTheme) { onSettingsChanged(settings.copy(autoScrollSpeed = value)) }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    val scrollSpeeds = listOf(0f to "Off", 1f to "Slow", 2f to "Calm", 3f to "Med", 4f to "Fast", 5f to "Max")
                    scrollSpeeds.takeLast(3).forEach { (value, label) ->
                        val active = settings.autoScrollSpeed == value
                        Chip(label, active, readerTheme) { onSettingsChanged(settings.copy(autoScrollSpeed = value)) }
                    }
                }
                Spacer(Modifier.height(24.dp))

                // Bionic Reading Toggle
                ToggleRow(
                    title = "Bionic reading",
                    subtitle = "Emphasize the start of longer words",
                    icon = Icons.Outlined.AutoAwesome,
                    active = false, // Mock for now, not in ReadingSettings model
                    readerTheme = readerTheme,
                    onToggle = { /* TODO: add to ReadingSettings if needed */ }
                )
                Spacer(Modifier.height(16.dp))
                
                // Keep Screen On Toggle
                ToggleRow(
                    title = "Keep screen on",
                    subtitle = "While this book is open",
                    icon = Icons.Outlined.LightMode,
                    active = false, // Mock for now, not in ReadingSettings model
                    readerTheme = readerTheme,
                    onToggle = { /* TODO: add to ReadingSettings if needed */ }
                )
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    readerTheme: ReaderThemeColors,
    hint: String? = null
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
        Icon(icon, contentDescription = null, tint = readerTheme.secondaryText, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            title,
            color = readerTheme.secondaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp,
        )
    }
    if (hint != null) {
        Text(
            hint,
            color = readerTheme.secondaryText.copy(alpha = 0.9f),
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 10.dp)
        )
    }
}

@Composable
private fun Chip(
    label: String,
    active: Boolean,
    readerTheme: ReaderThemeColors,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) Color(android.graphics.Color.parseColor(readerTheme.accent)).copy(alpha = 0.15f) else readerTheme.background)
            .border(
                width = 1.5.dp,
                color = if (active) Color(android.graphics.Color.parseColor(readerTheme.accent)) else readerTheme.secondaryText.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            label,
            color = if (active) Color(android.graphics.Color.parseColor(readerTheme.accent)) else readerTheme.text,
            fontSize = 13.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    readerTheme: ReaderThemeColors,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(readerTheme.background)
            .border(
                width = 1.5.dp,
                color = if (active) Color(android.graphics.Color.parseColor(readerTheme.accent)) else readerTheme.secondaryText.copy(alpha = 0.2f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onToggle)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (active) Color(android.graphics.Color.parseColor(readerTheme.accent)) else readerTheme.secondaryText,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    title,
                    color = readerTheme.text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    subtitle,
                    color = readerTheme.secondaryText,
                    fontSize = 12.sp,
                )
            }
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(if (active) Color(android.graphics.Color.parseColor(readerTheme.accent)) else readerTheme.secondaryText.copy(alpha = 0.35f))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                if (active) "ON" else "OFF",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
