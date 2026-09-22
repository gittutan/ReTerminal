package com.rk.terminal.ui.screens.terminal

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.rk.resources.strings
import com.rk.terminal.service.SessionService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalTopBar(
    sessionBinder: SessionService.SessionBinder?,
    onMenuClick: () -> Unit,
    onAddClick: () -> Unit,
    color: Color
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent
        ),
        title = {
            Column {
                Text(text = "ReTerminal", color = color)
                sessionBinder?.getService()?.currentSession?.value?.let { (id, mode) ->
                    Text(
                        style = MaterialTheme.typography.bodySmall,
                        text = "$id (${TerminalUtils.getNameOfWorkingMode(mode)})",
                        color = color
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.Menu, stringResource(strings.menu), tint = color)
            }
        },
        actions = {
            IconButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, stringResource(strings.shortcut_new_session), tint = color)
            }
        }
    )
}
