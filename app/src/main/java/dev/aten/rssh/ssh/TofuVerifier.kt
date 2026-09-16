package dev.aten.rssh.ssh

import java.security.PublicKey
import net.schmizz.sshj.transport.verification.HostKeyVerifier

/**
 * Trust-on-first-use host key check. Never trusts on its own: an unknown key is rejected and
 * reported via [outcome] so the UI can show the fingerprint and let the user confirm it.
 */
class TofuVerifier(private val knownBlob: ByteArray?) : HostKeyVerifier {
    sealed interface Outcome {
        data object Pending : Outcome
        data object Trusted : Outcome
        data class Unknown(val blob: ByteArray, val fingerprint: String) : Outcome
        data class Changed(val fingerprint: String) : Outcome
    }

    @Volatile
    var outcome: Outcome = Outcome.Pending
        private set

    override fun verify(hostname: String, port: Int, key: PublicKey): Boolean {
        val blob = HostKeys.encode(key)
        outcome = when {
            knownBlob == null -> Outcome.Unknown(blob, HostKeys.fingerprint(blob))
            knownBlob.contentEquals(blob) -> Outcome.Trusted
            else -> Outcome.Changed(HostKeys.fingerprint(blob))
        }
        return outcome is Outcome.Trusted
    }

    // Steers key-exchange towards the algorithm we already know, so a server with several
    // host keys doesn't look like a changed key on the next connection.
    override fun findExistingAlgorithms(hostname: String, port: Int): List<String> =
        knownBlob?.let { listOf(HostKeys.algorithm(it)) } ?: emptyList()
}
