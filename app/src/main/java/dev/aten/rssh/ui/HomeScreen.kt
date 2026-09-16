package dev.aten.rssh.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.aten.rssh.appContainer
import dev.aten.rssh.data.TILE_SLOT_COUNT
import dev.aten.rssh.data.TileSlot
import dev.aten.rssh.exec.CommandLauncher
import dev.aten.rssh.tile.BaseSlotTileService
import kotlinx.coroutines.launch

private enum class Tab(val label: String, val icon: ImageVector) {
    Commands("Commands", Icons.Filled.PlayArrow),
    Hosts("Hosts", Icons.Filled.Place),
    Tiles("Tiles", Icons.Filled.Settings),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onOpenHost: (Long) -> Unit, onOpenCommand: (Long) -> Unit, onOpenKey: () -> Unit) {
    var tab by rememberSaveable { mutableStateOf(Tab.Commands) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("rssh") },
                actions = {
                    IconButton(onClick = onOpenKey) { Icon(Icons.Filled.Lock, contentDescription = "SSH key") }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Icon(t.icon, contentDescription = null) },
                        label = { Text(t.label) },
                    )
                }
            }
        },
        floatingActionButton = {
            when (tab) {
                Tab.Commands -> FloatingActionButton(onClick = { onOpenCommand(0) }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add command")
                }
                Tab.Hosts -> FloatingActionButton(onClick = { onOpenHost(0) }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add host")
                }
                Tab.Tiles -> Unit
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                Tab.Commands -> CommandsTab(onOpenCommand)
                Tab.Hosts -> HostsTab(onOpenHost)
                Tab.Tiles -> TilesTab()
            }
        }
    }
}

@Composable
private fun CommandsTab(onOpen: (Long) -> Unit) {
    val context = LocalContext.current
    val db = context.appContainer.db
    val commands by remember { db.commands().observeAllWithHost() }.collectAsStateWithLifecycle(emptyList())
    if (commands.isEmpty()) {
        EmptyText("No commands yet.\nAdd a host, then a command.")
        return
    }
    LazyColumn {
        items(commands, key = { it.command.id }) { bound ->
            ListItem(
                headlineContent = { Text(bound.command.label) },
                supportingContent = {
                    Text("${bound.host.name} · ${bound.command.command}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                trailingContent = {
                    IconButton(onClick = { CommandLauncher.enqueue(context, bound.command.id) }) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Run")
                    }
                },
                modifier = Modifier.clickable { onOpen(bound.command.id) },
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun HostsTab(onOpen: (Long) -> Unit) {
    val context = LocalContext.current
    val db = context.appContainer.db
    val hosts by remember { db.hosts().observeAll() }.collectAsStateWithLifecycle(emptyList())
    if (hosts.isEmpty()) {
        EmptyText("No hosts yet.\nGenerate an SSH key (lock icon), then add a host.")
        return
    }
    LazyColumn {
        items(hosts, key = { it.id }) { host ->
            ListItem(
                headlineContent = { Text(host.name) },
                supportingContent = { Text("${host.username}@${host.hostname}:${host.port}") },
                trailingContent = {
                    if (host.knownHostKey != null) Icon(Icons.Filled.Lock, contentDescription = "Host key verified")
                },
                modifier = Modifier.clickable { onOpen(host.id) },
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun TilesTab() {
    val context = LocalContext.current
    val db = context.appContainer.db
    val scope = rememberCoroutineScope()
    val slots by remember { db.tileSlots().observeAll() }.collectAsStateWithLifecycle(emptyList())
    val commands by remember { db.commands().observeAllWithHost() }.collectAsStateWithLifecycle(emptyList())
    var editing by remember { mutableStateOf<Int?>(null) }

    fun assign(slot: Int, commandId: Long?) {
        editing = null
        scope.launch {
            if (commandId == null) db.tileSlots().clear(slot) else db.tileSlots().upsert(TileSlot(slot, commandId))
            BaseSlotTileService.refreshAll(context)
        }
    }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        Text(
            "Each tile runs one command. Add the tiles \"rssh 1\"–\"rssh 5\" from the Quick Settings editor; " +
                "the home-screen widget is added from your launcher's widget picker.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
        )
        for (slot in 0 until TILE_SLOT_COUNT) {
            val bound = slots.firstOrNull { it.slotIndex == slot }
                ?.let { s -> commands.firstOrNull { it.command.id == s.commandId } }
            ListItem(
                headlineContent = { Text("Tile ${slot + 1}") },
                supportingContent = { Text(bound?.let { "${it.command.label} · ${it.host.name}" } ?: "Unassigned") },
                modifier = Modifier.clickable { editing = slot },
            )
            HorizontalDivider()
        }
    }

    editing?.let { slot ->
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("Tile ${slot + 1}") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    if (commands.isEmpty()) Text("No commands yet.")
                    commands.forEach { bound ->
                        ListItem(
                            headlineContent = { Text(bound.command.label) },
                            supportingContent = { Text(bound.host.name) },
                            modifier = Modifier.clickable { assign(slot, bound.command.id) },
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { assign(slot, null) }) { Text("Clear") } },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun EmptyText(text: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
    }
}
