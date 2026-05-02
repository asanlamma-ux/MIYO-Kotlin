package com.miyu.reader.ui.core.components.material

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.miyu.reader.ui.core.theme.MiyoSpacing
import com.miyu.reader.ui.theme.LocalMIYUColors

@Composable
fun MiyoNavigationBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = LocalMIYUColors.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = colors.cardBackground.copy(alpha = if (colors.isDark) 0.92f else 0.96f),
        tonalElevation = 0.dp,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(56.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

@Composable
fun RowScope.MiyoNavigationBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    icon: @Composable (Color) -> Unit,
) {
    val colors = LocalMIYUColors.current
    val iconColor by animateColorAsState(
        targetValue = if (selected) colors.accent else colors.secondaryText,
        label = "navIconColor",
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) colors.onBackground else colors.secondaryText,
        label = "navLabelColor",
    )
    val indicatorColor by animateColorAsState(
        targetValue = if (selected) colors.accent else Color.Transparent,
        label = "navIndicatorColor",
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(18.dp))
            .selectable(
                selected = selected,
                role = Role.Tab,
                onClick = onClick,
            )
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .padding(bottom = 3.dp)
                .size(width = 18.dp, height = 3.dp)
                .clip(RoundedCornerShape(50))
                .background(indicatorColor),
        )
        icon(iconColor)
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = labelColor,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
