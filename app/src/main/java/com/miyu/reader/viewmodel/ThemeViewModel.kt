package com.miyu.reader.viewmodel

import androidx.lifecycle.ViewModel
<<<<<<< HEAD
=======
import androidx.lifecycle.viewModelScope
>>>>>>> debug
import com.miyu.reader.data.preferences.UserPreferences
import com.miyu.reader.domain.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
<<<<<<< HEAD
=======
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
>>>>>>> debug
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
<<<<<<< HEAD
    preferences: UserPreferences,
) : ViewModel() {
    val themeMode: Flow<ThemeMode> = preferences.themeMode
=======
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
>>>>>>> debug
}