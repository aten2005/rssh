package dev.aten.rssh.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.aten.rssh.appContainer
import dev.aten.rssh.data.Command
import dev.aten.rssh.exec.CommandLauncher
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandEditScreen(commandId: Long, onDone: () -> Unit) {
    val context = LocalContext.current
    val db = context.appContainer.db
    val scope = rememberCoroutineScope()
    val hosts by remember { db.hosts().observeAll() }.collectAsStateWithLifecycle(emptyList())

    var id by rememberSaveable { mutableLongStateOf(commandId) }
    var hostId by rememberSaveable { mutableLongStateOf(0L) }
    var label by rememberSaveable { mutableStateOf("") }
    var command by rememberSaveable { mutableStateOf("") }
    var timeout by rememberSaveable { mutableStateOf("30") }
    var loaded by rememberSaveable { mutableStateOf(commandId == 0L) }
    var hostMenuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(commandId) {
        if (loaded) return@LaunchedEffect
        db.commands().get(commandId)?.let {
            hostId = it.hostId
            label = it.label
            command = it.command
            timeout = it.timeoutSec.toString()
        }
        loaded = true
    }
    LaunchedEffect(hosts) {
        if (hostId == 0L) hostId = hosts.firstOrNull()?.id ?: 0L
    }

    val timeoutSec = timeout.toIntOrNull()
    val valid = label.isNotBlank() && command.isNotBlank() && hostId != 0L && timeoutSec != null && timeoutSec > 0

    suspend fun save(): Long {
        val rowId = db.commands().upsert(Command(id, hostId, label.trim(), command.trim(), timeoutSec ?: 30))
        if (id == 0L) id = rowId
        Surfaces.refresh(context)
        return id
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (commandId == 0L) "New command" else "Edit command") },
                navigationIcon = {
                    IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    if (id != 0L) {
                        IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box {
                OutlinedButton(onClick = { hostMenuOpen = true }, enabled = hosts.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        when {
                            hosts.isEmpty() -> "Add a host first"
                            else -> hosts.firstOrNull { it.id == hostId }?.name ?: "Choose host"
                        },
                        modifier = Modifier.weight(1f),
                    )
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(expanded = hostMenuOpen, onDismissRequest = { hostMenuOpen = false }) {
                    hosts.forEach { host ->
                        DropdownMenuItem(
                            text = { Text("${host.name} (${host.username}@${host.hostname})") },
                            onClick = { hostId = host.id; hostMenuOpen = false },
                        )
                    }
                }
            }
            OutlinedTextField(label, { label = it }, label = { Text("Label (shown on the tile)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                command, { command = it },
                label = { Text("Command") },
                textStyle = TextStyle(fontFamily = FontFamily.Monospace),
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                timeout, { timeout = it },
                label = { Text("Timeout (seconds)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            Text("The command runs through the user's login shell on the host.", style = MaterialTheme.typography.bodySmall)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { scope.launch { CommandLauncher.enqueue(context, save()) } },
                    enabled = valid,
                    modifier = Modifier.weight(1f),
                ) { Text("Save & run") }
                Button(onClick = { scope.launch { save(); onDone() } }, enabled = valid, modifier = Modifier.weight(1f)) {
                    Text("Save")
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete command?") },
            text = { Text("Tiles using it are cleared and its shortcuts are disabled.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        db.commands().get(id)?.let { db.commands().delete(it) }
                        Surfaces.refresh(context)
                        onDone()
                    }
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}
