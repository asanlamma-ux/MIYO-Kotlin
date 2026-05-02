package com.miyu.reader.ui.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miyu.reader.R
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
import com.miyu.reader.ui.theme.SpecialThemePreviewArt

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
        dailyGoalMinutes: Int,
    ) -> Unit,
) {
    val colors = LocalMIYUColors.current
    var setupStep by remember { mutableStateOf(SetupStep.LIBRARY) }
    var selectedThemeMode by remember { mutableStateOf(ThemeMode.SYSTEM) }
    var selectedReaderThemeId by remember {
        mutableStateOf(initialReaderThemeId.takeIf { it.isNotBlank() } ?: DefaultReaderThemeId)
    }
    var selectedMode by remember { mutableStateOf(ReadingModePreset.CONTINUOUS) }
    var marginPreset by remember { mutableStateOf(MarginPreset.MEDIUM) }
    var fontSize by remember { mutableFloatStateOf(18f) }
    var dailyGoalMinutes by remember { mutableIntStateOf(30) }
    var tapZonesEnabled by remember { mutableStateOf(true) }
    var continuousChapters by remember { mutableStateOf(true) }
    var bionicReading by remember { mutableStateOf(false) }

    fun saveSetup() {
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
            dailyGoalMinutes,
        )
    }

    ModalBottomSheet(
        onDismissRequest = {
            // Do not mark onboarding complete from incidental sheet dismissal.
            // The explicit Skip/Finish buttons are the only state-changing exits.
        },
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
            WizardHeader(step = setupStep)
            Spacer(Modifier.height(18.dp))
            SetupStepIndicator(current = setupStep)
            Spacer(Modifier.height(22.dp))

            Crossfade(targetState = setupStep, label = "initial-setup-step") { step ->
                when (step) {
                    SetupStep.LIBRARY -> LibrarySetupStep(
                        dailyGoalMinutes = dailyGoalMinutes,
                        onDailyGoalChange = { dailyGoalMinutes = it },
                    )
                    SetupStep.APPEARANCE -> AppearanceSetupStep(
                        selectedThemeMode = selectedThemeMode,
                        selectedReaderThemeId = selectedReaderThemeId,
                        onThemeModeSelected = { mode ->
                            selectedThemeMode = mode
                            onPreviewThemeMode(mode)
                        },
                        onReaderThemeSelected = { themeId ->
                            selectedReaderThemeId = themeId
                            onPreviewReaderTheme(themeId)
                        },
                    )
                    SetupStep.READER -> ReaderSetupStep(
                        selectedMode = selectedMode,
                        marginPreset = marginPreset,
                        fontSize = fontSize,
                        tapZonesEnabled = tapZonesEnabled,
                        continuousChapters = continuousChapters,
                        bionicReading = bionicReading,
                        onModeSelected = { preset ->
                            selectedMode = preset
                            continuousChapters = preset != ReadingModePreset.PAGED
                            tapZonesEnabled = preset != ReadingModePreset.MINIMAL
                        },
                        onMarginPresetSelected = { marginPreset = it },
                        onFontSizeChange = { fontSize = it.coerceIn(14f, 24f) },
                        onTapZonesChange = { tapZonesEnabled = it },
                        onContinuousChaptersChange = { continuousChapters = it },
                        onBionicReadingChange = { bionicReading = it },
                    )
                }
            }

            Spacer(Modifier.height(26.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        if (setupStep == SetupStep.LIBRARY) {
                            onSkip()
                        } else {
                            setupStep = setupStep.previous()
                        }
                    },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(if (setupStep == SetupStep.LIBRARY) "Skip" else "Back")
                }
                Button(
                    onClick = {
                        if (setupStep == SetupStep.READER) {
                            saveSetup()
                        } else {
                            setupStep = setupStep.next()
                        }
                    },
                    modifier = Modifier.weight(1.35f).height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(
                        if (setupStep == SetupStep.READER) "Finish Setup" else "Continue",
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

private enum class SetupStep(
    val index: Int,
    val title: String,
    val subtitle: String,
    @DrawableRes val iconRes: Int,
) {
    LIBRARY(
        index = 0,
        title = "Welcome to Miyo",
        subtitle = "A local-first reader with Koodo-style setup: library, palette, then reading controls.",
        iconRes = R.drawable.ic_setup_library_vault,
    ),
    APPEARANCE(
        index = 1,
        title = "Choose the look",
        subtitle = "Reader palettes also tune the app shell, so the preview changes immediately.",
        iconRes = R.drawable.ic_setup_palette_cards,
    ),
    READER(
        index = 2,
        title = "Tune reading",
        subtitle = "Set sane defaults for scroll mode, gestures, margins, and typography.",
        iconRes = R.drawable.ic_setup_reader_tuning,
    );

    fun next(): SetupStep = entries.getOrElse(index + 1) { READER }

    fun previous(): SetupStep = entries.getOrElse(index - 1) { LIBRARY }
}

@Composable
private fun WizardHeader(step: SetupStep) {
    val colors = LocalMIYUColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            color = colors.accent.copy(alpha = 0.14f),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.size(62.dp),
        ) {
            Icon(
                painter = painterResource(step.iconRes),
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.padding(14.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                step.title,
                color = colors.onBackground,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
            )
            Text(
                step.subtitle,
                color = colors.secondaryText,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 21.sp,
            )
        }
    }
}

@Composable
private fun SetupStepIndicator(current: SetupStep) {
    val colors = LocalMIYUColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SetupStep.entries.forEach { step ->
            val active = step.index <= current.index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(5.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (active) colors.accent else colors.secondaryText.copy(alpha = 0.18f),
                    ),
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun LibrarySetupStep(
    dailyGoalMinutes: Int,
    onDailyGoalChange: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SetupFeatureCard(
            iconRes = R.drawable.ic_setup_library_vault,
            title = "Local library first",
            subtitle = "EPUB files are imported into your device library; online catalogs stay optional.",
        )
        SetupFeatureCard(
            iconRes = R.drawable.ic_setup_private_device,
            title = "Private by default",
            subtitle = "Books, terms, bookmarks, and reading progress stay on-device unless you opt into sync later.",
        )
        SetupFeatureCard(
            iconRes = R.drawable.ic_setup_reader_tuning,
            title = "Reader-ready defaults",
            subtitle = "Continuous scroll, tap zones, and chapter append can be tuned before your first book.",
        )
        SetupSectionHeader("Daily reading goal", Icons.Outlined.WbSunny)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(15, 30, 45, 60).forEach { minutes ->
                FilterChip(
                    selected = dailyGoalMinutes == minutes,
                    onClick = { onDailyGoalChange(minutes) },
                    label = { Text("$minutes min") },
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AppearanceSetupStep(
    selectedThemeMode: ThemeMode,
    selectedReaderThemeId: String,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onReaderThemeSelected: (String) -> Unit,
) {
    Column {
        SetupSectionHeader("App theme", Icons.Outlined.Palette)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ThemeMode.entries.forEach { mode ->
                ElevatedFilterChip(
                    selected = selectedThemeMode == mode,
                    onClick = { onThemeModeSelected(mode) },
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
                    onClick = { onReaderThemeSelected(theme.id) },
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ReaderSetupStep(
    selectedMode: ReadingModePreset,
    marginPreset: MarginPreset,
    fontSize: Float,
    tapZonesEnabled: Boolean,
    continuousChapters: Boolean,
    bionicReading: Boolean,
    onModeSelected: (ReadingModePreset) -> Unit,
    onMarginPresetSelected: (MarginPreset) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onTapZonesChange: (Boolean) -> Unit,
    onContinuousChaptersChange: (Boolean) -> Unit,
    onBionicReadingChange: (Boolean) -> Unit,
) {
    Column {
        SetupSectionHeader("Reading mode", Icons.Outlined.WbSunny)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ReadingModePreset.entries.forEach { preset ->
                ModeChoice(
                    preset = preset,
                    selected = selectedMode == preset,
                    onClick = { onModeSelected(preset) },
                )
            }
        }

        Spacer(Modifier.height(22.dp))
        SetupSectionHeader("Layout", Icons.Outlined.FormatSize)
        Text(
            "Font size ${fontSize.toInt()}px",
            color = LocalMIYUColors.current.secondaryText,
            style = MaterialTheme.typography.labelLarge,
        )
        Slider(
            value = fontSize,
            onValueChange = onFontSizeChange,
            valueRange = 14f..24f,
            steps = 9,
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MarginPreset.entries.forEach { preset ->
                FilterChip(
                    selected = marginPreset == preset,
                    onClick = { onMarginPresetSelected(preset) },
                    label = { Text(preset.name.lowercase().replaceFirstChar { it.uppercase() }) },
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        SetupSwitchRow(
            title = "Tap zones",
            subtitle = "Use screen edges for reader movement.",
            checked = tapZonesEnabled,
            onCheckedChange = onTapZonesChange,
        )
        SetupSwitchRow(
            title = "Continuous chapters",
            subtitle = "Append the next chapter below the current one.",
            checked = continuousChapters,
            onCheckedChange = onContinuousChaptersChange,
        )
        SetupSwitchRow(
            title = "Bionic reading",
            subtitle = "Emphasize word beginnings for faster scanning.",
            checked = bionicReading,
            onCheckedChange = onBionicReadingChange,
        )
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
private fun SetupFeatureCard(
    @DrawableRes iconRes: Int,
    title: String,
    subtitle: String,
) {
    val colors = LocalMIYUColors.current
    Surface(
        color = colors.cardBackground,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = colors.accent.copy(alpha = 0.12f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.padding(11.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    color = colors.onBackground,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    subtitle,
                    color = colors.secondaryText,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp,
                )
            }
        }
    }
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
                SpecialThemePreviewArt(
                    theme = theme,
                    modifier = Modifier.matchParentSize(),
                )
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
                        if (theme.isSpecial) "Special" else if (theme.isDark) "Dark" else "Light",
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
