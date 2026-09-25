package com.example

import com.example.crypto.CryptoEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File

class ExampleUnitTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testCryptoEngine_EncryptionAndDecryptionZeroLoss() {
        val originalText = "TeleVault Zero-Loss Large File Backup Test Data 1234567890!@#$%^&*()"
        val originalBytes = originalText.toByteArray(Charsets.UTF_8)
        val originalSha256 = CryptoEngine.calculateSha256(ByteArrayInputStream(originalBytes))

        val encryptedFile = tempFolder.newFile("test_encrypted.enc")
        val restoredFile = tempFolder.newFile("test_restored.txt")
        val passphrase = "SecretPassword!2026"

        // Encrypt
        CryptoEngine.encryptFile(
            inputStream = ByteArrayInputStream(originalBytes),
            destinationFile = encryptedFile,
            passphrase = passphrase
        )

        assertTrue(encryptedFile.exists())
        assertTrue(encryptedFile.length() > originalBytes.size) // header + salt + iv + gcm tag
        assertTrue(CryptoEngine.isTeleVaultEncrypted(encryptedFile))

        // Decrypt
        CryptoEngine.decryptFile(
            encryptedFile = encryptedFile,
            destinationFile = restoredFile,
            passphrase = passphrase
        )

        val restoredText = restoredFile.readText(Charsets.UTF_8)
        assertEquals(originalText, restoredText)

        val restoredSha256 = CryptoEngine.calculateFileSha256(restoredFile)
        assertEquals(originalSha256, restoredSha256)
    }

    @Test
    fun testCryptoEngine_WrongPassphraseFailsGcmAuthentication() {
        val originalBytes = "Top Secret Credentials".toByteArray(Charsets.UTF_8)
        val encryptedFile = tempFolder.newFile("secret.enc")
        val restoredFile = tempFolder.newFile("restored.txt")

        CryptoEngine.encryptFile(
            inputStream = ByteArrayInputStream(originalBytes),
            destinationFile = encryptedFile,
            passphrase = "CorrectPassword"
        )

        // Attempt decrypt with wrong password
        assertThrows(Exception::class.java) {
            CryptoEngine.decryptFile(
                encryptedFile = encryptedFile,
                destinationFile = restoredFile,
                passphrase = "WrongPassword"
            )
        }
    }
}
