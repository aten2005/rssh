package dev.aten.rssh.exec

import dev.aten.rssh.ssh.SshRunner.Outcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultFormatterTest {
    @Test
    fun `short success uses the first non-empty output line, truncated`() {
        assertEquals("✓ done", ResultFormatter.short(Outcome.Success(0, "\n  \n", "")))
        assertEquals("✓ up 3 days", ResultFormatter.short(Outcome.Success(0, "\n up 3 days \nmore", "")))
        assertEquals(
            "✓ " + "x".repeat(24),
            ResultFormatter.short(Outcome.Success(0, "x".repeat(40), "")),
        )
    }

    @Test
    fun `short failures name the cause briefly`() {
        assertEquals("✗ exit 2", ResultFormatter.short(Outcome.Success(2, "", "boom")))
        assertEquals("✗ host key unverified", ResultFormatter.short(Outcome.UnknownHostKey(ByteArray(0), "SHA256:x")))
        assertEquals("✗ host key changed", ResultFormatter.short(Outcome.HostKeyChanged("SHA256:x")))
        assertEquals("✗ Timed out after 30s", ResultFormatter.short(Outcome.Failure("Timed out after 30s")))
    }

    @Test
    fun `ok only for a zero exit`() {
        assertTrue(ResultFormatter.isOk(Outcome.Success(0, "", "")))
        assertFalse(ResultFormatter.isOk(Outcome.Success(1, "", "")))
        assertFalse(ResultFormatter.isOk(Outcome.Failure("x")))
    }

    @Test
    fun `toast format is unchanged`() {
        assertEquals("✓ up: ok", ResultFormatter.format("up", Outcome.Success(0, "ok\n", "")))
        assertEquals("✗ up (exit 1): err", ResultFormatter.format("up", Outcome.Success(1, "", "err")))
    }
}
