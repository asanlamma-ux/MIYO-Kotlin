package com.miyu.reader.ui.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.miyu.reader.ui.theme.LocalMIYUColors

@Composable
internal fun LibraryWorkspaceSurface(
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalMIYUColors.current
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colors.background.copy(alpha = if (colors.isDark) 0.985f else 0.97f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            content = content,
        )
    }
}

@Composable
internal fun WorkspaceExitButton(
    label: String,
    onClick: () -> Unit,
) {
    val colors = LocalMIYUColors.current
    TextButton(
        onClick = onClick,
        modifier = Modifier.padding(bottom = 8.dp),
        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp),
    ) {
        Text(
            "> $label",
            color = colors.secondaryText,
            textDecoration = TextDecoration.Underline,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
