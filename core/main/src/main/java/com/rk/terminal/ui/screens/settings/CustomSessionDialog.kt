package com.rk.terminal.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rk.resources.strings
import java.io.File

@Composable
fun CustomSessionDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, shellPath: String) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var shellPath by remember { mutableStateOf("/sdcard/ReTerminal/") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun validate(): String? {
        val trimmedName = name.trim()
        val trimmedPath = shellPath.trim()

        if (trimmedName.isBlank()) return context.getString(strings.name_cannot_be_empty)
        if (trimmedPath.isBlank()) return context.getString(strings.shell_path_empty)
        if (!trimmedPath.startsWith("/")) return context.getString(strings.absolute_path_required)

        val file = File(trimmedPath)
        if (!file.exists()) return context.getString(strings.file_not_found)
        if (!file.isFile) return context.getString(strings.path_not_file)

        return null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(strings.new_custom_session)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = { Text(stringResource(strings.session_name)) },
                    isError = errorMessage != null && name.isBlank(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = shellPath,
                    onValueChange = {
                        shellPath = it
                        errorMessage = null
                    },
                    label = { Text(stringResource(strings.shell_script_path)) },
                    isError = errorMessage != null && shellPath.isBlank(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMessage != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val error = validate()
                if (error != null) {
                    errorMessage = error
                } else {
                    onSave(name.trim(), shellPath.trim())
                }
            }) { Text(stringResource(strings.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(strings.cancel)) }
        }
    )
}
