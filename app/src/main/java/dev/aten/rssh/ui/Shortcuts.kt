package dev.aten.rssh.ui

import android.content.Context
import androidx.glance.appwidget.updateAll
import dev.aten.rssh.tile.BaseSlotTileService
import dev.aten.rssh.widget.CommandWidget

/** Re-renders tiles and widgets after commands or hosts change. */
object Shortcuts {
    suspend fun refresh(context: Context) {
        CommandWidget().updateAll(context)
        BaseSlotTileService.refreshAll(context)
    }
}
