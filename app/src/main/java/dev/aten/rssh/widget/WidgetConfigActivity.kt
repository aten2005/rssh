package dev.aten.rssh.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dev.aten.rssh.appContainer
import dev.aten.rssh.ui.RsshTheme
import kotlinx.coroutines.launch

class WidgetConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        val appWidgetId = intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        setContent {
            RsshTheme {
                PickCommandScreen(onPick = { commandId -> lifecycleScope.launch { bind(appWidgetId, commandId) } })
            }
        }
    }

    private suspend fun bind(appWidgetId: Int, commandId: Long) {
        val glanceId = GlanceAppWidgetManager(this).getGlanceIdBy(appWidgetId)
        updateAppWidgetState(this, glanceId) { it[CommandWidget.KEY_COMMAND_ID] = commandId }
        CommandWidget().update(this, glanceId)
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
        finish()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickCommandScreen(onPick: (Long) -> Unit) {
    val db = androidx.compose.ui.platform.LocalContext.current.appContainer.db
    val commands by remember { db.commands().observeAllWithHost() }.collectAsStateWithLifecycle(emptyList())
    Scaffold(topBar = { TopAppBar(title = { Text("Choose a command") }) }) { padding ->
        if (commands.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("No commands yet. Open rssh and add one first.", textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(Modifier.padding(padding)) {
                items(commands, key = { it.command.id }) { bound ->
                    ListItem(
                        headlineContent = { Text(bound.command.label) },
                        supportingContent = { Text(bound.host.name) },
                        modifier = Modifier.clickable { onPick(bound.command.id) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
