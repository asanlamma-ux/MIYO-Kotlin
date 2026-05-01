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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslationSheetBottomSheet(
    sourceText: String,
    status: String?,
    readerTheme: ReaderThemeColors,
    onOpenExternal: () -> Unit,
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
                Text(
                    status ?: "Translation unavailable.",
                    color = readerTheme.text,
                    fontSize = 15.sp,
                    lineHeight = 24.sp,
                )

                Spacer(Modifier.height(32.dp))

                Button(
                    onClick = onOpenExternal,
                    colors = ButtonDefaults.buttonColors(containerColor = readerTheme.accent),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Open in Google Translate", color = readerTheme.background, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "In-app translation uses the same MyMemory public endpoint as the React Native app. No API key is required; avoid sending sensitive text.",
                    color = readerTheme.secondaryText.copy(alpha = 0.75f),
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                )
            }
        }
    }
}
