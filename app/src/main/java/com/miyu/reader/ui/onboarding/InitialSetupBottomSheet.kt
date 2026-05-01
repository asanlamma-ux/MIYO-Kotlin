package com.miyu.reader.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.SettingsSuggest
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miyu.reader.domain.model.MarginPreset
import com.miyu.reader.domain.model.PageAnimation
import com.miyu.reader.domain.model.ReaderFlowMode
import com.miyu.reader.domain.model.ReadingSettings
import com.miyu.reader.domain.model.TapZoneNavMode
import com.miyu.reader.domain.model.ThemeMode
import com.miyu.reader.domain.model.TypographySettings
import com.miyu.reader.ui.theme.DefaultReaderThemeId
import com.miyu.reader.ui.theme.LocalMIYUColors
import com.miyu.reader.ui.theme.ReaderColors
import com.miyu.reader.ui.theme.ReaderThemeColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InitialSetupBottomSheet(
    initialReaderThemeId: String,
    onSkip: () -> Unit,
    onPreviewThemeMode: (ThemeMode) -> Unit,
    onPreviewReaderTheme: (String) -> Unit,
    onSave: (
        themeMode: ThemeMode,
        readerThemeId: String,
        readingSettings: ReadingSettings,
        typography: TypographySettings,
    ) -> Unit,
) {
    val colors = LocalMIYUColors.current
    var selectedThemeMode by remember { mutableStateOf(ThemeMode.SYSTEM) }
    var selectedReaderThemeId by remember {
        mutableStateOf(initialReaderThemeId.takeIf { it.isNotBlank() } ?: DefaultReaderThemeId)
    }
    var selectedMode by remember { mutableStateOf(ReadingModePreset.CONTINUOUS) }
    var marginPreset by remember { mutableStateOf(MarginPreset.MEDIUM) }
    var fontSize by remember { mutableFloatStateOf(18f) }
    var tapZonesEnabled by remember { mutableStateOf(true) }
    var continuousChapters by remember { mutableStateOf(true) }
    var bionicReading by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onSkip,
        containerColor = colors.background,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 8.dp)
                    .size(width = 44.dp, height = 4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(colors.secondaryText.copy(alpha = 0.36f)),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = colors.accent.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.size(58.dp),
                ) {
                    Icon(
                        Icons.Outlined.SettingsSuggest,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.padding(15.dp),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        "Set Up Miyo",
                        color = colors.onBackground,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                    )
                    Text(
                        "Pick your defaults now, or skip and change them later in Settings.",
                        color = colors.secondaryText,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 21.sp,
                    )
                }
            }

            Spacer(Modifier.height(22.dp))
            SetupSectionHeader("App theme", Icons.Outlined.Palette)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    ElevatedFilterChip(
                        selected = selectedThemeMode == mode,
                        onClick = {
                            selectedThemeMode = mode
                            onPreviewThemeMode(mode)
                        },
                        label = {
                            Text(
                                when (mode) {
                                    ThemeMode.SYSTEM -> "Follow OS"
                                    ThemeMode.LIGHT -> "Light"
                                    ThemeMode.DARK -> "Dark"
                                },
                            )
                        },
                        leadingIcon = if (selectedThemeMode == mode) {
                            { Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else {
                            null
                        },
                    )
                }
            }

            Spacer(Modifier.height(22.dp))
            SetupSectionHeader("Reader palette", Icons.Outlined.AutoStories)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ReaderColors.allThemes.forEach { theme ->
                    ReaderThemeChoice(
                        theme = theme,
                        selected = selectedReaderThemeId == theme.id,
                        onClick = {
                            selectedReaderThemeId = theme.id
                            onPreviewReaderTheme(theme.id)
                        },
                    )
                }
            }

            Spacer(Modifier.height(22.dp))
            SetupSectionHeader("Reading mode", Icons.Outlined.WbSunny)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ReadingModePreset.entries.forEach { preset ->
                    ModeChoice(
                        preset = preset,
                        selected = selectedMode == preset,
                        onClick = {
                            selectedMode = preset
                            continuousChapters = preset != ReadingModePreset.PAGED
                            tapZonesEnabled = preset != ReadingModePreset.MINIMAL
                        },
                    )
                }
            }

            Spacer(Modifier.height(22.dp))
            SetupSectionHeader("Layout", Icons.Outlined.FormatSize)
            Text(
                "Font size ${fontSize.toInt()}px",
                color = colors.secondaryText,
                style = MaterialTheme.typography.labelLarge,
            )
            Slider(
                value = fontSize,
                onValueChange = { fontSize = it.coerceIn(14f, 24f) },
                valueRange = 14f..24f,
                steps = 9,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MarginPreset.entries.forEach { preset ->
                    FilterChip(
                        selected = marginPreset == preset,
                        onClick = { marginPreset = preset },
                        label = { Text(preset.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            SetupSwitchRow(
                title = "Tap zones",
                subtitle = "Use screen edges for reader navigation.",
                checked = tapZonesEnabled,
                onCheckedChange = { tapZonesEnabled = it },
            )
            SetupSwitchRow(
                title = "Continuous chapters",
                subtitle = "Load the next chapter below the current chapter.",
                checked = continuousChapters,
                onCheckedChange = { continuousChapters = it },
            )
            SetupSwitchRow(
                title = "Bionic reading",
                subtitle = "Emphasize word beginnings for faster scanning.",
                checked = bionicReading,
                onCheckedChange = { bionicReading = it },
            )

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    val readingSettings = selectedMode.settings.copy(
                        tapZonesEnabled = tapZonesEnabled,
                        autoAdvanceChapter = continuousChapters,
                        bionicReading = bionicReading,
                        marginPreset = marginPreset,
                    )
                    onSave(
                        selectedThemeMode,
                        selectedReaderThemeId,
                        readingSettings,
                        TypographySettings(fontSize = fontSize),
                    )
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(18.dp),
            ) {
                Text("Use These Settings", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(18.dp),
            ) {
                Text("Skip, Use Defaults")
            }
        }
    }
}

private enum class ReadingModePreset(
    val title: String,
    val subtitle: String,
    val settings: ReadingSettings,
) {
    CONTINUOUS(
        title = "Continuous scroll",
        subtitle = "Webnovel-style reading with next chapter appended below.",
        settings = ReadingSettings(
            readingFlowMode = ReaderFlowMode.SCROLL,
            pageAnimation = PageAnimation.SLIDE,
            tapZoneNavMode = TapZoneNavMode.SCROLL,
            autoAdvanceChapter = true,
        ),
    ),
    PAGED(
        title = "Paged taps",
        subtitle = "Edge taps move between chapters; no auto-append.",
        settings = ReadingSettings(
            readingFlowMode = ReaderFlowMode.PAGED,
            pageAnimation = PageAnimation.SLIDE,
            tapZoneNavMode = TapZoneNavMode.CHAPTER,
            autoAdvanceChapter = false,
        ),
    ),
    MINIMAL(
        title = "Quiet reading",
        subtitle = "Reduced motion with fewer accidental reader controls.",
        settings = ReadingSettings(
            readingFlowMode = ReaderFlowMode.SCROLL,
            pageAnimation = PageAnimation.NONE,
            tapZonesEnabled = false,
            reducedMotion = true,
            autoAdvanceChapter = true,
        ),
    ),
}

@Composable
private fun SetupSectionHeader(title: String, icon: ImageVector) {
    val colors = LocalMIYUColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 10.dp),
    ) {
        Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(20.dp))
        Text(
            title.uppercase(),
            color = colors.secondaryText,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderThemeChoice(
    theme: ReaderThemeColors,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalMIYUColors.current
    Surface(
        onClick = onClick,
        color = theme.cardBackground,
        shape = RoundedCornerShape(18.dp),
        shadowElevation = if (selected) 5.dp else 1.dp,
        modifier = Modifier
            .width(150.dp)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) theme.accent else colors.secondaryText.copy(alpha = 0.16f),
                shape = RoundedCornerShape(18.dp),
            ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(74.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(theme.background),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ReaderPreviewLine(theme.text, 72)
                    ReaderPreviewLine(theme.secondaryText, 96)
                    ReaderPreviewLine(theme.secondaryText, 64)
                    ReaderPreviewLine(theme.accent, 36)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        theme.name,
                        color = theme.text,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                    )
                    Text(
                        if (theme.isDark) "Dark" else "Light",
                        color = theme.secondaryText,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                if (selected) {
                    Surface(color = theme.accent, shape = CircleShape, modifier = Modifier.size(26.dp)) {
                        Icon(
                            Icons.Outlined.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.padding(5.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReaderPreviewLine(color: Color, width: Int) {
    Box(
        modifier = Modifier
            .width(width.dp)
            .height(5.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.78f)),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModeChoice(
    preset: ReadingModePreset,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalMIYUColors.current
    Surface(
        onClick = onClick,
        color = if (selected) colors.accent.copy(alpha = 0.12f) else colors.cardBackground,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) colors.accent else colors.secondaryText.copy(alpha = 0.16f),
                shape = RoundedCornerShape(18.dp),
            ),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    preset.title,
                    color = colors.onBackground,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    preset.subtitle,
                    color = colors.secondaryText,
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 18.sp,
                )
            }
            if (selected) {
                Icon(Icons.Outlined.Check, contentDescription = null, tint = colors.accent)
            }
        }
    }
}

@Composable
private fun SetupSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colors = LocalMIYUColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = colors.onBackground,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            )
            Text(
                subtitle,
                color = colors.secondaryText,
                style = MaterialTheme.typography.bodySmall,
                lineHeight = 18.sp,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
