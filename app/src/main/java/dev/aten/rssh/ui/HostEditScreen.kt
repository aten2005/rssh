package dev.aten.rssh.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.aten.rssh.appContainer
import dev.aten.rssh.data.Host
import dev.aten.rssh.ssh.HostKeys
import dev.aten.rssh.ssh.SshRunner.Outcome
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostEditScreen(hostId: Long, onDone: () -> Unit) {
    val context = LocalContext.current
    val container = context.appContainer
    val scope = rememberCoroutineScope()

    var id by rememberSaveable { mutableLongStateOf(hostId) }
    var name by rememberSaveable { mutableStateOf("") }
    var hostname by rememberSaveable { mutableStateOf("") }
    var port by rememberSaveable { mutableStateOf("22") }
    var username by rememberSaveable { mutableStateOf("") }
    var knownHostKey by rememberSaveable { mutableStateOf<String?>(null) }
    var loaded by rememberSaveable { mutableStateOf(hostId == 0L) }

    var status by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var unknownKey by remember { mutableStateOf<Outcome.UnknownHostKey?>(null) }
    var changedKey by remember { mutableStateOf<Outcome.HostKeyChanged?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(hostId) {
        if (loaded) return@LaunchedEffect
        container.db.hosts().get(hostId)?.let {
            name = it.name
            hostname = it.hostname
            port = it.port.toString()
            username = it.username
            knownHostKey = it.knownHostKey
        }
        loaded = true
    }

    val valid = hostname.isNotBlank() && username.isNotBlank() && port.toIntOrNull() in 1..65535

    fun draft() = Host(
        id = id,
        name = name.trim().ifEmpty { hostname.trim() },
        hostname = hostname.trim(),
        port = port.toIntOrNull() ?: 22,
        username = username.trim(),
        knownHostKey = knownHostKey,
    )

    suspend fun save() {
        val rowId = container.db.hosts().upsert(draft())
        if (id == 0L) id = rowId
    }

    suspend fun runTest() {
        busy = true
        status = "Connecting…"
        when (val outcome = container.runner.test(draft())) {
            is Outcome.Success ->
                status = if (outcome.ok) "Connected ✓" else "Connected, but the test command exited ${outcome.exitCode}"
            is Outcome.UnknownHostKey -> { status = null; unknownKey = outcome }
            is Outcome.HostKeyChanged -> { status = null; changedKey = outcome }
            is Outcome.Failure -> status = "✗ ${outcome.message}"
        }
        busy = false
    }

    fun forgetKey() {
        knownHostKey = null
        if (id != 0L) scope.launch { container.db.hosts().setKnownHostKey(id, null) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (hostId == 0L) "New host" else "Edit host") },
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
            OutlinedTextField(name, { name = it }, label = { Text("Name (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(hostname, { hostname = it }, label = { Text("Hostname or IP") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(username, { username = it }, label = { Text("Username") }, singleLine = true, modifier = Modifier.weight(2f))
                OutlinedTextField(
                    port, { port = it }, label = { Text("Port") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
            }

            val known = knownHostKey
            if (known == null) {
                Text("Host key not verified yet — run Test connection.", style = MaterialTheme.typography.bodySmall)
            } else {
                Text("Host key ${HostKeys.fingerprint(HostKeys.fromBase64(known))}", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                TextButton(onClick = ::forgetKey) { Text("Forget host key") }
            }

            status?.let { Text(it) }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { scope.launch { runTest() } }, enabled = valid && !busy, modifier = Modifier.weight(1f)) {
                    Text("Test connection")
                }
                Button(onClick = { scope.launch { save(); onDone() } }, enabled = valid, modifier = Modifier.weight(1f)) {
                    Text("Save")
                }
            }
        }
    }

    unknownKey?.let { key ->
        AlertDialog(
            onDismissRequest = { unknownKey = null },
            title = { Text("Unknown host key") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${hostname.trim()}:${port} presented a key with fingerprint")
                    Text(key.fingerprint, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                    Text("Compare it with `ssh-keygen -lf /etc/ssh/ssh_host_*_key.pub` on the host before trusting it.")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    knownHostKey = HostKeys.toBase64(key.keyBlob)
                    unknownKey = null
                    scope.launch { save(); runTest() }
                }) { Text("Trust") }
            },
            dismissButton = {
                TextButton(onClick = { unknownKey = null; status = "Host key not trusted" }) { Text("Cancel") }
            },
        )
    }

    changedKey?.let { key ->
        AlertDialog(
            onDismissRequest = { changedKey = null },
            title = { Text("Host key changed!") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("The host's key no longer matches the saved one. New fingerprint:")
                    Text(key.fingerprint, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                    Text("This happens after a server reinstall — or a man-in-the-middle attack. The connection was refused.")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    forgetKey()
                    changedKey = null
                    status = "Saved key forgotten — test again to trust the new one"
                }) { Text("Forget saved key") }
            },
            dismissButton = { TextButton(onClick = { changedKey = null }) { Text("Keep") } },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete host?") },
            text = { Text("Its commands and tile assignments are deleted too.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        container.db.hosts().delete(draft())
                        Shortcuts.refresh(context)
                        onDone()
                    }
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}
