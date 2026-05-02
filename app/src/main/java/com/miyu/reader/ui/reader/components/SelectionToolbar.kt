package com.miyu.reader.ui.reader.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.miyu.reader.ui.core.theme.MiyoSpacing
import com.miyu.reader.ui.theme.ReaderThemeColors

data class SelectionData(
    val text: String,
    val originalText: String? = null,
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Float = 0f,
    val height: Float = 0f,
)

data class HighlightData(
    val text: String,
    val color: String,
    val note: String? = null,
    val textColor: String? = null,
)

private val HIGHLIGHT_COLORS = listOf(
    "#E8D97A", // Yellow
    "#8DB870", // Green
    "#6EC4B0", // Teal
    "#74B4E6", // Blue
)

private val TEXT_COLORS = listOf(
    "#222222", // Black
    "#CC3333", // Red
    "#2255CC", // Blue
    "#229944", // Green
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionToolbar(
    selection: SelectionData?,
    readerTheme: ReaderThemeColors,
    onClose: () -> Unit,
    onHighlight: (HighlightData) -> Unit,
    onNote: (HighlightData) -> Unit,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
    onDictionary: (String) -> Unit,
    onTranslate: (String) -> Unit,
    onBookmarkSelection: (String) -> Unit,
    onAddTerm: (String) -> Unit,
) {
    var showColorRow by remember(selection) { mutableStateOf(false) }
    var showNoteModal by remember(selection) { mutableStateOf(false) }
    var selectedHighlightColor by remember { mutableStateOf(HIGHLIGHT_COLORS[0]) }

    if (selection == null) return

    val bgColor = Color(0xEB1A1A24) // Dark transluscent
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp
    val selectionTop = with(density) { selection.y.toDp() }.coerceAtLeast(0.dp)
    val selectionHeight = with(density) { selection.height.toDp() }.coerceAtLeast(20.dp)
    val toolbarHeight = if (showColorRow) 98.dp else 54.dp
    val toolbarWidth = (screenWidth - MiyoSpacing.large - MiyoSpacing.large).coerceAtMost(280.dp)
    val anchorLeft = (
        with(density) { selection.x.toDp() } - (toolbarWidth / 2)
        ).coerceIn(MiyoSpacing.medium, (screenWidth - toolbarWidth - MiyoSpacing.medium).coerceAtLeast(MiyoSpacing.medium))
    val belowTop = selectionTop + selectionHeight + MiyoSpacing.large
    val aboveTop = selectionTop - toolbarHeight - MiyoSpacing.large
    val anchorTop = when {
        belowTop + toolbarHeight <= screenHeight - MiyoSpacing.large -> belowTop
        aboveTop >= MiyoSpacing.medium -> aboveTop
        else -> (screenHeight - toolbarHeight - MiyoSpacing.medium).coerceAtLeast(MiyoSpacing.medium)
    }

    // Note Modal
    if (showNoteModal) {
        NoteModal(
            selectedText = selection.text,
            readerTheme = readerTheme,
            initialColor = selectedHighlightColor,
            onClose = {
                showNoteModal = false
                onClose()
            },
            onConfirm = { note, color, textColor ->
                onNote(HighlightData(selection.text, color, note, textColor))
                showNoteModal = false
                onClose()
            },
        )
        return
    }

    Popup(
        offset = IntOffset(
            x = with(density) { anchorLeft.roundToPx() },
            y = with(density) { anchorTop.roundToPx() },
        ),
    ) {
        Surface(
            modifier = Modifier
                .width(toolbarWidth),
            shape = RoundedCornerShape(18.dp),
            color = bgColor,
            shadowElevation = 8.dp,
        ) {
            Column(modifier = Modifier.padding(bottom = 6.dp, top = 6.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = MiyoSpacing.small, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ToolbarItem("Note", Icons.Outlined.Edit) { showNoteModal = true }
                    ToolbarDivider()
                    ToolbarItem(
                        "Highlight", 
                        Icons.Outlined.Circle, 
                        isActive = showColorRow, 
                        activeColor = readerTheme.accent
                    ) { showColorRow = !showColorRow }
                    ToolbarDivider()
                    ToolbarItem("Term", Icons.Outlined.LibraryAdd) { 
                        onAddTerm(selection.originalText ?: selection.text)
                        onClose()
                    }
                    ToolbarDivider()
                    ToolbarItem("Copy", Icons.Outlined.ContentCopy) { 
                        onCopy(selection.text)
                        onClose()
                    }
                    ToolbarDivider()
                    ToolbarItem("Define", Icons.Outlined.MenuBook) {
                        onDictionary(selection.text)
                        onClose()
                    }
                    ToolbarDivider()
                    ToolbarItem("Translate", Icons.Outlined.Translate) {
                        onTranslate(selection.text)
                        onClose()
                    }
                    ToolbarDivider()
                    ToolbarItem("Share", Icons.Outlined.Share) {
                        onShare(selection.text)
                        onClose()
                    }
                }

                // Expanded Color Row
                AnimatedVisibility(visible = showColorRow) {
                    Column {
                        Divider(color = Color.White.copy(alpha = 0.12f), modifier = Modifier.padding(vertical = MiyoSpacing.small))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = MiyoSpacing.medium, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            HIGHLIGHT_COLORS.forEach { hex ->
                                val color = Color(android.graphics.Color.parseColor(hex))
                                val isSelected = selectedHighlightColor == hex
                                Box(
                                    modifier = Modifier
                                        .size(if (isSelected) 32.dp else 26.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isSelected) 2.dp else 0.dp,
                                            color = if (isSelected) Color.White else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { 
                                            selectedHighlightColor = hex
                                            onHighlight(HighlightData(selection.text, hex))
                                            showColorRow = false
                                            onClose()
                                        }
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(28.dp)
                                    .background(Color.White.copy(alpha = 0.12f))
                            )

                            TEXT_COLORS.forEach { hex ->
                                val color = Color(android.graphics.Color.parseColor(hex))
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable {
                                        onHighlight(HighlightData(selection.text, selectedHighlightColor, null, hex))
                                        showColorRow = false
                                        onClose()
                                        }.padding(3.dp)
                                ) {
                                    Text("A", color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Box(modifier = Modifier.width(16.dp).height(2.dp).background(color))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolbarItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean = false,
    activeColor: Color = Color.White,
    onClick: () -> Unit,
) {
    val color = if (isActive) activeColor else Color.White
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(if (isActive) Color.White.copy(alpha = 0.1f) else Color.Transparent)
            .padding(horizontal = 7.dp, vertical = 5.dp)
            .widthIn(min = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            color = if (isActive) activeColor else Color.White.copy(alpha = 0.78f),
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun ToolbarDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(30.dp)
            .background(Color.White.copy(alpha = 0.12f))
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteModal(
    selectedText: String,
    readerTheme: ReaderThemeColors,
    initialColor: String,
    onClose: () -> Unit,
    onConfirm: (note: String, color: String, textColor: String?) -> Unit,
) {
    var note by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(initialColor) }
    var selectedTextColor by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onClose,
        containerColor = readerTheme.cardBackground,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MiyoSpacing.large, vertical = MiyoSpacing.medium)
                .navigationBarsPadding(),
        ) {
            // Quote
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.width(4.dp).height(44.dp).clip(RoundedCornerShape(2.dp)).background(readerTheme.accent))
                Spacer(Modifier.width(10.dp))
                Text(
                    selectedText,
                    color = readerTheme.text,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    fontSize = 14.sp,
                    maxLines = 3,
                )
            }
            
            Spacer(Modifier.height(16.dp))

            // Input
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                placeholder = { Text("Enter your thoughts…", color = readerTheme.secondaryText) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = readerTheme.accent,
                    unfocusedBorderColor = readerTheme.secondaryText.copy(alpha = 0.3f),
                    focusedTextColor = readerTheme.text,
                    unfocusedTextColor = readerTheme.text,
                )
            )

            Spacer(Modifier.height(16.dp))

            // Colors
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                HIGHLIGHT_COLORS.forEach { hex ->
                    val color = Color(android.graphics.Color.parseColor(hex))
                    val isSelected = selectedColor == hex
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = if (isSelected) readerTheme.accent else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { selectedColor = hex }
                    )
                }

                Box(modifier = Modifier.width(1.dp).height(24.dp).background(readerTheme.secondaryText.copy(alpha = 0.25f)))

                TEXT_COLORS.forEach { hex ->
                    val color = Color(android.graphics.Color.parseColor(hex))
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            selectedTextColor = if (selectedTextColor == hex) null else hex
                        }.padding(4.dp)
                    ) {
                        Text(
                            "A", 
                            color = color, 
                            fontSize = 18.sp, 
                            fontWeight = if (selectedTextColor == hex) FontWeight.ExtraBold else FontWeight.SemiBold
                        )
                        Box(modifier = Modifier.width(16.dp).height(2.dp).background(color))
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onClose) {
                    Text("Cancel", color = readerTheme.secondaryText)
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { onConfirm(note, selectedColor, selectedTextColor) },
                    colors = ButtonDefaults.buttonColors(containerColor = readerTheme.accent)
                ) {
                    Text("Save Note", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
