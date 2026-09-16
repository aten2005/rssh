package dev.aten.rssh.tile

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import dev.aten.rssh.R
import dev.aten.rssh.appContainer
import dev.aten.rssh.data.CommandWithHost
import dev.aten.rssh.data.TILE_SLOT_COUNT
import dev.aten.rssh.exec.CommandLauncher
import dev.aten.rssh.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Quick Settings tiles are fixed manifest entries, so each of the five slots is a subclass. */
abstract class BaseSlotTileService(private val slotIndex: Int) : TileService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onStartListening() {
        scope.launch { render(boundCommand()) }
    }

    override fun onClick() {
        unlockAndRun {
            scope.launch {
                val bound = boundCommand()
                if (bound == null) openApp() else CommandLauncher.enqueue(this@BaseSlotTileService, bound.command.id)
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

    private fun render(bound: CommandWithHost?) {
        val tile = qsTile ?: return
        tile.label = bound?.command?.label ?: getString(R.string.tile_unassigned, slotIndex + 1)
        tile.subtitle = bound?.host?.name ?: getString(R.string.tile_tap_to_set_up)
        tile.state = if (bound != null) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
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

        /** Asks the system to re-query every tile so labels follow slot changes. */
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
