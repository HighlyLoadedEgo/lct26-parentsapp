package ru.nksk.parentsapp.feature.pin.security

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHasherTest {
    private val salt = ByteArray(16) { it.toByte() }

    @Test
    fun `same pin and salt reproduce the same hash`() {
        val first = PinHasher.hash("1234".toCharArray(), salt, 1_000)
        val second = PinHasher.hash("1234".toCharArray(), salt, 1_000)
        assertArrayEquals(first, second)
    }

    @Test
    fun `wrong pin produces a different hash`() {
        val first = PinHasher.hash("1234".toCharArray(), salt, 1_000)
        val second = PinHasher.hash("9999".toCharArray(), salt, 1_000)
        assertFalse(first.contentEquals(second))
    }

    @Test
    fun `different salts produce different hashes`() {
        val otherSalt = ByteArray(16) { (it + 1).toByte() }
        val first = PinHasher.hash("1234".toCharArray(), salt, 1_000)
        val second = PinHasher.hash("1234".toCharArray(), otherSalt, 1_000)
        assertFalse(first.contentEquals(second))
    }

    @Test
    fun `new salt is random and 16 bytes long`() {
        assertFalse(PinHasher.newSalt().contentEquals(PinHasher.newSalt()))
        assertTrue(PinHasher.newSalt().size == 16)
    }
}
