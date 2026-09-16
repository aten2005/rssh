package dev.aten.rssh.exec

import dev.aten.rssh.ssh.SshRunner.Outcome

/** Builds the one-line toast shown after a command runs. */
object ResultFormatter {
    private const val MAX_OUTPUT_CHARS = 80

    fun format(label: String, outcome: Outcome): String = when (outcome) {
        is Outcome.Success ->
            if (outcome.ok) "✓ $label${firstLine(outcome.stdout)}"
            else "✗ $label (exit ${outcome.exitCode})${firstLine(outcome.stderr.ifBlank { outcome.stdout })}"
        is Outcome.UnknownHostKey -> "✗ $label: host key not verified — open rssh to confirm it"
        is Outcome.HostKeyChanged -> "✗ $label: HOST KEY CHANGED — refused"
        is Outcome.Failure -> "✗ $label: ${outcome.message}"
    }

    private fun firstLine(text: String): String =
        text.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.isNotEmpty() }
            ?.let { ": " + it.take(MAX_OUTPUT_CHARS) }
            ?: ""
}
