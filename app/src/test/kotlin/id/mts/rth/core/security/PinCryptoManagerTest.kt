package id.mts.rth.core.security

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit Test untuk memverifikasi fungsionalitas algoritma hashing PBKDF2WithHmacSHA256
 * pada PinCryptoManager.
 */
class PinCryptoManagerTest {

    @Test
    fun `test generate salt produces 16 random bytes and unique each call`() {
        val salt1 = PinCryptoManager.generateSalt()
        val salt2 = PinCryptoManager.generateSalt()

        assertEquals(16, salt1.size)
        assertEquals(16, salt2.size)
        assertFalse(salt1.contentEquals(salt2))
    }

    @Test
    fun `test salt base64 encoding and decoding roundtrip`() {
        val originalSalt = PinCryptoManager.generateSalt()
        val base64Str = PinCryptoManager.saltToBase64(originalSalt)
        val decodedSalt = PinCryptoManager.base64ToSalt(base64Str)

        assertArrayEquals(originalSalt, decodedSalt)
    }

    @Test
    fun `test hash pin is deterministic for identical pin and salt`() {
        val pin = "123456"
        val salt = PinCryptoManager.generateSalt()

        val hash1 = PinCryptoManager.hashPin(pin, salt)
        val hash2 = PinCryptoManager.hashPin(pin, salt)

        assertEquals(hash1, hash2)
        assertTrue(hash1.isNotBlank())
    }

    @Test
    fun `test different salts produce distinct hashes for same pin`() {
        val pin = "123456"
        val salt1 = PinCryptoManager.generateSalt()
        val salt2 = PinCryptoManager.generateSalt()

        val hash1 = PinCryptoManager.hashPin(pin, salt1)
        val hash2 = PinCryptoManager.hashPin(pin, salt2)

        assertNotEquals(hash1, hash2)
    }

    @Test
    fun `test verify pin with correct pin returns true`() {
        val pin = "654321"
        val salt = PinCryptoManager.generateSalt()
        val saltBase64 = PinCryptoManager.saltToBase64(salt)
        val hashBase64 = PinCryptoManager.hashPin(pin, salt)

        val isValid = PinCryptoManager.verifyPin(pin, saltBase64, hashBase64)
        assertTrue(isValid)
    }

    @Test
    fun `test verify pin with incorrect pin returns false`() {
        val correctPin = "123456"
        val wrongPin = "654321"
        val salt = PinCryptoManager.generateSalt()
        val saltBase64 = PinCryptoManager.saltToBase64(salt)
        val hashBase64 = PinCryptoManager.hashPin(correctPin, salt)

        val isValid = PinCryptoManager.verifyPin(wrongPin, saltBase64, hashBase64)
        assertFalse(isValid)
    }

    @Test
    fun `test verify pin with malformed salt or hash handles safely without crashing`() {
        val result = PinCryptoManager.verifyPin("123456", "invalid-salt", "invalid-hash")
        assertFalse(result)
    }
}
