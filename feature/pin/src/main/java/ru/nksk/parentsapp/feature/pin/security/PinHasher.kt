package ru.nksk.parentsapp.feature.pin.security

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Salts and hashes the PIN; the plaintext PIN never leaves this object.
 * PBKDF2 with HMAC-SHA256 is the widely supported KDF on Android; the cost
 * factor lives in the stored record so it can grow without a migration.
 */
object PinHasher {
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val SALT_BYTES = 16
    private const val KEY_BITS = 256

    fun newSalt(): ByteArray = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }

    fun hash(pin: CharArray, salt: ByteArray, iterations: Int): ByteArray =
        SecretKeyFactory.getInstance(ALGORITHM)
            .generateSecret(PBEKeySpec(pin, salt, iterations, KEY_BITS))
            .encoded
}
