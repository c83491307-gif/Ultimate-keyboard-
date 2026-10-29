package com.creazzy.novakeys.data

import android.content.ContentResolver
import android.net.Uri
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

/**
 * Handles common Chrome CSV columns (name,url,username,password) and Bitwarden
 * columns (folder,favorite,type,name,notes,fields,login_uri,login_username,login_password).
 * CSV export contains plaintext secrets: callers must use a user-selected SAF URI and warn first.
 */
object PasswordCsv {
    private fun parseLine(line: String): List<String> {
        val out = mutableListOf<String>(); val cell = StringBuilder(); var quoted = false; var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && quoted && i + 1 < line.length && line[i + 1] == '"' -> { cell.append('"'); i++ }
                c == '"' -> quoted = !quoted
                c == ',' && !quoted -> { out += cell.toString(); cell.setLength(0) }
                else -> cell.append(c)
            }
            i++
        }
        out += cell.toString()
        return out
    }
    private fun quote(s: String) = "\"" + s.replace("\"", "\"\"") + "\""

    fun import(resolver: ContentResolver, uri: Uri): List<PasswordEntry> {
        resolver.openInputStream(uri).use { stream ->
            requireNotNull(stream) { "Cannot open CSV" }
            val reader = BufferedReader(InputStreamReader(stream, Charsets.UTF_8))
            val header = reader.readLine()?.removePrefix("\uFEFF") ?: return emptyList()
            val columns = parseLine(header).map { it.trim().lowercase() }
            fun value(row: List<String>, vararg names: String): String {
                val idx = names.firstNotNullOfOrNull { columns.indexOf(it).takeIf { n -> n >= 0 } } ?: return ""
                return row.getOrNull(idx).orEmpty()
            }
            return reader.lineSequence().mapNotNull { line ->
                val row = parseLine(line)
                val username = value(row, "username", "login_username", "user name", "email")
                val password = value(row, "password", "login_password")
                val url = value(row, "url", "login_uri", "website")
                if (password.isBlank() || (username.isBlank() && url.isBlank())) null
                else PasswordEntry(label = value(row, "name", "label").ifBlank { url },
                    username = username, password = password, website = url,
                    folder = value(row, "folder").ifBlank { "Imported" })
            }.toList()
        }
    }

    fun export(resolver: ContentResolver, uri: Uri, entries: List<PasswordEntry>) {
        resolver.openOutputStream(uri).use { stream ->
            requireNotNull(stream) { "Cannot open destination" }
            OutputStreamWriter(stream, Charsets.UTF_8).use { writer ->
                writer.appendLine("name,url,username,password,folder")
                entries.forEach { e ->
                    writer.appendLine(listOf(e.label, e.website, e.username, e.password, e.folder)
                        .joinToString(",") { quote(it) })
                }
            }
        }
    }
}
