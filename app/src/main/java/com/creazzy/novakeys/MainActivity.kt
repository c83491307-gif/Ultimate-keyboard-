package com.creazzy.novakeys

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.creazzy.novakeys.data.PasswordCsv
import com.creazzy.novakeys.data.PasswordDatabase
import com.creazzy.novakeys.data.PasswordEntry
import com.creazzy.novakeys.security.PasswordTools
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var unlocked by remember { mutableStateOf(false) }
            var authError by remember { mutableStateOf("") }
            val executor = remember { ContextCompat.getMainExecutor(this@MainActivity) }
            val prompt = remember {
                BiometricPrompt(this@MainActivity, executor, object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        getSharedPreferences("vault_session", MODE_PRIVATE).edit().putLong("unlocked_until", System.currentTimeMillis() + 300000L).apply()
                        unlocked = true
                        authError = ""
                    }
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) { authError = errString.toString() }
                    override fun onAuthenticationFailed() { authError = "Authentication failed. Try again." }
                })
            }
            MaterialTheme {
                if (unlocked && getSharedPreferences("vault_session", MODE_PRIVATE).getLong("unlocked_until", 0L) > System.currentTimeMillis()) {
                    VaultScreen()
                } else {
                    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
                        Text("NovaKeys Vault", style = MaterialTheme.typography.headlineMedium)
                        Text("Unlock with your fingerprint or device screen lock.")
                        if (authError.isNotBlank()) Text(authError, color = MaterialTheme.colorScheme.error)
                        Button(onClick = {
                            val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
                            if (BiometricManager.from(this@MainActivity).canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS) {
                                prompt.authenticate(BiometricPrompt.PromptInfo.Builder().setTitle("Unlock NovaKeys").setSubtitle("Authenticate to access saved credentials").setAllowedAuthenticators(authenticators).build())
                            } else authError = "Enable a fingerprint or screen lock in Android settings first."
                        }, modifier = Modifier.fillMaxWidth()) { Text("Unlock vault") }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun VaultScreen() {
    val context = LocalContext.current
    val dao = remember { PasswordDatabase.get(context).passwords() }
    val scope = rememberCoroutineScope()
    val entries by dao.observeAll().collectAsState(initial = emptyList())
    var query by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    var website by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var folder by remember { mutableStateOf("General") }
    var editingId by remember { mutableStateOf(0L) }
    var createdAt by remember { mutableStateOf(0L) }
    var showForm by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var showExportWarning by remember { mutableStateOf(false) }
    var exportBitwarden by remember { mutableStateOf(false) }
    var showAudit by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                val imported = PasswordCsv.import(context.contentResolver, uri)
                imported.forEach { dao.upsert(it) }
                status = "Imported ${imported.size} entries"
            }.onFailure { status = "Import failed: ${it.message ?: "invalid CSV"}" }
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) scope.launch {
            runCatching { PasswordCsv.export(context.contentResolver, uri, entries); status = "Chrome-compatible CSV exported" }
                .onFailure { status = "Export failed: ${it.message ?: "could not write file"}" }
        }
    }
    val bitwardenExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) scope.launch {
            runCatching { PasswordCsv.exportBitwarden(context.contentResolver, uri, entries); status = "Bitwarden-compatible CSV exported" }
                .onFailure { status = "Export failed: ${it.message ?: "could not write file"}" }
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("NovaKeys Password Vault") }) },
        floatingActionButton = { FloatingActionButton(onClick = {
            editingId = 0L; createdAt = 0L; label = ""; website = ""; username = ""; password = ""; folder = "General"; showForm = true
        }) { Text("+") } }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
            Text("محلي ومشفّر • Local encrypted vault", style = MaterialTheme.typography.bodyMedium)
            if (status.isNotBlank()) Text(status, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(query, { query = it }, label = { Text("بحث / Search") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { importLauncher.launch(arrayOf("text/*", "application/csv", "application/vnd.ms-excel")) }, modifier = Modifier.weight(1f)) { Text("Import CSV") }
                OutlinedButton(onClick = { exportBitwarden = false; showExportWarning = true }, modifier = Modifier.weight(1f)) { Text("Export CSV") }
            }
            Button(onClick = { runCatching { context.startActivity(Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE).setData(android.net.Uri.parse("package:${context.packageName}"))) } },
                modifier = Modifier.fillMaxWidth()) { Text("Enable Autofill in Android Settings") }
            Button(onClick = { runCatching { context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) } },
                modifier = Modifier.fillMaxWidth()) { Text("Enable NovaKeys keyboard") }
            Button(onClick = { showAudit = !showAudit }, modifier = Modifier.fillMaxWidth()) { Text(if (showAudit) "Hide password audit" else "Audit weak/reused passwords") }
            if (showAudit) {
                val findings = PasswordTools.weakOrRepeated(entries.map { it.password })
                Text("Audit results: ${findings.size} weak or reused password(s)", style = MaterialTheme.typography.titleSmall)
                findings.values.distinct().forEach { reason -> Text("• $reason", style = MaterialTheme.typography.bodySmall) }
            }
            Button(onClick = { password = PasswordTools.generate(); editingId = 0L; showForm = true },
                modifier = Modifier.fillMaxWidth()) { Text("Generate strong password") }
            val shown = entries.filter { query.isBlank() || it.label.contains(query, true) || it.website.contains(query, true) || it.username.contains(query, true) || it.folder.contains(query, true) }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(shown, key = { it.id }) { entry ->
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(entry.label, style = MaterialTheme.typography.titleMedium)
                            Text(entry.website)
                            Text(entry.username)
                            Text("Folder: ${entry.folder}", style = MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = {
                                    editingId = entry.id; createdAt = entry.createdAt
                                    label = entry.label; website = entry.website; username = entry.username
                                    password = entry.password; folder = entry.folder; showForm = true
                                }) { Text("Edit") }
                                TextButton(onClick = { scope.launch { dao.delete(entry); status = "Deleted" } }) { Text("Delete") }
                            }
                        }
                    }
                }
            }
        }
    }
    if (showForm) AlertDialog(
        onDismissRequest = { showForm = false },
        title = { Text(if (editingId == 0L) "Add credential" else "Edit credential") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Name") })
                OutlinedTextField(value = website, onValueChange = { website = it }, label = { Text("Website / domain") })
                OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username / email") })
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") })
                OutlinedTextField(value = folder, onValueChange = { folder = it }, label = { Text("Folder") })
                TextButton(onClick = { password = PasswordTools.generate() }) { Text("Generate strong password") }
            }
        },
        confirmButton = { TextButton(onClick = {
            scope.launch {
                dao.upsert(PasswordEntry(id = editingId, label = label.ifBlank { website }, website = website.trim(),
                    username = username, password = password, folder = folder.ifBlank { "General" },
                    createdAt = if (createdAt == 0L) System.currentTimeMillis() else createdAt))
                status = "Saved locally"; label = ""; website = ""; username = ""; password = ""
                folder = "General"; editingId = 0L; createdAt = 0L; showForm = false
            }
        }) { Text("Save") } },
        dismissButton = { TextButton(onClick = { showForm = false }) { Text("Cancel") } }
    )
    if (showExportWarning) AlertDialog(
        onDismissRequest = { showExportWarning = false },
        title = { Text("Export plaintext passwords?") },
        text = { Text("CSV exports contain readable passwords. Save only to a trusted location, do not share the file, and delete it when finished.") },
        confirmButton = { TextButton(onClick = {
            showExportWarning = false
            if (exportBitwarden) bitwardenExportLauncher.launch("novakeys-bitwarden.csv")
            else exportLauncher.launch("novakeys-passwords.csv")
        }) { Text("Continue export") } },
        dismissButton = { TextButton(onClick = { showExportWarning = false }) { Text("Cancel") } }
    )
}
