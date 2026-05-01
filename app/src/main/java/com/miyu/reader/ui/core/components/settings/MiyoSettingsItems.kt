package com.miyu.reader.ui.core.components.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miyu.reader.ui.theme.LocalMIYUColors

@Composable
fun MiyoSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMIYUColors.current
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
        color = colors.secondaryText,
        modifier = modifier.padding(top = 4.dp, bottom = 8.dp),
    )
}

@Composable
fun MiyoSettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalMIYUColors.current
    Column(modifier = modifier.padding(top = 8.dp)) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            ),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            color = colors.secondaryText,
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = colors.cardBackground.copy(alpha = 0.92f)),
            elevation = CardDefaults.cardElevation(defaultElevation = if (colors.isDark) 0.dp else 2.dp),
        ) {
            Column { content() }
        }
    }
}

@Composable
fun MiyoSettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    accentColor: Color = LocalMIYUColors.current.accent,
    trailing: @Composable (() -> Unit)? = null,
) {
    val colors = LocalMIYUColors.current
    ListItem(
        headlineContent = {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = colors.onBackground,
            )
        },
        supportingContent = {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.secondaryText)
        },
        leadingContent = {
            Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
        },
        trailingContent = trailing ?: {
            if (onClick != null) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowForwardIos,
                    contentDescription = null,
                    tint = colors.secondaryText,
                    modifier = Modifier.size(14.dp),
                )
            }
        },
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
    )
}

@Composable
fun MiyoSettingsSwitch(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accentColor: Color = LocalMIYUColors.current.accent,
) {
    val colors = LocalMIYUColors.current
    ListItem(
        headlineContent = {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = colors.onBackground,
            )
        },
        supportingContent = {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.secondaryText)
        },
        leadingContent = {
            Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(checkedTrackColor = accentColor),
            )
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> MiyoExpandableChoiceSetting(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    currentValue: String,
    choices: List<T>,
    choiceLabel: (T) -> String,
    selectedChoice: T,
    onChoiceSelected: (T) -> Unit,
) {
    val colors = LocalMIYUColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
    ) {
        MiyoSettingsItem(
            icon = icon,
            title = title,
            subtitle = subtitle,
            onClick = { onExpandedChange(!expanded) },
            accentColor = accentColor,
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(currentValue, color = colors.secondaryText, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowForwardIos,
                        contentDescription = null,
                        tint = colors.secondaryText,
                        modifier = Modifier
                            .size(14.dp)
                            .rotate(if (expanded) 90f else 0f),
                    )
                }
            },
        )
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 58.dp, end = 18.dp, bottom = 16.dp),
                shape = RoundedCornerShape(18.dp),
                color = colors.background.copy(alpha = if (colors.isDark) 0.38f else 0.58f),
            ) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                ) {
                    choices.forEach { choice ->
                        FilterChip(
                            selected = selectedChoice == choice,
                            onClick = {
                                onChoiceSelected(choice)
                                onExpandedChange(false)
                            },
                            label = { Text(choiceLabel(choice), fontWeight = FontWeight.SemiBold) },
                        )
                    }
                }
            }
        }
    }
}
