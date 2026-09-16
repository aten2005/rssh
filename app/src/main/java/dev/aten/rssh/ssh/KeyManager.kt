package dev.aten.rssh.ssh

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import net.schmizz.sshj.common.Buffer

/**
 * One Ed25519 key pair per device. The PKCS#8 private key is stored AES-GCM encrypted with a
 * non-exportable key from AndroidKeyStore; the public key is stored as plain X.509 SPKI.
 */
class KeyManager(context: Context) {
    private val privFile = File(context.filesDir, "id_ed25519.enc")
    private val pubFile = File(context.filesDir, "id_ed25519.pub")
    private val lock = Any()

    fun hasKey(): Boolean = privFile.exists() && pubFile.exists()

    fun generate(): KeyPair = synchronized(lock) {
        val pair = KeyPairGenerator.getInstance("Ed25519", "BC").generateKeyPair()
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, wrapKey()) }
        val encrypted = cipher.doFinal(pair.private.encoded)
        privFile.writeBytes(cipher.iv + encrypted)
        pubFile.writeBytes(pair.public.encoded)
        pair
    }

    fun loadKeyPair(): KeyPair? = synchronized(lock) {
        if (!hasKey()) return null
        val stored = privFile.readBytes()
        val iv = stored.copyOfRange(0, GCM_IV_LEN)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, wrapKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        }
        val pkcs8 = cipher.doFinal(stored, GCM_IV_LEN, stored.size - GCM_IV_LEN)
        val factory = KeyFactory.getInstance("Ed25519", "BC")
        KeyPair(
            factory.generatePublic(X509EncodedKeySpec(pubFile.readBytes())),
            factory.generatePrivate(PKCS8EncodedKeySpec(pkcs8)),
        )
    }

    fun publicKeyBlob(): ByteArray? {
        if (!hasKey()) return null
        // Ed25519 SPKI is a fixed 12-byte header followed by the 32 raw key bytes.
        val raw = pubFile.readBytes().takeLast(ED25519_KEY_LEN).toByteArray()
        return Buffer.PlainBuffer().putString(KEY_TYPE).putBytes(raw).compactData
    }

    /** One `authorized_keys` line. */
    fun openSshPublicKey(): String? = publicKeyBlob()?.let { "$KEY_TYPE ${HostKeys.toBase64(it)} rssh" }

    fun fingerprint(): String? = publicKeyBlob()?.let(HostKeys::fingerprint)

    private fun wrapKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(WRAP_KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val spec = KeyGenParameterSpec.Builder(
            WRAP_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            .apply { init(spec) }
            .generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val WRAP_KEY_ALIAS = "rssh-key-wrap"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_IV_LEN = 12
        const val GCM_TAG_BITS = 128
        const val KEY_TYPE = "ssh-ed25519"
        const val ED25519_KEY_LEN = 32
    }
}
