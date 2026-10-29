package com.creazzy.novakeys.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import java.security.SecureRandom

/**
 * Creates a random SQLCipher passphrase and wraps it with an AES-256 key held by Android Keystore.
 * The wrapped passphrase is stored in app-private SharedPreferences, never in source control.
 */
class DatabaseKeyManager(private val context: Context) {
    private val alias = "novakeys_db_wrap_v1"
    private val prefs = context.getSharedPreferences("db_key_wrap", Context.MODE_PRIVATE)

    private fun keystoreKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setKeySize(256)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build())
        return generator.generateKey()
    }

    @Synchronized fun passphrase(): ByteArray {
        val encoded = prefs.getString("wrapped", null)
        if (encoded != null) {
            val all = Base64.decode(encoded, Base64.NO_WRAP)
            val iv = all.copyOfRange(0, 12)
            val ciphertext = all.copyOfRange(12, all.size)
            return Cipher.getInstance("AES/GCM/NoPadding").run {
                init(Cipher.DECRYPT_MODE, keystoreKey(), GCMParameterSpec(128, iv))
                doFinal(ciphertext)
            }
        }
        val raw = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, keystoreKey())
        val wrapped = cipher.iv + cipher.doFinal(raw)
        check(prefs.edit().putString("wrapped", Base64.encodeToString(wrapped, Base64.NO_WRAP)).commit()) {
            "Could not persist database key"
        }
        return raw
    }
}
