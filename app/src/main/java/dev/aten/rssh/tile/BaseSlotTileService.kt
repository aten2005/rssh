package dev.aten.rssh.tile

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService
import dev.aten.rssh.R
import dev.aten.rssh.appContainer
import dev.aten.rssh.data.CommandWithHost
import dev.aten.rssh.data.TILE_SLOT_COUNT
import dev.aten.rssh.exec.CommandLauncher
import dev.aten.rssh.exec.RunState
import dev.aten.rssh.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Quick Settings tiles are fixed manifest entries, so each of the five slots is a subclass.
 *
 * While the panel is open the tile follows the command's [RunState]: "Running…" during the SSH
 * round trip, then a short result, then back to the host name. Tapping never collapses the
 * panel, so the user sees the whole sequence.
 */
abstract class BaseSlotTileService(private val slotIndex: Int) : TileService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var listenJob: Job? = null

    override fun onStartListening() {
        listenJob?.cancel()
        listenJob = scope.launch {
            val bound = boundCommand()
            if (bound == null) {
                render(null, RunState.Idle)
            } else {
                appContainer.runStates.observe(bound.command.id).collect { render(bound, it) }
            }
        }
    }

    override fun onStopListening() {
        listenJob?.cancel()
        listenJob = null
    }

    override fun onClick() {
        unlockAndRun {
            scope.launch {
                val bound = boundCommand()
                if (bound == null) {
                    openApp()
                } else {
                    CommandLauncher.enqueue(this@BaseSlotTileService, bound.command.id)
                    // Flip to "Running…" now rather than when WorkManager gets around to starting the worker.
                    appContainer.runStates.running(bound.command.id)
                }
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun boundCommand(): CommandWithHost? {
        val db = appContainer.db
        return db.tileSlots().get(slotIndex)?.let { db.commands().getWithHost(it.commandId) }
    }

    private fun render(bound: CommandWithHost?, run: RunState) {
        val tile = qsTile ?: return
        val content = tileContent(
            bound,
            run,
            unassignedLabel = getString(R.string.tile_unassigned, slotIndex + 1),
            tapToSetUp = getString(R.string.tile_tap_to_set_up),
            runningLabel = getString(R.string.tile_running),
        )
        tile.label = content.label
        tile.subtitle = content.subtitle
        tile.state = content.state
        tile.updateTile()
    }

    @SuppressLint("StartActivityAndCollapseDeprecated") // the PendingIntent overload only exists on API 34+
    private fun openApp() {
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent, flags))
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    companion object {
        private val services = listOf(
            SlotTileService0::class.java,
            SlotTileService1::class.java,
            SlotTileService2::class.java,
            SlotTileService3::class.java,
            SlotTileService4::class.java,
        )

        /**
         * Asks the system to re-query every tile. Only honoured for tiles declaring ACTIVE_TILE,
         * which these deliberately are not: passive tiles re-read their slot every time the panel
         * opens, and active tiles would stop getting onStartListening on open, breaking live state.
         */
        fun refreshAll(context: Context) {
            check(services.size == TILE_SLOT_COUNT)
            services.forEach { requestListeningState(context, ComponentName(context, it)) }
        }
    }
}

class SlotTileService0 : BaseSlotTileService(0)
class SlotTileService1 : BaseSlotTileService(1)
class SlotTileService2 : BaseSlotTileService(2)
class SlotTileService3 : BaseSlotTileService(3)
class SlotTileService4 : BaseSlotTileService(4)
