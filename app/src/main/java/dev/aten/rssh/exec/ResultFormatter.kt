package dev.aten.rssh.exec

import dev.aten.rssh.ssh.SshRunner.Outcome

/** Builds the one-line toast shown after a command runs, and the shorter tile subtitle. */
object ResultFormatter {
    private const val MAX_OUTPUT_CHARS = 80
    private const val MAX_SHORT_CHARS = 24
    const val MISSING = "✗ missing"

    fun format(label: String, outcome: Outcome): String = when (outcome) {
        is Outcome.Success ->
            if (outcome.ok) "✓ $label${firstLine(outcome.stdout)}"
            else "✗ $label (exit ${outcome.exitCode})${firstLine(outcome.stderr.ifBlank { outcome.stdout })}"
        is Outcome.UnknownHostKey -> "✗ $label: host key not verified — open rssh to confirm it"
        is Outcome.HostKeyChanged -> "✗ $label: HOST KEY CHANGED — refused"
        is Outcome.Failure -> "✗ $label: ${outcome.message}"
    }

    fun isOk(outcome: Outcome): Boolean = outcome is Outcome.Success && outcome.ok

    /** Fits a Quick Settings tile subtitle, so a few words at most. */
    fun short(outcome: Outcome): String = when (outcome) {
        is Outcome.Success ->
            if (outcome.ok) "✓ " + (firstLine(outcome.stdout, MAX_SHORT_CHARS) ?: "done")
            else "✗ exit ${outcome.exitCode}"
        is Outcome.UnknownHostKey -> "✗ host key unverified"
        is Outcome.HostKeyChanged -> "✗ host key changed"
        is Outcome.Failure -> "✗ " + (firstLine(outcome.message, MAX_SHORT_CHARS) ?: "failed")
    }

    private fun firstLine(text: String): String = firstLine(text, MAX_OUTPUT_CHARS)?.let { ": $it" } ?: ""

    private fun firstLine(text: String, max: Int): String? =
        text.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.isNotEmpty() }
            ?.take(max)
}
