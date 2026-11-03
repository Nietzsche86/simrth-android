package id.mts.rth.core.security

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Pengelola keamanan kriptografi PIN offline untuk ruang kelas.
 * Menggunakan PBKDF2WithHmacSHA256 dengan 10.000 iterasi, salt 16-byte, dan kunci 256-bit.
 * Kompatibel dengan Android API 26+ dan JVM Unit Testing murni via java.util.Base64.
 */
object PinCryptoManager {
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 10_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_LENGTH_BYTES = 16

    private val secureRandom = SecureRandom()

    /**
     * Menghasilkan salt acak kriptografis 16-byte (128-bit).
     */
    fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_LENGTH_BYTES)
        secureRandom.nextBytes(salt)
        return salt
    }

    /**
     * Menghasilkan hash PIN 6 digit menggunakan salt yang diberikan.
     * Mengembalikan representasi Base64 dari hash 256-bit.
     */
    fun hashPin(pin: String, salt: ByteArray): String {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        val hash = factory.generateSecret(spec).encoded
        return Base64.getEncoder().encodeToString(hash)
    }

    /**
     * Mengonversi salt ByteArray ke format String Base64 untuk disimpan di DataStore.
     */
    fun saltToBase64(salt: ByteArray): String {
        return Base64.getEncoder().encodeToString(salt)
    }

    /**
     * Mengonversi String Base64 kembali ke ByteArray salt.
     */
    fun base64ToSalt(saltBase64: String): ByteArray {
        return Base64.getDecoder().decode(saltBase64)
    }

    /**
     * Memverifikasi input PIN offline terhadap salt dan hash yang tersimpan.
     * Berjalan cepat (< 50 ms) di perangkat Android.
     */
    fun verifyPin(pin: String, saltBase64: String, expectedHashBase64: String): Boolean {
        return try {
            val salt = base64ToSalt(saltBase64)
            val computedHash = hashPin(pin, salt)
            computedHash == expectedHashBase64
        } catch (_: Exception) {
            false
        }
    }
}
