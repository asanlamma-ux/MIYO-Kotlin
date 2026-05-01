package com.miyu.reader.ui.reader.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miyu.reader.domain.model.DictionaryLookupResult
import com.miyu.reader.domain.model.LookupSource
import com.miyu.reader.ui.theme.ReaderThemeColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryLookupBottomSheet(
    word: String,
    result: DictionaryLookupResult?,
    downloadedDictionaryCount: Int,
    isLoading: Boolean,
    readerTheme: ReaderThemeColors,
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
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.MenuBook, contentDescription = null, tint = readerTheme.accent)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Dictionary",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = readerTheme.text,
                    )
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
                    .padding(horizontal = 24.dp)
            ) {
                Text(
                    word,
                    color = readerTheme.text,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                )
                Spacer(Modifier.height(16.dp))

                if (isLoading) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 12.dp)) {
                        CircularProgressIndicator(color = readerTheme.accent, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(12.dp))
                        Text("Looking up…", color = readerTheme.secondaryText, fontSize = 14.sp)
                    }
                } else if (result != null) {
                    Text(
                        if (result.source == LookupSource.OFFLINE) "Offline · ${result.dictionaryName}" else "Online · ${result.dictionaryName}",
                        color = readerTheme.secondaryText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.4.sp,
                    )
                    Spacer(Modifier.height(14.dp))
                    result.entries.forEachIndexed { index, entry ->
                        if (index > 0) Spacer(Modifier.height(12.dp))
                        Surface(
                            color = readerTheme.background,
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(entry.term, color = readerTheme.text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                entry.partOfSpeech?.let {
                                    Spacer(Modifier.height(2.dp))
                                    Text(it, color = readerTheme.accent, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    entry.definition,
                                    color = readerTheme.text,
                                    fontSize = 15.sp,
                                    lineHeight = 24.sp,
                                )
                                entry.example?.let {
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "Example: $it",
                                        color = readerTheme.secondaryText,
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp,
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "No definition found.",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 15.sp,
                            lineHeight = 24.sp,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (downloadedDictionaryCount > 0) {
                                "Your offline dictionaries and the online fallback did not return a match."
                            } else {
                                "No offline dictionaries are installed yet, and the online fallback did not return a match."
                            },
                            color = readerTheme.secondaryText,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                        )
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
