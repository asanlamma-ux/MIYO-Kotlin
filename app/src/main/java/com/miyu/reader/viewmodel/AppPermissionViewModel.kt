package com.miyu.reader.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miyu.reader.data.preferences.UserPreferences
import com.miyu.reader.permissions.MiyoPermissionSnapshot
import com.miyu.reader.permissions.MiyoPermissions
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppPermissionUiState(
    val snapshot: MiyoPermissionSnapshot,
    val storageAutoRedirectComplete: Boolean = false,
)

@HiltViewModel
class AppPermissionViewModel @Inject constructor(
    private val preferences: UserPreferences,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        AppPermissionUiState(snapshot = MiyoPermissions.snapshot(appContext)),
    )
    val uiState: StateFlow<AppPermissionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferences.storagePermissionAutoRedirectComplete.collect { complete ->
                _uiState.update { it.copy(storageAutoRedirectComplete = complete) }
            }
        }
    }

    fun refresh() {
        _uiState.update { it.copy(snapshot = MiyoPermissions.snapshot(appContext)) }
    }

    fun markStorageAutoRedirectComplete() {
        viewModelScope.launch {
            preferences.setStoragePermissionAutoRedirectComplete(true)
        }
    }
}
