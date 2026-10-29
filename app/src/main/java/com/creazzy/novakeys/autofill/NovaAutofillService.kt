package com.creazzy.novakeys.autofill

import android.app.assist.AssistStructure
import android.os.CancellationSignal
import android.service.autofill.*
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import com.creazzy.novakeys.R
import com.creazzy.novakeys.data.PasswordDatabase
import kotlinx.coroutines.runBlocking
import android.content.Context

class NovaAutofillService : AutofillService() {
    override fun onFillRequest(request: FillRequest, cancellationSignal: CancellationSignal, callback: FillCallback) {
        if (cancellationSignal.isCanceled) { callback.onFailure("Request cancelled"); return }
        val structure = request.fillContexts.lastOrNull()?.structure
        if (structure == null) { callback.onSuccess(null); return }
        val parsed = parseStructure(structure) ?: run { callback.onSuccess(null); return }
        if (parsed.usernameId == null && parsed.passwordId == null) { callback.onSuccess(null); return }
        try {
            val entries = runBlocking {
                PasswordDatabase.get(applicationContext).passwords().forWebsite(parsed.domain)
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
            callback.onSuccess(response.build())
        } catch (t: Throwable) {
            callback.onFailure("Vault unavailable. Unlock NovaKeys and try again.")
        }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        val structure = request.fillContexts.lastOrNull()?.structure
        val parsed = structure?.let(::parseStructure)
        if (parsed == null || (parsed.usernameId == null && parsed.passwordId == null)) {
            callback.onSuccess(); return
        }
        val username = parsed.usernameValue.orEmpty()
        val password = parsed.passwordValue.orEmpty()
        if (password.isBlank() || isPaymentField(parsed)) { callback.onSuccess(); return }
        try {
            runBlocking {
                PasswordDatabase.get(applicationContext).passwords().upsert(
                    com.creazzy.novakeys.data.PasswordEntry(
                        label = parsed.domain,
                        username = username,
                        password = password,
                        website = parsed.domain
                    )
                )
            }
            callback.onSuccess()
        } catch (_: Throwable) { callback.onFailure("Could not save credentials") }
    }

    private data class Parsed(
        val domain: String, val usernameId: android.view.autofill.AutofillId?,
        val passwordId: android.view.autofill.AutofillId?,
        val usernameValue: String?, val passwordValue: String?,
        val payment: Boolean
    )
    private fun isPaymentField(p: Parsed) = p.payment
    private fun parseStructure(structure: AssistStructure): Parsed? {
        var userId: android.view.autofill.AutofillId? = null
        var passId: android.view.autofill.AutofillId? = null
        var userValue: String? = null; var passValue: String? = null
        var domain = ""; var payment = false
        fun visit(node: AssistStructure.ViewNode) {
            val hints = (0 until node.autofillHintsCount).mapNotNull { node.getAutofillHintAt(it) }
            val hintText = (hints + listOf(node.hint.orEmpty(), node.idEntry.orEmpty())).joinToString(" ").lowercase()
            if (hintText.contains("credit") || hintText.contains("card") || hintText.contains("cc-") ||
                hintText.contains("cvc") || hintText.contains("cvv") || hintText.contains("iban") ||
                hintText.contains("payment")) payment = true
            if (node.webDomain?.isNotBlank() == true) domain = node.webDomain!!
            if (domain.isBlank() && node.idEntry?.contains("://") == true) domain = node.idEntry!!
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
        if (domain.isBlank()) domain = "unknown"
        return Parsed(domain, userId, passId, userValue, passValue, payment)
    }

    private fun presentation(title: String, subtitle: String): RemoteViews =
        RemoteViews(packageName, android.R.layout.simple_list_item_2).apply {
            setTextViewText(android.R.id.text1, title)
            setTextViewText(android.R.id.text2, subtitle)
        }

    override fun onConnected() { super.onConnected() }
    override fun onDisconnected() { super.onDisconnected() }
}
