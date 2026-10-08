package com.sublearn.platform

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.sublearn.domain.SecretStore
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** AES-GCM ciphertext in noBackupFilesDir; the raw provider key never enters DataStore or exports. */
class AndroidKeystoreSecretStore(context: Context) : SecretStore {
    private val directory = File(context.applicationContext.noBackupFilesDir, "secure-keys")
    private val alias = "com.sublearn.provider-secrets.v1"

    override suspend fun read(providerId: String): String? = withContext(Dispatchers.IO) {
        synchronized(lock) {
            val file = secretFile(providerId)
            if (!file.exists()) return@synchronized null
            val bytes = file.readBytes()
            require(bytes.size > IV_LENGTH) { "Stored secret is damaged" }
            val iv = bytes.copyOfRange(0, IV_LENGTH)
            val ciphertext = bytes.copyOfRange(IV_LENGTH, bytes.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(TAG_BITS, iv))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        }
    }

    override suspend fun write(providerId: String, secret: String) = withContext(Dispatchers.IO) {
        synchronized(lock) {
            directory.mkdirs()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            val iv = cipher.iv
            val ciphertext = cipher.doFinal(secret.toByteArray(Charsets.UTF_8))
            secretFile(providerId).writeBytes(iv + ciphertext)
        }
    }

    override suspend fun delete(providerId: String) = withContext(Dispatchers.IO) {
        synchronized(lock) { secretFile(providerId).delete(); Unit }
    }

    private fun secretFile(providerId: String): File {
        require(providerId.matches(Regex("[a-z0-9_-]{1,32}"))) { "Invalid secret identifier" }
        return File(directory, "$providerId.bin")
    }

    private fun getOrCreateKey(): SecretKey {
        val store = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_LENGTH = 12
        const val TAG_BITS = 128
        val lock = Any()
    }
}
