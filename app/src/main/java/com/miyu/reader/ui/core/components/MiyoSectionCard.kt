package com.miyu.reader.ui.core.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.miyu.reader.ui.core.theme.MiyoSpacing
import com.miyu.reader.ui.theme.LocalMIYUColors

@Composable
fun MiyoSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalMIYUColors.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MiyoSpacing.medium, vertical = MiyoSpacing.small),
        shape = RoundedCornerShape(MiyoSpacing.extraLarge),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground.copy(alpha = 0.94f)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (colors.isDark) 0.dp else 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MiyoSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(MiyoSpacing.small),
            content = {
                Text(
                    title,
                    color = colors.onBackground,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
                content()
            },
        )
    }
}
