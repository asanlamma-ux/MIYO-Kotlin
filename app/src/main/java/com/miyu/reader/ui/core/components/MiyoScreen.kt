package com.miyu.reader.ui.core.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.miyu.reader.ui.core.theme.MiyoSpacing
import com.miyu.reader.ui.theme.LocalMIYUColors

@Composable
fun MiyoScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    subtitle: String? = null,
    count: Int? = null,
    actions: @Composable () -> Unit = {},
) {
    val colors = LocalMIYUColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MiyoSpacing.large, vertical = MiyoSpacing.small),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            eyebrow?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.secondaryText,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    color = colors.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                count?.let {
                    Spacer(Modifier.width(MiyoSpacing.small))
                    Surface(
                        shape = CircleShape,
                        color = colors.cardBackground.copy(alpha = 0.88f),
                        shadowElevation = 1.dp,
                    ) {
                        Text(
                            text = it.toString(),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color = colors.secondaryText,
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
            subtitle?.let {
                Spacer(Modifier.height(MiyoSpacing.extraSmall))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.secondaryText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        actions()
    }
}

@Composable
fun MiyoIconActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMIYUColors.current
    FilledTonalIconButton(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = colors.accent)
    }
}

@Composable
fun MiyoEmptyScreen(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = LocalMIYUColors.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MiyoSpacing.large),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = CircleShape,
                color = colors.accent.copy(alpha = 0.13f),
                modifier = Modifier.size(72.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
            Spacer(Modifier.height(MiyoSpacing.medium))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = colors.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(MiyoSpacing.small))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.secondaryText,
                textAlign = TextAlign.Center,
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(MiyoSpacing.large))
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = MiyoSpacing.large, vertical = 12.dp),
                ) {
                    Text(actionLabel, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun MiyoWorkspaceSurface(
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
                .padding(horizontal = MiyoSpacing.large, vertical = MiyoSpacing.medium),
            content = content,
        )
    }
}

@Composable
fun MiyoWorkspaceExitButton(
    label: String,
    onClick: () -> Unit,
) {
    val colors = LocalMIYUColors.current
    TextButton(
        onClick = onClick,
        modifier = Modifier.padding(bottom = MiyoSpacing.small),
        contentPadding = PaddingValues(horizontal = 0.dp, vertical = MiyoSpacing.small),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = null,
            tint = colors.secondaryText,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = label,
            color = colors.secondaryText,
            textDecoration = TextDecoration.Underline,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
