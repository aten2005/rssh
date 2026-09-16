package dev.aten.rssh.shortcut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CommandShortcutsTest {
    @Test
    fun `shortcut id round-trips the command id`() {
        assertEquals(42L, CommandShortcuts.commandId(CommandShortcuts.id(42)))
    }

    @Test
    fun `foreign or malformed ids are ignored`() {
        assertNull(CommandShortcuts.commandId("other-42"))
        assertNull(CommandShortcuts.commandId("command-"))
        assertNull(CommandShortcuts.commandId("command-abc"))
        assertNull(CommandShortcuts.commandId("command-0"))
    }
}
