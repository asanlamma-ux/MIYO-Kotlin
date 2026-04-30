package com.miyu.reader.ui.reader.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Translate
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslationSheetBottomSheet(
    sourceText: String,
    readerTheme: ReaderThemeColors,
    onDismiss: () -> Unit,
) {
    var translatedText by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(sourceText) {
        isLoading = true
        translatedText = null
        // Simulate translation delay.
        // In a real implementation, this would call a Translation API or ViewModel
        delay(1200)
        translatedText = "[Simulated Translation]: ${sourceText.take(100)}..."
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
                    Icon(Icons.Outlined.Translate, contentDescription = null, tint = readerTheme.accent)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Translation",
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
                    "Original",
                    color = readerTheme.secondaryText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.4.sp,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    sourceText,
                    color = readerTheme.text,
                    fontSize = 15.sp,
                    lineHeight = 24.sp,
                )

                Spacer(Modifier.height(24.dp))

                Text(
                    "English",
                    color = readerTheme.secondaryText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.4.sp,
                )
                Spacer(Modifier.height(8.dp))

                if (isLoading) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 12.dp)) {
                        CircularProgressIndicator(color = readerTheme.accent, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(12.dp))
                        Text("Translating…", color = readerTheme.secondaryText, fontSize = 14.sp)
                    }
                } else if (translatedText != null) {
                    Text(
                        translatedText!!,
                        color = readerTheme.text,
                        fontSize = 15.sp,
                        lineHeight = 24.sp,
                    )
                } else {
                    Text(
                        "Translation unavailable.",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 15.sp,
                        lineHeight = 24.sp,
                    )
                }

                Spacer(Modifier.height(32.dp))

                Text(
                    "Free translation service. Limit applies to long text.",
                    color = readerTheme.secondaryText.copy(alpha = 0.75f),
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                )
            }
        }
    }
}
