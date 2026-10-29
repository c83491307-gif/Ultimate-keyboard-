package com.creazzy.novakeys.security

import java.security.SecureRandom

object PasswordTools {
    private const val LOWER = "abcdefghijkmnopqrstuvwxyz"
    private const val UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ"
    private const val DIGITS = "23456789"
    private const val SYMBOLS = "!@#$%^&*()-_=+[]{}:,.?"
    fun generate(length: Int = 20, symbols: Boolean = true): String {
        require(length in 12..128) { "Length must be 12..128" }
        val alphabet = LOWER + UPPER + DIGITS + if (symbols) SYMBOLS else ""
        val random = SecureRandom()
        val chars = CharArray(length) { alphabet[random.nextInt(alphabet.length)] }
        // Ensure the output includes each class, then shuffle with SecureRandom.
        val required = listOf(LOWER, UPPER, DIGITS) + if (symbols) listOf(SYMBOLS) else emptyList()
        required.forEachIndexed { i, set -> chars[i] = set[random.nextInt(set.length)] }
        for (i in chars.lastIndex downTo 1) {
            val j = random.nextInt(i + 1); val t = chars[i]; chars[i] = chars[j]; chars[j] = t
        }
        return String(chars)
    }

    fun weakOrRepeated(passwords: List<String>): Map<String, String> {
        val counts = passwords.groupingBy { it }.eachCount()
        return passwords.distinct().mapNotNull { p ->
            val reason = when {
                counts[p]!! > 1 -> "Repeated password"
                p.length < 12 -> "Short password (<12)"
                p.lowercase() == p || p.uppercase() == p -> "Only one letter case"
                p.all { it.isLetterOrDigit() } -> "No symbols"
                else -> null
            }
            reason?.let { p to it }
        }.toMap()
    }
}
