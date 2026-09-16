package dev.aten.rssh.ssh

import java.security.KeyPairGenerator
import java.security.PublicKey
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TofuVerifierTest {
    private val keyA = rsaKey()
    private val keyB = rsaKey()

    @Test
    fun `unknown key is rejected and reported with its fingerprint`() {
        val verifier = TofuVerifier(knownBlob = null)

        assertFalse(verifier.verify("host", 22, keyA))

        val outcome = verifier.outcome as TofuVerifier.Outcome.Unknown
        assertArrayEquals(HostKeys.encode(keyA), outcome.blob)
        assertTrue(outcome.fingerprint.startsWith("SHA256:"))
        assertTrue(verifier.findExistingAlgorithms("host", 22).isEmpty())
    }

    @Test
    fun `matching key is trusted`() {
        val verifier = TofuVerifier(HostKeys.encode(keyA))

        assertTrue(verifier.verify("host", 22, keyA))

        assertEquals(TofuVerifier.Outcome.Trusted, verifier.outcome)
        assertEquals(listOf("ssh-rsa"), verifier.findExistingAlgorithms("host", 22))
    }

    @Test
    fun `changed key is rejected`() {
        val verifier = TofuVerifier(HostKeys.encode(keyA))

        assertFalse(verifier.verify("host", 22, keyB))

        val expected = HostKeys.fingerprint(HostKeys.encode(keyB))
        assertEquals(TofuVerifier.Outcome.Changed(expected), verifier.outcome)
    }

    @Test
    fun `blob round-trips through base64 and decodes to the same key`() {
        val blob = HostKeys.encode(keyA)
        val decoded = HostKeys.decode(HostKeys.fromBase64(HostKeys.toBase64(blob)))
        assertArrayEquals(keyA.encoded, decoded.encoded)
    }

    private fun rsaKey(): PublicKey =
        KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair().public
}
