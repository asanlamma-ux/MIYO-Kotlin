package com.miyu.reader.ui.library

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import com.miyu.reader.ui.core.components.MiyoWorkspaceExitButton
import com.miyu.reader.ui.core.components.MiyoWorkspaceSurface

@Composable
internal fun LibraryWorkspaceSurface(
    content: @Composable ColumnScope.() -> Unit,
) {
    MiyoWorkspaceSurface(content = content)
}

@Composable
internal fun WorkspaceExitButton(
    label: String,
    onClick: () -> Unit,
) {
    MiyoWorkspaceExitButton(label = label, onClick = onClick)
}
