package com.creazzy.novakeys.data

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Portable AES-GCM encrypted backup. The backup passphrase is never stored. */
object PasswordBackup {
    private const val FORMAT = "novakeys-encrypted-backup"
    private const val ITERATIONS = 210_000
    fun encode(entries: List<PasswordEntry>, passphrase: CharArray): String {
        require(passphrase.size >= 10) { "Use a backup passphrase with at least 10 characters." }
        val rows = JSONArray()
        entries.forEach { e -> rows.put(JSONObject().apply {
            put("id", e.id); put("label", e.label); put("username", e.username)
            put("password", e.password); put("website", e.website); put("folder", e.folder)
            put("createdAt", e.createdAt); put("updatedAt", e.updatedAt)
        }) }
        val plaintext = JSONObject().put("schemaVersion", 1).put("entries", rows).toString().toByteArray(Charsets.UTF_8)
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, derive(passphrase, salt), GCMParameterSpec(128, iv))
            val encrypted = cipher.doFinal(plaintext)
            JSONObject().apply {
                put("format", FORMAT); put("version", 1); put("kdf", "PBKDF2WithHmacSHA256")
                put("iterations", ITERATIONS); put("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
                put("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
                put("ciphertext", Base64.encodeToString(encrypted, Base64.NO_WRAP))
            }.toString(2)
        } finally { plaintext.fill(0) }
    }

    fun decode(serialized: String, passphrase: CharArray): List<PasswordEntry> {
        require(serialized.length <= 25_000_000) { "Backup file is too large." }
        val envelope = JSONObject(serialized)
        require(envelope.optString("format") == FORMAT && envelope.optInt("version") == 1) {
            "Not a supported NovaKeys encrypted backup."
        }
        val iterations = envelope.optInt("iterations", -1)
        require(iterations in 100_000..500_000) { "Unsupported backup encryption parameters." }
        val salt = Base64.decode(envelope.getString("salt"), Base64.DEFAULT)
        val iv = Base64.decode(envelope.getString("iv"), Base64.DEFAULT)
        val ciphertext = Base64.decode(envelope.getString("ciphertext"), Base64.DEFAULT)
        require(salt.size == 16 && iv.size == 12 && ciphertext.size <= 20_000_000) { "Invalid backup structure." }
        val plain = Cipher.getInstance("AES/GCM/NoPadding").run {
            init(Cipher.DECRYPT_MODE, derive(passphrase, salt, iterations), GCMParameterSpec(128, iv))
            doFinal(ciphertext)
        }
        return try {
            val root = JSONObject(String(plain, Charsets.UTF_8))
            require(root.optInt("schemaVersion") == 1) { "Unsupported backup data version." }
            val rows = root.getJSONArray("entries")
            require(rows.length() <= 100_000) { "Backup contains too many entries." }
            (0 until rows.length()).map { i ->
                val row = rows.getJSONObject(i)
                PasswordEntry(
                    id = row.optLong("id", 0L), label = row.optString("label"),
                    username = row.optString("username"), password = row.optString("password"),
                    website = row.optString("website"), folder = row.optString("folder", "General"),
                    createdAt = row.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = row.optLong("updatedAt", System.currentTimeMillis())
                )
            }
        } finally { plain.fill(0) }
    }

    private fun derive(passphrase: CharArray, salt: ByteArray, iterations: Int = ITERATIONS): SecretKeySpec {
        val spec = PBEKeySpec(passphrase, salt, iterations, 256)
        return try {
            SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded, "AES")
        } finally { spec.clearPassword() }
    }
}
