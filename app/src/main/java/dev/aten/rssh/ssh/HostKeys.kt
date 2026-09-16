package dev.aten.rssh.ssh

import java.security.MessageDigest
import java.security.PublicKey
import java.util.Base64
import net.schmizz.sshj.common.Buffer

/** SSH wire-format public-key blobs, as used in authorized_keys and known_hosts. */
object HostKeys {
    fun encode(key: PublicKey): ByteArray = Buffer.PlainBuffer().putPublicKey(key).compactData

    fun decode(blob: ByteArray): PublicKey = Buffer.PlainBuffer(blob).readPublicKey()

    fun algorithm(blob: ByteArray): String = Buffer.PlainBuffer(blob).readString()

    /** Matches `ssh-keygen -lf`: SHA256 of the blob, base64 without padding. */
    fun fingerprint(blob: ByteArray): String =
        "SHA256:" + Base64.getEncoder().withoutPadding()
            .encodeToString(MessageDigest.getInstance("SHA-256").digest(blob))

    fun toBase64(blob: ByteArray): String = Base64.getEncoder().encodeToString(blob)

    fun fromBase64(text: String): ByteArray = Base64.getDecoder().decode(text)
}
