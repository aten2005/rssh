package dev.aten.rssh.ui

import android.content.Context
import dev.aten.rssh.shortcut.CommandShortcuts
import dev.aten.rssh.tile.BaseSlotTileService

/** Updates shortcuts and tiles after commands or hosts change. */
object Surfaces {
    suspend fun refresh(context: Context) {
        CommandShortcuts.sync(context)
        BaseSlotTileService.refreshAll(context)
    }
}
