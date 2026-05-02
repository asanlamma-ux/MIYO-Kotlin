package com.miyu.reader.ui.core.components.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miyu.reader.ui.core.theme.MiyoSettingsPaddings
import com.miyu.reader.ui.core.theme.MiyoSpacing
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
        modifier = modifier.padding(top = MiyoSpacing.extraSmall, bottom = MiyoSpacing.small),
    )
}

@Composable
fun MiyoSettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalMIYUColors.current
    Column(modifier = modifier.padding(top = MiyoSpacing.extraSmall)) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
            ),
            modifier = Modifier.padding(
                horizontal = MiyoSettingsPaddings.horizontal,
                vertical = MiyoSpacing.extraSmall,
            ),
            color = colors.onBackground,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(),
        ) {
            content()
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = MiyoSettingsPaddings.horizontal + 34.dp),
            color = colors.secondaryText.copy(alpha = 0.12f),
        )
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
    showNavigationArrow: Boolean = true,
) {
    val colors = LocalMIYUColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(
                horizontal = MiyoSettingsPaddings.horizontal,
                vertical = MiyoSettingsPaddings.vertical,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = colors.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.secondaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (trailing != null) {
            Box(
                modifier = Modifier.widthIn(max = 132.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                trailing()
            }
        }
        if (onClick != null && showNavigationArrow) {
            Icon(
                Icons.AutoMirrored.Outlined.ArrowForwardIos,
                contentDescription = null,
                tint = colors.secondaryText,
                modifier = Modifier.size(14.dp),
            )
        }
    }
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(
                horizontal = MiyoSettingsPaddings.horizontal,
                vertical = MiyoSettingsPaddings.vertical,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = colors.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.secondaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = accentColor),
        )
    }
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
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        label = "settings_choice_arrow",
    )
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
                    Text(
                        currentValue,
                        color = colors.secondaryText,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End,
                    )
                    Spacer(Modifier.width(MiyoSpacing.small))
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowForwardIos,
                        contentDescription = null,
                        tint = colors.secondaryText,
                        modifier = Modifier
                            .size(14.dp)
                            .rotate(arrowRotation),
                    )
                }
            },
            showNavigationArrow = false,
        )
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 56.dp,
                        end = MiyoSettingsPaddings.horizontal,
                        bottom = MiyoSpacing.medium,
                    ),
                shape = RoundedCornerShape(MiyoSpacing.large),
                color = colors.background.copy(alpha = if (colors.isDark) 0.38f else 0.58f),
            ) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MiyoSpacing.medium),
                    horizontalArrangement = Arrangement.spacedBy(MiyoSpacing.small),
                    verticalArrangement = Arrangement.spacedBy(MiyoSpacing.small),
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
