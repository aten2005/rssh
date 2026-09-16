package dev.aten.rssh.shortcut

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import dev.aten.rssh.R
import dev.aten.rssh.appContainer
import dev.aten.rssh.data.CommandWithHost

/** Pinned home-screen shortcuts, one per command, that run it through [RunCommandActivity]. */
object CommandShortcuts {
    const val EXTRA_COMMAND_ID = "commandId"
    private const val ID_PREFIX = "command-"

    fun id(commandId: Long): String = "$ID_PREFIX$commandId"

    fun commandId(shortcutId: String): Long? =
        shortcutId.takeIf { it.startsWith(ID_PREFIX) }?.removePrefix(ID_PREFIX)?.toLongOrNull()?.takeIf { it > 0 }

    /** Asks the launcher to pin a shortcut; false if the launcher does not support pinning. */
    fun requestPin(context: Context, bound: CommandWithHost): Boolean {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) return false
        return ShortcutManagerCompat.requestPinShortcut(context, build(context, bound), null)
    }

    /**
     * Relabels pinned shortcuts after edits and disables those whose command was deleted.
     * Only called from the foreground UI, so shortcut rate limiting does not apply.
     */
    suspend fun sync(context: Context) {
        val db = context.appContainer.db
        val pinned = ShortcutManagerCompat.getShortcuts(context, ShortcutManagerCompat.FLAG_MATCH_PINNED)
        val updated = mutableListOf<ShortcutInfoCompat>()
        val orphaned = mutableListOf<String>()
        for (shortcut in pinned) {
            val bound = commandId(shortcut.id)?.let { db.commands().getWithHost(it) }
            when {
                bound != null -> updated += build(context, bound)
                shortcut.isEnabled -> orphaned += shortcut.id
            }
        }
        if (updated.isNotEmpty()) ShortcutManagerCompat.updateShortcuts(context, updated)
        if (orphaned.isNotEmpty()) {
            ShortcutManagerCompat.disableShortcuts(context, orphaned, context.getString(R.string.shortcut_disabled))
        }
    }

    private fun build(context: Context, bound: CommandWithHost): ShortcutInfoCompat {
        val intent = Intent(context, RunCommandActivity::class.java)
            .setAction(Intent.ACTION_VIEW)
            .putExtra(EXTRA_COMMAND_ID, bound.command.id)
        return ShortcutInfoCompat.Builder(context, id(bound.command.id))
            .setShortLabel(bound.command.label)
            .setLongLabel("${bound.command.label} · ${bound.host.name}")
            .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_shortcut))
            .setIntent(intent)
            .build()
    }
}
