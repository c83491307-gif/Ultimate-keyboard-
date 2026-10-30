package com.creazzy.novakeys.data

import java.util.Base64
import javax.crypto.spec.SecretKeySpec
import org.junit.Assert.*
import org.junit.Test

class DatabaseKeyManagerTest {
    private val key = SecretKeySpec(ByteArray(32) { (it + 1).toByte() }, "AES")
    private val passphrase = ByteArray(32) { (it * 3).toByte() }

    @Test fun wrappedPassphraseRoundTripsWithoutChangingBytes() {
        val encoded = DatabaseKeyManager.encodeAndWrap(passphrase, key)
        assertArrayEquals(passphrase, DatabaseKeyManager.decodeAndUnwrap(encoded, key))
    }

    @Test fun wrongKeystoreKeyIsReportedAsCorruption() {
        val encoded = DatabaseKeyManager.encodeAndWrap(passphrase, key)
        val otherKey = SecretKeySpec(ByteArray(32) { 99.toByte() }, "AES")
        assertThrows(DatabaseKeyManager.KeyCorruptedException::class.java) {
            DatabaseKeyManager.decodeAndUnwrap(encoded, otherKey)
        }
    }

    @Test fun tamperedCiphertextIsRejected() {
        val encoded = DatabaseKeyManager.encodeAndWrap(passphrase, key)
        val bytes = Base64.getDecoder().decode(encoded)
        bytes[bytes.lastIndex] = (bytes.last().toInt() xor 1).toByte()
        val tampered = Base64.getEncoder().encodeToString(bytes)
        assertThrows(DatabaseKeyManager.KeyCorruptedException::class.java) {
            DatabaseKeyManager.decodeAndUnwrap(tampered, key)
        }
    }

    @Test fun malformedAndTruncatedEnvelopesAreRejected() {
        assertThrows(DatabaseKeyManager.KeyCorruptedException::class.java) {
            DatabaseKeyManager.decodeAndUnwrap("not-base64!", key)
        }
        val short = Base64.getEncoder().encodeToString(ByteArray(20))
        assertThrows(DatabaseKeyManager.KeyCorruptedException::class.java) {
            DatabaseKeyManager.decodeAndUnwrap(short, key)
        }
    }
}
