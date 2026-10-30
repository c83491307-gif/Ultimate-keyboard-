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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/** Autofill is available from API 26; this app currently requires API 31. */
@RequiresApi(Build.VERSION_CODES.O)
class NovaAutofillService : AutofillService() {
    private lateinit var serviceScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    override fun onDestroy() {
        if (::serviceScope.isInitialized) serviceScope.cancel()
        super.onDestroy()
    }

    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback
    ) {
        val completed = AtomicBoolean(false)
        if (cancellationSignal.isCanceled) return
        val job = serviceScope.launch(start = CoroutineStart.LAZY) {
            try {
                if (!isVaultUnlocked()) {
                    complete(completed) { callback.onSuccess(null) }
                    return@launch
                }

                val structure = request.fillContexts.lastOrNull()?.structure
                val parsed = structure?.let(::parseStructure)
                if (parsed == null || parsed.payment || parsed.domain.isBlank() ||
                    parsed.domain == "unknown" ||
                    (parsed.usernameId == null && parsed.passwordId == null)
                ) {
                    complete(completed) { callback.onSuccess(null) }
                    return@launch
                }

                val entries = PasswordDatabase.get(applicationContext).passwords().observeAll().first()
                    .filter { normalizeDomain(it.website) == parsed.domain }
                    .take(5)

                if (cancellationSignal.isCanceled) return@launch
                if (!isVaultUnlocked()) {
                    complete(completed) { callback.onSuccess(null) }
                    return@launch
                }
                if (entries.isEmpty()) {
                    complete(completed) { callback.onSuccess(null) }
                    return@launch
                }

                val response = FillResponse.Builder()
                entries.forEach { entry ->
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
                    response.setSaveInfo(
                        SaveInfo.Builder(
                            SaveInfo.SAVE_DATA_TYPE_USERNAME or SaveInfo.SAVE_DATA_TYPE_PASSWORD,
                            saveIds
                        ).build()
                    )
                }
                if (!cancellationSignal.isCanceled && isVaultUnlocked()) {
                    complete(completed) { callback.onSuccess(response.build()) }
                }
            } catch (cancelled: CancellationException) {
                // Cancellation is expected when Android abandons a fill request.
                throw cancelled
            } catch (error: Exception) {
                if (!cancellationSignal.isCanceled) {
                    complete(completed) {
                        callback.onFailure("Vault unavailable. Open NovaKeys and verify the vault is accessible.")
                    }
                }
            }
        }
        cancellationSignal.setOnCancelListener {
            job.cancel(CancellationException("Autofill request cancelled"))
        }
        if (cancellationSignal.isCanceled) job.cancel() else job.start()
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        val completed = AtomicBoolean(false)
        if (!isVaultUnlocked()) {
            complete(completed) { callback.onFailure("Unlock NovaKeys before saving credentials.") }
            return
        }

        val structure = request.fillContexts.lastOrNull()?.structure
        val parsed = structure?.let(::parseStructure)
        if (parsed == null || parsed.payment) {
            complete(completed) { callback.onSuccess() }
            return
        }
        val username = parsed.usernameValue.orEmpty()
        val password = parsed.passwordValue.orEmpty()
        if (password.isBlank() || parsed.domain.isBlank() || parsed.domain == "unknown") {
            complete(completed) { callback.onSuccess() }
            return
        }

        serviceScope.launch {
            try {
                if (!isVaultUnlocked()) {
                    complete(completed) { callback.onFailure("Unlock NovaKeys before saving credentials.") }
                    return@launch
                }
                val dao = PasswordDatabase.get(applicationContext).passwords()
                val existing = dao.forWebsite(parsed.domain).firstOrNull { it.username == username }
                if (!isVaultUnlocked()) {
                    complete(completed) { callback.onFailure("Vault locked before credentials could be saved.") }
                    return@launch
                }
                dao.upsert(
                    PasswordEntry(
                        id = existing?.id ?: 0,
                        label = parsed.domain,
                        username = username,
                        password = password,
                        website = parsed.domain,
                        folder = existing?.folder ?: "General",
                        createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                )
                complete(completed) { callback.onSuccess() }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                complete(completed) { callback.onFailure("Could not save credentials") }
            }
        }
    }

    private inline fun complete(once: AtomicBoolean, action: () -> Unit) {
        if (once.compareAndSet(false, true)) action()
    }

    private data class Parsed(
        val domain: String,
        val usernameId: AutofillId?,
        val passwordId: AutofillId?,
        val usernameValue: String?,
        val passwordValue: String?,
        val payment: Boolean
    )

    private fun parseStructure(structure: AssistStructure): Parsed {
        var userId: AutofillId? = null
        var passId: AutofillId? = null
        var userValue: String? = null
        var passValue: String? = null
        var domain = ""
        var payment = false
        var visited = 0

        fun visit(node: AssistStructure.ViewNode, depth: Int) {
            if (depth > MAX_STRUCTURE_DEPTH || visited++ >= MAX_STRUCTURE_NODES) return
            val hints = node.autofillHints?.toList().orEmpty()
            val hintText = (hints + listOf(node.hint.orEmpty(), node.idEntry.orEmpty()))
                .joinToString(" ").lowercase()
            if (PAYMENT_HINTS.any(hintText::contains)) payment = true
            node.webDomain?.takeIf { it.isNotBlank() }?.let { domain = normalizeDomain(it) }
            val value = node.autofillValue?.takeIf { it.isText }?.textValue?.toString()
            when {
                hints.any { it.contains("password", true) } || hintText.contains("password") -> {
                    passId = node.autofillId
                    passValue = value
                }
                hints.any { it.contains("username", true) || it.contains("emailAddress", true) } ||
                    hintText.contains("username") || hintText.contains("email") -> {
                    userId = node.autofillId
                    userValue = value
                }
            }
            for (i in 0 until node.childCount) visit(node.getChildAt(i), depth + 1)
        }

        for (i in 0 until structure.windowNodeCount) {
            if (visited >= MAX_STRUCTURE_NODES) break
            visit(structure.getWindowNodeAt(i).rootViewNode, 0)
        }
        return Parsed(domain.ifBlank { "unknown" }, userId, passId, userValue, passValue, payment)
    }

    private fun isVaultUnlocked(): Boolean =
        getSharedPreferences("vault_session", MODE_PRIVATE)
            .getLong("unlocked_until", 0L) > System.currentTimeMillis()

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

    companion object {
        private const val MAX_STRUCTURE_DEPTH = 40
        private const val MAX_STRUCTURE_NODES = 2_000
        private val PAYMENT_HINTS = listOf(
            "credit", "card", "cc-", "cvc", "cvv", "iban", "payment", "expiration", "cardnumber"
        )
    }
}
