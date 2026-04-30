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
import com.miyu.reader.ui.theme.ReaderThemeColors
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryLookupBottomSheet(
    word: String,
    readerTheme: ReaderThemeColors,
    onDismiss: () -> Unit,
) {
    var definition by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(word) {
        isLoading = true
        definition = null
        // Simulate dictionary lookup delay
        delay(800)
        definition = """
            1. n. A simulated definition for "$word".
            2. v. To test the dictionary UI implementation.
            
            "The developer implemented the $word feature successfully."
        """.trimIndent()
        isLoading = false
    }

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
                } else if (definition != null) {
                    Text(
                        definition!!,
                        color = readerTheme.text,
                        fontSize = 15.sp,
                        lineHeight = 24.sp,
                    )
                } else {
                    Text(
                        "No definition found.",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 15.sp,
                        lineHeight = 24.sp,
                    )
                }

                Spacer(Modifier.height(32.dp))

                Text(
                    "Uses offline dictionary packages. (Simulated)",
                    color = readerTheme.secondaryText.copy(alpha = 0.75f),
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                )
            }
        }
    }
}
