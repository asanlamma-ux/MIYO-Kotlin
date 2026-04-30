package com.miyu.reader.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miyu.reader.data.preferences.UserPreferences
import com.miyu.reader.data.repository.BookRepository
import com.miyu.reader.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val typography: TypographySettings = TypographySettings(),
    val readingSettings: ReadingSettings = ReadingSettings(),
    val readerThemeId: String = "sepia-classic",
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val bookCount: Int = 0,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: UserPreferences,
    bookRepository: BookRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferences.typography.collect { typo ->
                _uiState.update { it.copy(typography = typo) }
            }
        }
        viewModelScope.launch {
            preferences.readingSettings.collect { rs ->
                _uiState.update { it.copy(readingSettings = rs) }
            }
        }
        viewModelScope.launch {
            preferences.readerThemeId.collect { id ->
                _uiState.update { it.copy(readerThemeId = id) }
            }
        }
        viewModelScope.launch {
            preferences.themeMode.collect { mode ->
                _uiState.update { it.copy(themeMode = mode) }
            }
        }
        viewModelScope.launch {
            bookRepository.getAllBooks().collect { books ->
                _uiState.update { it.copy(bookCount = books.size) }
            }
        }
    }

    fun setFontSize(size: Float) {
        viewModelScope.launch {
            val current = _uiState.value.typography
            preferences.setTypography(current.copy(fontSize = size.coerceIn(12f, 28f)))
        }
    }

    fun setLineHeight(height: Float) {
        viewModelScope.launch {
            val current = _uiState.value.typography
            preferences.setTypography(current.copy(lineHeight = height.coerceIn(1.2f, 2.0f)))
        }
    }

    fun setTextAlign(align: TextAlign) {
        viewModelScope.launch {
            val current = _uiState.value.typography
            preferences.setTypography(current.copy(textAlign = align))
        }
    }

    fun setPageAnimation(animation: PageAnimation) {
        viewModelScope.launch {
            val current = _uiState.value.readingSettings
            preferences.setReadingSettings(current.copy(pageAnimation = animation))
        }
    }

    fun setTapZonesEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = _uiState.value.readingSettings
            preferences.setReadingSettings(current.copy(tapZonesEnabled = enabled))
        }
    }

    fun setVolumeButtonPageTurn(enabled: Boolean) {
        viewModelScope.launch {
            val current = _uiState.value.readingSettings
            preferences.setReadingSettings(current.copy(volumeButtonPageTurn = enabled))
        }
    }

    fun setImmersiveMode(enabled: Boolean) {
        viewModelScope.launch {
            val current = _uiState.value.readingSettings
            preferences.setReadingSettings(current.copy(immersiveMode = enabled))
        }
    }

    fun setMarginPreset(preset: MarginPreset) {
        viewModelScope.launch {
            val current = _uiState.value.readingSettings
            preferences.setReadingSettings(current.copy(marginPreset = preset))
        }
    }

    fun setReaderThemeId(id: String) {
        viewModelScope.launch { preferences.setReaderThemeId(id) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferences.setThemeMode(mode) }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            preferences.setTypography(TypographySettings())
            preferences.setReadingSettings(ReadingSettings())
        }
    }
}
