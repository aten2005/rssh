package dev.aten.rssh.tile

import android.service.quicksettings.Tile
import dev.aten.rssh.data.Command
import dev.aten.rssh.data.CommandWithHost
import dev.aten.rssh.data.Host
import dev.aten.rssh.exec.RunState
import org.junit.Assert.assertEquals
import org.junit.Test

class TileContentTest {
    private val bound = CommandWithHost(
        command = Command(id = 7, hostId = 3, label = "Reboot", command = "sudo reboot"),
        host = Host(id = 3, name = "pi", hostname = "pi.local", username = "pi"),
    )

    private fun content(bound: CommandWithHost?, run: RunState) =
        tileContent(bound, run, unassignedLabel = "rssh 1", tapToSetUp = "Tap to set up", runningLabel = "Running…")

    @Test
    fun `unassigned slot is inactive regardless of run state`() {
        assertEquals(TileContent("rssh 1", "Tap to set up", Tile.STATE_INACTIVE), content(null, RunState.Running))
    }

    @Test
    fun `idle shows label and host`() {
        assertEquals(TileContent("Reboot", "pi", Tile.STATE_ACTIVE), content(bound, RunState.Idle))
    }

    @Test
    fun `running greys the tile out`() {
        assertEquals(TileContent("Reboot", "Running…", Tile.STATE_UNAVAILABLE), content(bound, RunState.Running))
    }

    @Test
    fun `finished shows the summary and is tappable again`() {
        assertEquals(
            TileContent("Reboot", "✗ exit 1", Tile.STATE_ACTIVE),
            content(bound, RunState.Finished(ok = false, summary = "✗ exit 1")),
        )
    }
}
