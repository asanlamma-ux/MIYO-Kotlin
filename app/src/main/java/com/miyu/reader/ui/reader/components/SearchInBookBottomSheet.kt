package com.miyu.reader.ui.reader.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miyu.reader.ui.theme.ReaderThemeColors
import kotlinx.coroutines.delay

data class SearchResult(
    val chapterIndex: Int,
    val chapterTitle: String,
    val excerptBefore: String,
    val excerptMatch: String,
    val excerptAfter: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchInBookBottomSheet(
    currentChapterIndex: Int,
    readerTheme: ReaderThemeColors,
    onGoToChapter: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var searchFullBook by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    // Debounced Search Mock
    LaunchedEffect(query, searchFullBook) {
        if (query.trim().length < 2) {
            results = emptyList()
            isSearching = false
            return@LaunchedEffect
        }
        isSearching = true
        delay(400) // debounce
        
        // Mock search results
        val simulatedResults = mutableListOf<SearchResult>()
        val startCi = if (searchFullBook) 0 else currentChapterIndex
        val endCi = if (searchFullBook) startCi + 5 else startCi + 1
        
        for (ci in startCi until endCi) {
            simulatedResults.add(
                SearchResult(
                    chapterIndex = ci,
                    chapterTitle = "Chapter ${ci + 1}",
                    excerptBefore = "...this is a simulated search result for ",
                    excerptMatch = query,
                    excerptAfter = " and it works correctly..."
                )
            )
        }
        results = simulatedResults
        isSearching = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = readerTheme.cardBackground,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
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
                Text(
                    "Search in Book",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = readerTheme.text,
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = readerTheme.secondaryText)
                }
            }

            // Scope Toggle
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("This chapter", !searchFullBook, readerTheme) { searchFullBook = false }
                Chip("Entire book", searchFullBook, readerTheme) { searchFullBook = true }
            }

            // Search Input
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                placeholder = { Text("Search for words, phrases…", color = readerTheme.secondaryText.copy(alpha = 0.8f)) },
                leadingIcon = { Icon(Icons.Outlined.Search, null, tint = readerTheme.secondaryText) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, "Clear", tint = readerTheme.secondaryText)
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = readerTheme.secondaryText.copy(alpha = 0.5f),
                    unfocusedBorderColor = readerTheme.secondaryText.copy(alpha = 0.2f),
                    focusedTextColor = readerTheme.text,
                    unfocusedTextColor = readerTheme.text,
                    cursorColor = readerTheme.accent
                ),
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                singleLine = true,
            )

            // Meta
            if (query.length >= 2) {
                Text(
                    if (isSearching) "Searching…" else if (results.isEmpty()) "No results found" else "${results.size} result${if (results.size != 1) "s" else ""}",
                    color = readerTheme.secondaryText,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
            }

            // Results List
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (query.length >= 2 && !isSearching && results.isEmpty()) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No matches for \"$query\"", color = readerTheme.secondaryText, fontSize = 15.sp)
                            Spacer(Modifier.height(4.dp))
                            Text("Try different keywords", color = readerTheme.secondaryText, fontSize = 12.sp)
                        }
                    }
                }
                
                items(results) { result ->
                    val isCurrent = result.chapterIndex == currentChapterIndex
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isCurrent) readerTheme.accent.copy(alpha = 0.1f) else readerTheme.background)
                            .border(
                                1.dp,
                                if (isCurrent) readerTheme.accent.copy(alpha = 0.3f) else readerTheme.secondaryText.copy(alpha = 0.15f),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                onGoToChapter(result.chapterIndex)
                                onDismiss()
                            }
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(result.chapterTitle, color = readerTheme.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            if (isCurrent) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(readerTheme.accent.copy(alpha = 0.2f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("Current", color = readerTheme.accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        
                        Text(
                            buildAnnotatedString {
                                append(result.excerptBefore)
                                withStyle(SpanStyle(background = readerTheme.accent.copy(alpha = 0.35f), color = readerTheme.text, fontWeight = FontWeight.Bold)) {
                                    append(result.excerptMatch)
                                }
                                append(result.excerptAfter)
                            },
                            color = readerTheme.text,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Chip(label: String, active: Boolean, readerTheme: ReaderThemeColors, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) readerTheme.accent.copy(alpha = 0.22f) else readerTheme.background)
            .border(
                width = 1.5.dp,
                color = if (active) readerTheme.accent else readerTheme.secondaryText.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            label,
            color = if (active) readerTheme.accent else readerTheme.text,
            fontSize = 12.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}
