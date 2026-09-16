package dev.aten.rssh.ssh

import dev.aten.rssh.data.Host
import java.io.IOException
import java.security.KeyPair
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.schmizz.sshj.DefaultConfig
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.connection.ConnectionException
import net.schmizz.sshj.transport.TransportException
import net.schmizz.sshj.userauth.UserAuthException
import net.schmizz.sshj.userauth.keyprovider.KeyPairWrapper

class SshRunner(private val keyPairProvider: () -> KeyPair?) {
    sealed interface Outcome {
        data class Success(val exitCode: Int, val stdout: String, val stderr: String) : Outcome {
            val ok: Boolean get() = exitCode == 0
        }
        data class UnknownHostKey(val keyBlob: ByteArray, val fingerprint: String) : Outcome
        data class HostKeyChanged(val fingerprint: String) : Outcome
        data class Failure(val message: String) : Outcome
    }

    private val config by lazy { DefaultConfig() }

    suspend fun run(host: Host, command: String, timeoutSec: Int): Outcome = withContext(Dispatchers.IO) {
        val keyPair = keyPairProvider()
            ?: return@withContext Outcome.Failure("No SSH key yet — generate one in the app")
        val verifier = TofuVerifier(host.knownHostKey?.let(HostKeys::fromBase64))
        val client = SSHClient(config)
        client.addHostKeyVerifier(verifier)
        client.connectTimeout = CONNECT_TIMEOUT_MS
        try {
            client.connect(host.hostname, host.port)
            client.authPublickey(host.username, KeyPairWrapper(keyPair))
            client.startSession().use { session ->
                val cmd = session.exec(command)
                // Output is buffered in the channel window, so wait for exit before reading.
                // Commands printing more than the window (~2 MB) stall and hit the timeout.
                cmd.join(timeoutSec.toLong(), TimeUnit.SECONDS)
                Outcome.Success(
                    exitCode = cmd.exitStatus ?: -1,
                    stdout = cmd.inputStream.readBytes().decodeToString(),
                    stderr = cmd.errorStream.readBytes().decodeToString(),
                )
            }
        } catch (e: TransportException) {
            when (val o = verifier.outcome) {
                is TofuVerifier.Outcome.Unknown -> Outcome.UnknownHostKey(o.blob, o.fingerprint)
                is TofuVerifier.Outcome.Changed -> Outcome.HostKeyChanged(o.fingerprint)
                else -> Outcome.Failure(e.describe())
            }
        } catch (e: UserAuthException) {
            Outcome.Failure("Authentication failed: ${e.describe()}")
        } catch (e: ConnectionException) {
            if (e.message?.contains("Timeout", ignoreCase = true) == true) {
                Outcome.Failure("Timed out after ${timeoutSec}s")
            } else {
                Outcome.Failure(e.describe())
            }
        } catch (e: IOException) {
            Outcome.Failure(e.describe())
        } finally {
            runCatching { client.disconnect() }
        }
    }

    /** Connects, authenticates and runs a no-op; used by "Test connection" and the TOFU dialog. */
    suspend fun test(host: Host): Outcome = run(host, "true", TEST_TIMEOUT_SEC)

    private fun Throwable.describe(): String = message?.takeIf { it.isNotBlank() } ?: javaClass.simpleName

    private companion object {
        const val CONNECT_TIMEOUT_MS = 10_000
        const val TEST_TIMEOUT_SEC = 15
    }
}
