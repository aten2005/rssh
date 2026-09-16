package dev.aten.rssh.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aten.rssh.R
import dev.aten.rssh.appContainer
import dev.aten.rssh.data.CommandWithHost
import dev.aten.rssh.exec.CommandLauncher
import dev.aten.rssh.ui.MainActivity

class CommandWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val commandId = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)[KEY_COMMAND_ID]
        val bound = commandId?.let { context.appContainer.db.commands().getWithHost(it) }
        provideContent { WidgetContent(context, bound) }
    }

    companion object {
        val KEY_COMMAND_ID = longPreferencesKey("commandId")
    }
}

@Composable
private fun WidgetContent(context: Context, bound: CommandWithHost?) {
    GlanceTheme {
        val onClick = if (bound != null) {
            actionRunCallback<RunCommandAction>(actionParametersOf(RunCommandAction.COMMAND_ID to bound.command.id))
        } else {
            actionStartActivity<MainActivity>()
        }
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .cornerRadius(16.dp)
                .clickable(onClick)
                .padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = bound?.command?.label ?: context.getString(R.string.widget_unassigned),
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        color = GlanceTheme.colors.onSurface,
                    ),
                    maxLines = 2,
                )
                Text(
                    text = bound?.host?.name ?: context.getString(R.string.widget_tap_to_set_up),
                    style = TextStyle(fontSize = 12.sp, textAlign = TextAlign.Center, color = GlanceTheme.colors.onSurfaceVariant),
                    maxLines = 1,
                )
            }
        }
    }
}

class RunCommandAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        parameters[COMMAND_ID]?.let { CommandLauncher.enqueue(context, it) }
    }

    companion object {
        val COMMAND_ID = ActionParameters.Key<Long>("commandId")
    }
}

class CommandWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CommandWidget()
}
