package dev.aten.rssh.tile

import android.service.quicksettings.Tile
import dev.aten.rssh.data.CommandWithHost
import dev.aten.rssh.exec.RunState

data class TileContent(val label: String, val subtitle: String, val state: Int)

/** Pure mapping from slot binding and run state to what the tile shows; kept Android-free for tests. */
fun tileContent(
    bound: CommandWithHost?,
    run: RunState,
    unassignedLabel: String,
    tapToSetUp: String,
    runningLabel: String,
): TileContent {
    if (bound == null) return TileContent(unassignedLabel, tapToSetUp, Tile.STATE_INACTIVE)
    val label = bound.command.label
    return when (run) {
        RunState.Idle -> TileContent(label, bound.host.name, Tile.STATE_ACTIVE)
        // Unavailable greys the tile and makes SystemUI ignore taps until the run ends.
        RunState.Running -> TileContent(label, runningLabel, Tile.STATE_UNAVAILABLE)
        is RunState.Finished -> TileContent(label, run.summary, Tile.STATE_ACTIVE)
    }
}
