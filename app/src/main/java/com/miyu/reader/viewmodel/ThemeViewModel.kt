package com.miyu.reader.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miyu.reader.data.preferences.UserPreferences
import com.miyu.reader.domain.model.ReadingSettings
import com.miyu.reader.domain.model.ThemeMode
import com.miyu.reader.domain.model.TypographySettings
import com.miyu.reader.ui.theme.DefaultReaderThemeId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val preferences: UserPreferences,
) : ViewModel() {
    val themeMode: Flow<ThemeMode> = preferences.themeMode

    val readerThemeId: Flow<String> = preferences.readerThemeId

    val shouldShowInitialSetup: Flow<Boolean> = preferences.shouldShowInitialSetup

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferences.setThemeMode(mode) }
    }

    fun setReaderThemeId(id: String) {
        viewModelScope.launch { preferences.setReaderThemeId(id) }
    }

    fun saveInitialSetup(
        themeMode: ThemeMode,
        readerThemeId: String,
        readingSettings: ReadingSettings,
        typography: TypographySettings,
    ) {
        viewModelScope.launch {
            preferences.setThemeMode(themeMode)
            preferences.setReaderThemeId(readerThemeId)
            preferences.setReadingSettings(readingSettings)
            preferences.setTypography(typography)
            preferences.setInitialSetupComplete(true)
        }
    }

    fun skipInitialSetup() {
        viewModelScope.launch {
            preferences.setThemeMode(ThemeMode.SYSTEM)
            preferences.setReaderThemeId(DefaultReaderThemeId)
            preferences.setInitialSetupComplete(true)
        }
    }
}
