package com.surfaceocean.nexttraceroute

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import com.google.gson.JsonObject
import java.io.File
import java.security.KeyStore
import java.time.Instant
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Ciphertext is excluded from backup; the key never leaves AndroidKeyStore. */
internal class V4TokenStore(context: Context, fileName: String = "nexttrace-v4-authorization.bin") {
    init { require(fileName.matches(Regex("[a-zA-Z0-9.-]+"))) }
    private val file = AtomicFile(File(context.noBackupFilesDir, fileName))

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }

    fun load(): V4Grant? = synchronized(LOCK) {
        if (!file.baseFile.exists() && !File(file.baseFile.path + ".bak").exists()) return@synchronized null
        runCatching {
            val bytes = file.openRead().use { input ->
                val buffer = ByteArray(16_385)
                var count = 0
                while (count < buffer.size) {
                    val read = input.read(buffer, count, buffer.size - count)
                    if (read == -1) break
                    count += read
                }
                buffer.copyOf(count)
            }
            require(bytes.size in 30..16_384 && bytes[0] == 1.toByte())
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(1, 13)))
            val text = cipher.doFinal(bytes.copyOfRange(13, bytes.size)).toString(Charsets.UTF_8)
            parseV4Grant(text, requireUnexpired = false) ?: error("Invalid stored authorization")
        // Keep an invalid configured state; never silently downgrade on decryption failure.
        }.getOrElse { V4Grant("", 0) }
    }

    fun save(grant: V4Grant) = synchronized(LOCK) {
        require(grant.usable())
        val text = JsonObject().apply {
            addProperty("token", grant.token)
            addProperty("expires_at", Instant.ofEpochMilli(grant.expiresAtMillis).toString())
        }.toString().toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = byteArrayOf(1) + cipher.iv + cipher.doFinal(text)
        var output: java.io.FileOutputStream? = null
        try {
            output = file.startWrite()
            output.write(encrypted)
            file.finishWrite(output)
        } catch (e: Exception) {
            file.failWrite(output)
            throw java.io.IOException("Unable to save NextTrace authorization")
        }
    }

    fun clear() = synchronized(LOCK) { file.delete() }

    companion object {
        private const val KEY_ALIAS = "nexttrace-v4-authorization"
        private val LOCK = Any()
    }
}
