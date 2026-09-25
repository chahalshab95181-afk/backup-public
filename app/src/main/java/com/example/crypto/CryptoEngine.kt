package com.example.crypto

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoEngine {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val KEY_LENGTH_BITS = 256
    private const val ITERATIONS = 10_000
    private const val SALT_LENGTH = 16
    private const val IV_LENGTH = 12 // Standard 96-bit IV for AES-GCM
    private const val GCM_TAG_LENGTH_BITS = 128

    val MAGIC_HEADER = byteArrayOf('T'.code.toByte(), 'V'.code.toByte(), 'L'.code.toByte(), 'T'.code.toByte())
    const val CURRENT_VERSION: Byte = 1

    private val secureRandom = SecureRandom()

    /**
     * Derives a 256-bit AES key from a passphrase and salt using PBKDF2.
     */
    private fun deriveKey(passphrase: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(passphrase, salt, ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    /**
     * Encrypts input stream to output file using AES-256-GCM.
     * File header:
     * [4 bytes MAGIC "TVLT"] [1 byte version] [16 bytes salt] [12 bytes IV] [AES-GCM Ciphertext + Tag]
     */
    fun encryptFile(
        inputStream: InputStream,
        destinationFile: File,
        passphrase: String,
        onProgress: ((bytesRead: Long) -> Unit)? = null
    ): Long {
        val salt = ByteArray(SALT_LENGTH).also { secureRandom.nextBytes(it) }
        val iv = ByteArray(IV_LENGTH).also { secureRandom.nextBytes(it) }
        val secretKey = deriveKey(passphrase.toCharArray(), salt)

        val cipher = Cipher.getInstance(ALGORITHM)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

        FileOutputStream(destinationFile).use { fos ->
            // Write Header
            fos.write(MAGIC_HEADER)
            fos.write(CURRENT_VERSION.toInt())
            fos.write(salt)
            fos.write(iv)

            CipherOutputStream(fos, cipher).use { cos ->
                val buffer = ByteArray(64 * 1024)
                var read: Int
                var total: Long = 0
                while (inputStream.read(buffer).also { read = it } != -1) {
                    cos.write(buffer, 0, read)
                    total += read
                    onProgress?.invoke(total)
                }
                cos.flush()
            }
        }
        return destinationFile.length()
    }

    /**
     * Decrypts an encrypted TeleVault file to a target destination.
     * Validates magic bytes, extracts salt & IV, and decrypts using AES-256-GCM.
     * Throws exception on invalid password or corrupted data.
     */
    fun decryptFile(
        encryptedFile: File,
        destinationFile: File,
        passphrase: String,
        onProgress: ((bytesWritten: Long) -> Unit)? = null
    ): Boolean {
        FileInputStream(encryptedFile).use { fis ->
            // Check Magic Header
            val magic = ByteArray(4)
            val readMagic = fis.read(magic)
            if (readMagic != 4 || !magic.contentEquals(MAGIC_HEADER)) {
                throw IllegalArgumentException("Not a valid TeleVault encrypted file (invalid header)")
            }

            // Version
            val version = fis.read()
            if (version != CURRENT_VERSION.toInt()) {
                throw IllegalArgumentException("Unsupported TeleVault format version: $version")
            }

            // Salt
            val salt = ByteArray(SALT_LENGTH)
            if (fis.read(salt) != SALT_LENGTH) {
                throw IllegalArgumentException("Corrupted file header: incomplete salt")
            }

            // IV
            val iv = ByteArray(IV_LENGTH)
            if (fis.read(iv) != IV_LENGTH) {
                throw IllegalArgumentException("Corrupted file header: incomplete IV")
            }

            val secretKey = deriveKey(passphrase.toCharArray(), salt)
            val cipher = Cipher.getInstance(ALGORITHM)
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

            CipherInputStream(fis, cipher).use { cis ->
                FileOutputStream(destinationFile).use { fos ->
                    val buffer = ByteArray(64 * 1024)
                    var read: Int
                    var total: Long = 0
                    while (cis.read(buffer).also { read = it } != -1) {
                        fos.write(buffer, 0, read)
                        total += read
                        onProgress?.invoke(total)
                    }
                    fos.flush()
                }
            }
        }
        return true
    }

    /**
     * Calculates streaming SHA-256 checksum of an input stream.
     * Essential for verifying zero loss and detect modified files.
     */
    fun calculateSha256(inputStream: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(64 * 1024)
        var read: Int
        while (inputStream.read(buffer).also { read = it } != -1) {
            digest.update(buffer, 0, read)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Calculates SHA-256 of a local file.
     */
    fun calculateFileSha256(file: File): String {
        return FileInputStream(file).use { calculateSha256(it) }
    }

    /**
     * Checks if a file has TeleVault magic header.
     */
    fun isTeleVaultEncrypted(file: File): Boolean {
        if (!file.exists() || file.length() < 33) return false
        return try {
            FileInputStream(file).use { fis ->
                val magic = ByteArray(4)
                if (fis.read(magic) != 4) return false
                magic.contentEquals(MAGIC_HEADER)
            }
        } catch (e: Exception) {
            false
        }
    }
}
