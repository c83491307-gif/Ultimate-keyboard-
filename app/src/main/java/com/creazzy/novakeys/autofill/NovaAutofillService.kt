package com.creazzy.novakeys.autofill

import android.app.assist.AssistStructure
import android.os.Build
import android.os.CancellationSignal
import android.service.autofill.*
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import com.creazzy.novakeys.data.PasswordDatabase
import com.creazzy.novakeys.data.PasswordEntry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/** Autofill framework is available from Android 8.0 (API 26); older Android versions use the keyboard without this service. */
@RequiresApi(Build.VERSION_CODES.O)
class NovaAutofillService : AutofillService() {
    override fun onFillRequest(request: FillRequest, cancellationSignal: CancellationSignal, callback: FillCallback) {
        if (!isVaultUnlocked()) { callback.onSuccess(null); return }
        if (cancellationSignal.isCanceled) { callback.onFailure("Request cancelled"); return }
        val structure = request.fillContexts.lastOrNull()?.structure
        val parsed = structure?.let(::parseStructure)
        if (parsed == null || parsed.payment || parsed.domain.isBlank() ||
            parsed.domain == "unknown" || (parsed.usernameId == null && parsed.passwordId == null)) {
            callback.onSuccess(null); return
        }
        try {
            val entries = runBlocking {
                PasswordDatabase.get(applicationContext).passwords().observeAll().first()
                    .filter { normalizeDomain(it.website) == normalizeDomain(parsed.domain) }
            }
            if (entries.isEmpty()) { callback.onSuccess(null); return }
            val response = FillResponse.Builder()
            entries.take(5).forEach { entry ->
                val dataset = Dataset.Builder(presentation(entry.label, entry.username))
                parsed.usernameId?.let { id ->
                    dataset.setValue(id, AutofillValue.forText(entry.username), presentation(entry.label, entry.username))
                }
                parsed.passwordId?.let { id ->
                    dataset.setValue(id, AutofillValue.forText(entry.password), presentation(entry.label, "••••••••"))
                }
                response.addDataset(dataset.build())
            }
            val saveIds = listOfNotNull(parsed.usernameId, parsed.passwordId).toTypedArray()
            if (saveIds.isNotEmpty()) {
                response.setSaveInfo(SaveInfo.Builder(
                    SaveInfo.SAVE_DATA_TYPE_USERNAME or SaveInfo.SAVE_DATA_TYPE_PASSWORD, saveIds
                ).build())
            }
            callback.onSuccess(response.build())
        } catch (_: Throwable) {
            callback.onFailure("Vault unavailable. Open NovaKeys and verify the vault is accessible.")
        }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        if (!isVaultUnlocked()) { callback.onFailure("Unlock NovaKeys before saving credentials."); return }
        val structure = request.fillContexts.lastOrNull()?.structure
        val parsed = structure?.let(::parseStructure)
        if (parsed == null || parsed.payment) { callback.onSuccess(); return }
        val username = parsed.usernameValue.orEmpty()
        val password = parsed.passwordValue.orEmpty()
        if (password.isBlank() || parsed.domain.isBlank() || parsed.domain == "unknown") {
            callback.onSuccess(); return
        }
        try {
            runBlocking {
                val dao = PasswordDatabase.get(applicationContext).passwords()
                val existing = dao.forWebsite(parsed.domain).firstOrNull { it.username == username }
                dao.upsert(PasswordEntry(
                    id = existing?.id ?: 0, label = parsed.domain, username = username, password = password,
                    website = parsed.domain, folder = existing?.folder ?: "General",
                    createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                ))
            }
            callback.onSuccess()
        } catch (_: Throwable) { callback.onFailure("Could not save credentials") }
    }

    private data class Parsed(
        val domain: String, val usernameId: AutofillId?, val passwordId: AutofillId?,
        val usernameValue: String?, val passwordValue: String?, val payment: Boolean
    )

    private fun parseStructure(structure: AssistStructure): Parsed {
        var userId: AutofillId? = null
        var passId: AutofillId? = null
        var userValue: String? = null
        var passValue: String? = null
        var domain = ""
        var payment = false
        fun visit(node: AssistStructure.ViewNode) {
            val hints = node.autofillHints?.toList().orEmpty()
            val hintText = (hints + listOf(node.hint.orEmpty(), node.idEntry.orEmpty())).joinToString(" ").lowercase()
            if (listOf("credit", "card", "cc-", "cvc", "cvv", "iban", "payment", "expiration", "cardnumber")
                    .any { hintText.contains(it) }) payment = true
            node.webDomain?.takeIf { it.isNotBlank() }?.let { domain = normalizeDomain(it) }
            val value = node.autofillValue?.takeIf { it.isText }?.textValue?.toString()
            when {
                hints.any { it.contains("password", true) } || hintText.contains("password") -> {
                    passId = node.autofillId; passValue = value
                }
                hints.any { it.contains("username", true) || it.contains("emailAddress", true) } ||
                    hintText.contains("username") || hintText.contains("email") -> {
                    userId = node.autofillId; userValue = value
                }
            }
            for (i in 0 until node.childCount) visit(node.getChildAt(i))
        }
        for (i in 0 until structure.windowNodeCount) visit(structure.getWindowNodeAt(i).rootViewNode)
        return Parsed(domain.ifBlank { "unknown" }, userId, passId, userValue, passValue, payment)
    }

    private fun isVaultUnlocked(): Boolean =
        getSharedPreferences("vault_session", MODE_PRIVATE).getLong("unlocked_until", 0L) > System.currentTimeMillis()

    private fun normalizeDomain(raw: String): String {
        val value = raw.trim().lowercase()
        val parsed = android.net.Uri.parse(if (value.contains("://")) value else "https://$value")
        return parsed.host?.removePrefix("www.")?.trimEnd('.').orEmpty()
    }

    private fun presentation(title: String, subtitle: String): RemoteViews =
        RemoteViews(packageName, android.R.layout.simple_list_item_2).apply {
            setTextViewText(android.R.id.text1, title)
            setTextViewText(android.R.id.text2, subtitle)
        }
}
