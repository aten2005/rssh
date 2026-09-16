package dev.aten.rssh.shortcut

import android.app.Activity
import android.os.Bundle
import dev.aten.rssh.exec.CommandLauncher

/**
 * Invisible target for pinned launcher shortcuts (shortcuts can only start activities).
 * Not exported: the system launches pinned shortcuts as this package, so no other app can trigger it.
 */
class RunCommandActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val commandId = intent.getLongExtra(CommandShortcuts.EXTRA_COMMAND_ID, 0)
        if (commandId > 0) CommandLauncher.enqueue(this, commandId)
        finish() // Theme.NoDisplay requires finishing before onResume completes
    }
}
