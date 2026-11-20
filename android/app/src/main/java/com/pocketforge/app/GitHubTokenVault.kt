package com.pocketforge.app

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Stores repository-scoped GitHub tokens encrypted with an Android Keystore AES-GCM key. */
class GitHubTokenVault(context: Context) {
    private val preferences = context.getSharedPreferences("pocketforge.github.credentials.v1", Context.MODE_PRIVATE)

    fun save(repository: String, token: String) {
        require(repository.matches(REPOSITORY_PATTERN)) { "Use owner/repository format." }
        require(token.isNotBlank()) { "Token cannot be empty." }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(token.toByteArray(Charsets.UTF_8))
        val payload = Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
        check(preferences.edit().putString(preferenceKey(repository), payload).commit()) {
            "Could not persist the encrypted token."
        }
    }

    fun read(repository: String): String? {
        require(repository.matches(REPOSITORY_PATTERN)) { "Use owner/repository format." }
        val payload = preferences.getString(preferenceKey(repository), null) ?: return null
        return runCatching {
            val bytes = Base64.decode(payload, Base64.NO_WRAP)
            require(bytes.size > IV_LENGTH) { "Invalid encrypted token." }
            val iv = bytes.copyOfRange(0, IV_LENGTH)
            val encrypted = bytes.copyOfRange(IV_LENGTH, bytes.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(TAG_BITS, iv))
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        }.getOrElse {
            clear(repository)
            null
        }
    }

    fun clear(repository: String) {
        require(repository.matches(REPOSITORY_PATTERN)) { "Use owner/repository format." }
        preferences.edit().remove(preferenceKey(repository)).commit()
    }

    private fun preferenceKey(repository: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(repository.lowercase().toByteArray(Charsets.UTF_8))
        return TOKEN_PREFIX + digest.joinToString("") { "%02x".format(it) }
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val KEY_ALIAS = "pocketforge.github.token.aes"
        const val TOKEN_PREFIX = "encrypted_token_"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_LENGTH = 12
        const val TAG_BITS = 128
        val REPOSITORY_PATTERN = Regex("^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$")
    }
}
