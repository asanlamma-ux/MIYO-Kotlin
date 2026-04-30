package com.miyu.reader.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miyu.reader.data.preferences.UserPreferences
import com.miyu.reader.domain.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val preferences: UserPreferences,
) : ViewModel() {
    val themeMode: Flow<ThemeMode> = preferences.themeMode

    val readerThemeId: Flow<String> = preferences.readerThemeId

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferences.setThemeMode(mode) }
    }

    fun setReaderThemeId(id: String) {
        viewModelScope.launch { preferences.setReaderThemeId(id) }
    }
}
