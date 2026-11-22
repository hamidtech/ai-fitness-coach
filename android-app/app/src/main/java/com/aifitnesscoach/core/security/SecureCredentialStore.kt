package com.aifitnesscoach.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Abstraction for storing sensitive credentials (external AI provider API keys,
 * optional user-supplied Hugging Face token) separately from Room/DataStore backups and exports.
 */
interface SecretStore {
    fun getSecret(key: String): String?
    fun putSecret(key: String, value: String?)
    fun removeSecret(key: String)
    fun clearAllSecrets()
    fun hasSecret(key: String): Boolean = !getSecret(key).isNullOrBlank()
}

/**
 * Pure JVM in-memory secret store for deterministic unit testing.
 */
class InMemorySecretStore : SecretStore {
    private val map = linkedMapOf<String, String>()

    override fun getSecret(key: String): String? = map[key]

    override fun putSecret(key: String, value: String?) {
        if (value.isNullOrBlank()) {
            map.remove(key)
        } else {
            map[key] = value.trim()
        }
    }

    override fun removeSecret(key: String) {
        map.remove(key)
    }

    override fun clearAllSecrets() {
        map.clear()
    }
}

/**
 * Android Keystore AES-256-GCM backed vault stored strictly in app-private filesDir.
 * Excluded from Android Auto Backup (allowBackup=false) and never included in workout exports or logs.
 */
class AndroidKeystoreSecretStore(private val context: Context) : SecretStore {

    private val lock = Any()
    private val gson = Gson()
    private val vaultFile = File(context.filesDir, VAULT_FILE_NAME)

    @Volatile
    private var cachedSecrets: MutableMap<String, String>? = null

    override fun getSecret(key: String): String? = synchronized(lock) {
        loadMap()[key]
    }

    override fun putSecret(key: String, value: String?) = synchronized(lock) {
        val current = loadMap().toMutableMap()
        if (value.isNullOrBlank()) {
            current.remove(key)
        } else {
            current[key] = value.trim()
        }
        saveMap(current)
    }

    override fun removeSecret(key: String) = synchronized(lock) {
        val current = loadMap().toMutableMap()
        if (current.remove(key) != null) {
            saveMap(current)
        }
    }

    override fun clearAllSecrets() = synchronized(lock) {
        cachedSecrets = mutableMapOf()
        if (vaultFile.exists()) {
            vaultFile.delete()
        }
    }

    private fun loadMap(): MutableMap<String, String> {
        cachedSecrets?.let { return it }
        if (!vaultFile.exists()) {
            return mutableMapOf<String, String>().also { cachedSecrets = it }
        }
        val decoded = runCatching {
            val raw = vaultFile.readBytes()
            if (raw.size <= IV_SIZE_BYTES + 4) return@runCatching mutableMapOf<String, String>()
            val buffer = ByteBuffer.wrap(raw)
            val ivLen = buffer.int
            if (ivLen != IV_SIZE_BYTES || raw.size <= 4 + ivLen) {
                return@runCatching mutableMapOf<String, String>()
            }
            val iv = ByteArray(ivLen)
            buffer.get(iv)
            val cipherText = ByteArray(buffer.remaining())
            buffer.get(cipherText)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
            val plainJson = String(cipher.doFinal(cipherText), Charsets.UTF_8)
            val type = object : TypeToken<MutableMap<String, String>>() {}.type
            gson.fromJson<MutableMap<String, String>>(plainJson, type) ?: mutableMapOf()
        }.getOrElse {
            mutableMapOf()
        }
        cachedSecrets = decoded
        return decoded
    }

    private fun saveMap(map: MutableMap<String, String>) {
        cachedSecrets = map
        if (map.isEmpty()) {
            if (vaultFile.exists()) vaultFile.delete()
            return
        }
        val plainBytes = gson.toJson(map).toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val iv = cipher.iv
        val cipherText = cipher.doFinal(plainBytes)
        val out = ByteBuffer.allocate(4 + iv.size + cipherText.size)
            .putInt(iv.size)
            .put(iv)
            .put(cipherText)
            .array()
        val tempFile = File(context.filesDir, "$VAULT_FILE_NAME.tmp")
        tempFile.writeBytes(out)
        if (vaultFile.exists()) vaultFile.delete()
        if (!tempFile.renameTo(vaultFile)) {
            vaultFile.writeBytes(out)
            tempFile.delete()
        }
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "aifc_provider_secrets"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_BITS = 128
        private const val IV_SIZE_BYTES = 12
        private const val VAULT_FILE_NAME = "secure_vault.bin"

        fun providerApiKeyAlias(providerId: String): String = "provider_api_key_$providerId"
        const val HUGGING_FACE_TOKEN_ALIAS: String = "hf_user_access_token"
    }
}
