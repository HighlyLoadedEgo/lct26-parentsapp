package ru.nksk.parentsapp.feature.pin.access

import java.io.IOException
import kotlin.io.encoding.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** PBKDF2-HMAC-SHA1 is available on Android API 24; no API 26 Base64 dependency. */
internal class PinHasher {
    fun create(pin: String): String {
        requireValidPin(pin)
        val salt = ByteArray(SALT_BYTES).also(SecureRandom()::nextBytes)
        val verifier = derive(pin, salt, ITERATIONS)
        return "1|pbkdf2-sha1|$ITERATIONS|${salt.toHex()}|${verifier.toHex()}"
    }

    fun validate(record: String) { parse(record) }

    fun verify(pin: String, record: String): Boolean {
        requireValidPin(pin)
        val parsed = parse(record)
        val actual = derive(pin, parsed.salt, parsed.iterations, parsed.algorithm)
        return try {
            MessageDigest.isEqual(actual, parsed.verifier)
        } finally {
            actual.fill(0)
        }
    }

    private fun derive(pin: String, salt: ByteArray, iterations: Int, algorithm: String = "PBKDF2WithHmacSHA1"): ByteArray {
        val password = pin.toCharArray()
        val specification = PBEKeySpec(password, salt, iterations, VERIFIER_BYTES * 8)
        return try {
            SecretKeyFactory.getInstance(algorithm).generateSecret(specification).encoded
        } finally {
            specification.clearPassword()
            password.fill('\u0000')
        }
    }

    private fun parse(record: String): Record {
        if (record.length > 256) throw IOException("Invalid parent PIN record")
        val parts = record.split('|')
        if (parts.size == 4 && parts[0] == "1") {
            val iterations = parts[1].toIntOrNull()?.takeIf { it in 10_000..1_000_000 }
                ?: throw IOException("Invalid legacy PIN work factor")
            val salt = try { Base64.decode(parts[2]) } catch (error: IllegalArgumentException) {
                throw IOException("Invalid legacy PIN salt", error)
            }
            val verifier = try { Base64.decode(parts[3]) } catch (error: IllegalArgumentException) {
                throw IOException("Invalid legacy PIN verifier", error)
            }
            if (salt.size != SALT_BYTES || verifier.size != VERIFIER_BYTES) throw IOException("Invalid legacy PIN length")
            return Record(iterations, salt, verifier, "PBKDF2WithHmacSHA256")
        }
        if (parts.size != 5 || parts[0] != "1" || parts[1] != "pbkdf2-sha1") {
            throw IOException("Unsupported parent PIN record")
        }
        val iterations = parts[2].toIntOrNull()
            ?.takeIf { it in 10_000..1_000_000 }
            ?: throw IOException("Invalid parent PIN work factor")
        return Record(iterations, parts[3].fromHex(SALT_BYTES), parts[4].fromHex(VERIFIER_BYTES))
    }

    private fun ByteArray.toHex(): String = buildString(size * 2) {
        for (byte in this@toHex) {
            val value = byte.toInt() and 0xff
            append(HEX[value ushr 4])
            append(HEX[value and 0xf])
        }
    }

    private fun String.fromHex(expectedBytes: Int): ByteArray {
        if (length != expectedBytes * 2) throw IOException("Invalid parent PIN verifier length")
        return ByteArray(expectedBytes) { index ->
            val high = HEX.indexOf(this[index * 2])
            val low = HEX.indexOf(this[index * 2 + 1])
            if (high < 0 || low < 0) throw IOException("Invalid parent PIN verifier encoding")
            ((high shl 4) or low).toByte()
        }
    }

    private data class Record(val iterations: Int, val salt: ByteArray, val verifier: ByteArray,
        val algorithm: String = "PBKDF2WithHmacSHA1")

    private companion object {
        const val ITERATIONS = 210_000
        const val SALT_BYTES = 16
        const val VERIFIER_BYTES = 32
        const val HEX = "0123456789abcdef"
    }
}
