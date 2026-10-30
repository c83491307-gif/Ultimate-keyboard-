package com.creazzy.novakeys.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.SecureRandom
import java.util.Base64 as JvmBase64
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Wraps the SQLCipher passphrase with an Android Keystore AES-GCM key.
 *
 * Important: once a wrapped value exists, a missing Keystore key is treated as
 * a recovery error. We never silently generate a replacement key, because doing
 * so would make the existing encrypted database appear irrecoverably corrupt.
 */
class DatabaseKeyManager(private val context: Context) {
    private val alias = "novakeys_db_wrap_v1"
    private val prefs = context.getSharedPreferences("db_key_wrap", Context.MODE_PRIVATE)

    open class KeyRecoveryException(message: String, cause: Throwable? = null) : Exception(message, cause)
    class KeyLostException(cause: Throwable? = null) :
        KeyRecoveryException("The database wrapping key is missing. Restore the encrypted vault backup.", cause)
    class KeyCorruptedException(cause: Throwable? = null) :
        KeyRecoveryException("The stored database key is damaged or cannot be authenticated. The vault was not changed.", cause)

    private fun findKeystoreKey(): SecretKey? {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return ks.getKey(alias, null) as? SecretKey
    }

    private fun createKeystoreKey(): SecretKey {
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setKeySize(256)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    @Synchronized
    @Throws(KeyRecoveryException::class)
    fun passphrase(): ByteArray {
        val encoded = prefs.getString("wrapped", null)
        if (encoded != null) {
            val key = try {
                findKeystoreKey() ?: throw KeyLostException()
            } catch (e: KeyRecoveryException) {
                throw e
            } catch (e: Exception) {
                throw KeyRecoveryException("Could not access the database wrapping key. No database data was changed.", e)
            }

            return try {
                val wrapped = decodeAndUnwrap(encoded, key)
                if (wrapped.size != PASSPHRASE_BYTES) throw KeyCorruptedException()
                wrapped
            } catch (e: KeyRecoveryException) {
                throw e
            } catch (e: Exception) {
                throw KeyCorruptedException(e)
            }
        }

        // First installation only: no wrapped value exists, so creating a key is safe.
        val key = try {
            findKeystoreKey() ?: createKeystoreKey()
        } catch (e: Exception) {
            throw KeyRecoveryException("Could not create the secure database key.", e)
        }
        val raw = ByteArray(PASSPHRASE_BYTES).also(SecureRandom()::nextBytes)
        val encodedWrapped = try {
            encodeAndWrap(raw, key)
        } catch (e: Exception) {
            throw KeyRecoveryException("Could not protect the database key.", e)
        }
        if (!prefs.edit().putString("wrapped", encodedWrapped).commit()) {
            throw KeyRecoveryException("Could not persist the secure database key.")
        }
        return raw
    }

    companion object {
        private const val IV_BYTES = 12
        private const val PASSPHRASE_BYTES = 32
        private const val TAG_BITS = 128
        private const val MIN_WRAPPED_BYTES = IV_BYTES + PASSPHRASE_BYTES + TAG_BITS / 8

        internal fun decodeAndUnwrap(encoded: String, key: SecretKey): ByteArray {
            val all = try {
                JvmBase64.getDecoder().decode(encoded)
            } catch (e: IllegalArgumentException) {
                throw KeyCorruptedException(e)
            }
            if (all.size < MIN_WRAPPED_BYTES) throw KeyCorruptedException()
            val iv = all.copyOfRange(0, IV_BYTES)
            val ciphertext = all.copyOfRange(IV_BYTES, all.size)
            return try {
                Cipher.getInstance("AES/GCM/NoPadding").run {
                    init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
                    doFinal(ciphertext)
                }
            } catch (e: AEADBadTagException) {
                throw KeyCorruptedException(e)
            }
        }

        internal fun encodeAndWrap(raw: ByteArray, key: SecretKey): String {
            require(raw.size == PASSPHRASE_BYTES)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            return JvmBase64.getEncoder().encodeToString(cipher.iv + cipher.doFinal(raw))
        }
    }
}
