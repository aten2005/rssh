package dev.aten.rssh.ssh

import dev.aten.rssh.data.Host
import dev.aten.rssh.ssh.SshRunner.Outcome
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PublicKey
import kotlinx.coroutines.runBlocking
import org.apache.sshd.common.config.keys.KeyUtils
import org.apache.sshd.common.keyprovider.KeyPairProvider
import org.apache.sshd.server.Environment
import org.apache.sshd.server.ExitCallback
import org.apache.sshd.server.SshServer
import org.apache.sshd.server.auth.pubkey.PublickeyAuthenticator
import org.apache.sshd.server.channel.ChannelSession
import org.apache.sshd.server.command.Command
import org.apache.sshd.server.command.CommandFactory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SshRunnerTest {
    private val clientKey = rsaKeyPair()
    private val hostKey = rsaKeyPair()
    private lateinit var server: SshServer
    private val runner = SshRunner { clientKey }

    @Before
    fun startServer() {
        server = SshServer.setUpDefaultServer().apply {
            port = 0
            keyPairProvider = KeyPairProvider { listOf(hostKey) }
            publickeyAuthenticator = PublickeyAuthenticator { _, key, _ -> KeyUtils.compareKeys(key, clientKey.public) }
            commandFactory = CommandFactory { _, command -> FakeCommand(command) }
            start()
        }
    }

    @After
    fun stopServer() {
        server.stop(true)
    }

    @Test
    fun `runs command and captures stdout`() = runBlocking {
        val outcome = runner.run(host(), "echo hi", 10) as Outcome.Success
        assertEquals(0, outcome.exitCode)
        assertEquals("hi\n", outcome.stdout)
        assertEquals("", outcome.stderr)
    }

    @Test
    fun `reports non-zero exit code and stderr`() = runBlocking {
        val outcome = runner.run(host(), "fail", 10) as Outcome.Success
        assertEquals(3, outcome.exitCode)
        assertEquals("boom\n", outcome.stderr)
    }

    @Test
    fun `unknown host key is refused and reported`() = runBlocking {
        val outcome = runner.run(host(known = null), "echo hi", 10) as Outcome.UnknownHostKey
        assertEquals(HostKeys.fingerprint(HostKeys.encode(hostKey.public)), outcome.fingerprint)
    }

    @Test
    fun `changed host key is refused`() = runBlocking {
        val outcome = runner.run(host(known = rsaKeyPair().public), "echo hi", 10)
        assertTrue(outcome is Outcome.HostKeyChanged)
    }

    @Test
    fun `wrong client key fails authentication`() = runBlocking {
        val outcome = SshRunner { rsaKeyPair() }.run(host(), "echo hi", 10) as Outcome.Failure
        assertTrue(outcome.message, outcome.message.startsWith("Authentication failed"))
    }

    @Test
    fun `missing client key fails without connecting`() = runBlocking {
        val outcome = SshRunner { null }.run(host(), "echo hi", 10)
        assertTrue(outcome is Outcome.Failure)
    }

    @Test
    fun `command exceeding timeout is reported`() = runBlocking {
        val outcome = runner.run(host(), "hang", 1) as Outcome.Failure
        assertEquals("Timed out after 1s", outcome.message)
    }

    private fun host(known: PublicKey? = hostKey.public) = Host(
        id = 1,
        name = "test",
        hostname = "127.0.0.1",
        port = server.port,
        username = "user",
        knownHostKey = known?.let { HostKeys.toBase64(HostKeys.encode(it)) },
    )

    private fun rsaKeyPair(): KeyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()

    /** In-JVM stand-in for a shell: no processes, deterministic output and exit codes. */
    private class FakeCommand(private val command: String) : Command {
        private lateinit var out: OutputStream
        private lateinit var err: OutputStream
        private var exit: ExitCallback? = null

        override fun setInputStream(input: InputStream) {}
        override fun setOutputStream(out: OutputStream) { this.out = out }
        override fun setErrorStream(err: OutputStream) { this.err = err }
        override fun setExitCallback(callback: ExitCallback) { exit = callback }

        override fun start(channel: ChannelSession, env: Environment) {
            when {
                command.startsWith("echo ") -> {
                    out.write((command.removePrefix("echo ") + "\n").toByteArray())
                    out.flush()
                    exit?.onExit(0)
                }
                command == "fail" -> {
                    err.write("boom\n".toByteArray())
                    err.flush()
                    exit?.onExit(3)
                }
                command == "hang" -> Unit
                else -> exit?.onExit(127)
            }
        }

        override fun destroy(channel: ChannelSession) {}
    }
}
