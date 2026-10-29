package com.creazzy.novakeys

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
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
import com.creazzy.novakeys.data.PasswordBackup
import com.creazzy.novakeys.data.PasswordCsv
import com.creazzy.novakeys.data.PasswordDatabase
import com.creazzy.novakeys.data.PasswordEntry
import com.creazzy.novakeys.security.PasswordTools
import kotlinx.coroutines.launch
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile

class MainActivity : FragmentActivity() {
    private var resumeTick by mutableIntStateOf(0)

    override fun onResume() {
        super.onResume()
        resumeTick++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val currentResumeTick = resumeTick
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
                    VaultScreen(refreshKey = currentResumeTick)
                } else {
                    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
                        Text("NovaKeys Vault", style = MaterialTheme.typography.headlineMedium)
                        Text("Unlock with your fingerprint or device screen lock.")
                        KeyboardSetupCard(refreshKey = currentResumeTick)
                        Spacer(Modifier.height(12.dp))
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

@Composable
private fun KeyboardSetupCard(refreshKey: Int) {
    val context = LocalContext.current
    val componentId = "${context.packageName}/${com.creazzy.novakeys.ime.NovaInputMethodService::class.java.name}"
    // Reading these values again on Activity resume reflects changes made in Android Settings.
    @Suppress("UNUSED_VARIABLE") val resumeRefresh = refreshKey
    val enabledMethods = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_INPUT_METHODS)
    val defaultMethod = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
    val enabled = com.creazzy.novakeys.ime.KeyboardSetupState.isEnabled(enabledMethods, componentId)
    val isDefault = com.creazzy.novakeys.ime.KeyboardSetupState.isDefault(defaultMethod, componentId)
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Keyboard setup", style = MaterialTheme.typography.titleMedium)
            Text(
                when {
                    isDefault -> "NovaKeys is your current default keyboard."
                    enabled -> "NovaKeys is enabled. Select it as the default keyboard to start using it."
                    else -> "Step 1: enable NovaKeys. Step 2: choose it as the default keyboard."
                },
                color = if (isDefault) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(onClick = {
                runCatching { context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
                    .onFailure { android.widget.Toast.makeText(context, "Could not open keyboard settings", android.widget.Toast.LENGTH_LONG).show() }
            }, modifier = Modifier.fillMaxWidth()) {
                Text(if (enabled) "Manage enabled keyboards" else "1 · Enable NovaKeys")
            }
            Button(onClick = {
                runCatching {
                    val manager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    manager.showInputMethodPicker()
                }.onFailure {
                    android.widget.Toast.makeText(context, "Open a text field to choose a keyboard", android.widget.Toast.LENGTH_LONG).show()
                }
            }, modifier = Modifier.fillMaxWidth()) { Text(if (isDefault) "Switch keyboard" else "2 · Choose default keyboard") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun VaultScreen(refreshKey: Int) {
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
    var backupPassphrase by remember { mutableStateOf("") }
    var showBackupPassphrase by remember { mutableStateOf(false) }
    var backupAction by remember { mutableStateOf("export") }

    var storageTreeUri by remember { mutableStateOf(context.getSharedPreferences("local_storage", 0).getString("tree_uri", null)) }
    var storedFiles by remember { mutableStateOf<List<DocumentFile>>(emptyList()) }
    var pendingExportUri by remember { mutableStateOf<Uri?>(null) }
    fun appFolder(): DocumentFile? {
        val raw = storageTreeUri ?: return null
        val root = DocumentFile.fromTreeUri(context, Uri.parse(raw)) ?: return null
        return root.findFile("NovaKeys") ?: root.createDirectory("NovaKeys")
    }
    fun refreshStoredFiles() { storedFiles = appFolder()?.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList() }
    LaunchedEffect(storageTreeUri) { refreshStoredFiles() }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            context.getSharedPreferences("local_storage", 0).edit().putString("tree_uri", uri.toString()).apply()
            storageTreeUri = uri.toString()
            val folder = appFolder() ?: error("Could not create NovaKeys folder")
            refreshStoredFiles()
            status = "NovaKeys folder ready: " + (folder.name ?: "NovaKeys")
        }.onFailure { status = "Folder setup failed: " + (it.message ?: "unknown error") }
    }
    val mediaImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) scope.launch { runCatching {
            val folder = appFolder() ?: error("Choose a phone folder first.")
            var count = 0
            uris.forEach { uri ->
                val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null } ?: "imported-file"
                val safeName = name.substringAfterLast('/').replace(Regex("[^A-Za-z0-9._ -]"), "_").take(100).ifBlank { "imported-file" }
                val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
                val dest = folder.createFile(mime, System.currentTimeMillis().toString() + "_" + safeName) ?: error("Cannot create file for " + safeName)
                context.contentResolver.openInputStream(uri).use { input ->
                    requireNotNull(input) { "Cannot read " + safeName }
                    context.contentResolver.openOutputStream(dest.uri, "w").use { output -> requireNotNull(output) { "Cannot write " + safeName }; input.copyTo(output) }
                }
                count++
            }
            refreshStoredFiles()
            status = "Imported " + count + " file(s) into NovaKeys folder"
        }.onFailure { status = "File import failed: " + (it.message ?: "unknown error") } }
    }
    val storedFileExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { target ->
        val source = pendingExportUri
        if (target != null && source != null) scope.launch { runCatching {
            context.contentResolver.openInputStream(source).use { input -> requireNotNull(input) { "Cannot read source file" }; context.contentResolver.openOutputStream(target, "w").use { output -> requireNotNull(output) { "Cannot write destination" }; input.copyTo(output) } }
            status = "File exported successfully"
        }.onFailure { status = "File export failed: " + (it.message ?: "unknown error") }; pendingExportUri = null }
    }
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
    val backupExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                val encrypted = PasswordBackup.encode(entries, backupPassphrase.toCharArray())
                context.contentResolver.openOutputStream(uri).use { stream ->
                    requireNotNull(stream) { "Cannot open destination" }
                    stream.write(encrypted.toByteArray(Charsets.UTF_8))
                }
                status = "Encrypted backup exported"
            }.onFailure { status = "Backup export failed: ${it.message ?: "unknown error"}" }
            backupPassphrase = ""
        }
    }
    val backupImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                val raw = context.contentResolver.openInputStream(uri).use { stream ->
                    requireNotNull(stream) { "Cannot open backup file" }
                    stream.readBytes().toString(Charsets.UTF_8)
                }
                val restored = PasswordBackup.decode(raw, backupPassphrase.toCharArray())
                restored.forEach { dao.upsert(it) }
                status = "Restored ${restored.size} encrypted-backup entries"
            }.onFailure { status = "Backup import failed: wrong passphrase or invalid file (${it.message ?: "unknown error"})" }
            backupPassphrase = ""
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { backupAction = "export"; showBackupPassphrase = true }, modifier = Modifier.weight(1f)) { Text("Export encrypted backup") }
                OutlinedButton(onClick = { backupAction = "import"; showBackupPassphrase = true }, modifier = Modifier.weight(1f)) { Text("Import phone backup") }
            }
            Button(onClick = { runCatching { context.startActivity(Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE).setData(android.net.Uri.parse("package:${context.packageName}"))) } },
                modifier = Modifier.fillMaxWidth()) { Text("Enable Autofill in Android Settings") }
            KeyboardSetupCard(refreshKey = refreshKey)
            Button(onClick = {
                context.startActivity(Intent(context, com.creazzy.novakeys.clipboard.ScreenshotClipboardActivity::class.java))
            }, modifier = Modifier.fillMaxWidth()) { Text("Screenshot & Image Clipboard") }
            Button(onClick = {
                context.startActivity(Intent(context, com.creazzy.novakeys.clipboard.TextToImageActivity::class.java))
            }, modifier = Modifier.fillMaxWidth()) { Text("Convert copied text to image") }
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Phone storage · Local files", style = MaterialTheme.typography.titleMedium)
                    Text(if (storageTreeUri == null) "Choose a location in phone storage. NovaKeys will create its own folder there." else "Folder: NovaKeys · " + storedFiles.size + " file(s)")
                    Button(onClick = { folderPicker.launch(null) }, modifier = Modifier.fillMaxWidth()) { Text(if (storageTreeUri == null) "Choose / create NovaKeys folder" else "Change phone folder") }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { mediaImportLauncher.launch(arrayOf("*/*")) }, modifier = Modifier.weight(1f), enabled = storageTreeUri != null) { Text("Import files / media") }
                        OutlinedButton(onClick = { backupAction = "folderExport"; showBackupPassphrase = true }, modifier = Modifier.weight(1f), enabled = storageTreeUri != null) { Text("Save encrypted backup") }
                    }
                    OutlinedButton(onClick = { refreshStoredFiles() }, modifier = Modifier.fillMaxWidth(), enabled = storageTreeUri != null) { Text("Refresh folder files") }
                    storedFiles.take(30).forEach { file ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) { Text(file.name ?: "Unnamed file", style = MaterialTheme.typography.bodyMedium); Text(file.length().toString() + " bytes", style = MaterialTheme.typography.bodySmall) }
                            TextButton(onClick = { pendingExportUri = file.uri; storedFileExportLauncher.launch(file.name ?: "novakeys-file") }) { Text("Export") }
                        }
                    }
                    if (storedFiles.size > 30) Text("Showing first 30 files.")
                }
            }
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
    if (showBackupPassphrase) AlertDialog(
        onDismissRequest = { showBackupPassphrase = false; backupPassphrase = "" },
        title = { Text(when (backupAction) {
            "export" -> "Encrypt backup"
            "import" -> "Decrypt phone backup"
            else -> "Decrypt phone backup"
        }) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Use a separate backup passphrase with at least 10 characters. NovaKeys never saves or uploads this passphrase.")
                OutlinedTextField(value = backupPassphrase, onValueChange = { backupPassphrase = it },
                    label = { Text("Backup passphrase") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (backupPassphrase.length < 10) {
                    status = "Backup passphrase must contain at least 10 characters."
                } else {
                    showBackupPassphrase = false
                    when (backupAction) {
                        "export" -> backupExportLauncher.launch("novakeys-encrypted-backup.json")
                        "import" -> backupImportLauncher.launch(arrayOf("application/json", "text/*"))
"folderExport" -> scope.launch {
    runCatching {
        val folder = appFolder() ?: error("Choose a phone folder first.")
        val encrypted = PasswordBackup.encode(entries, backupPassphrase.toCharArray())
        val dest = folder.createFile("application/json", "novakeys-backup-" + System.currentTimeMillis() + ".json") ?: error("Cannot create backup file")
        context.contentResolver.openOutputStream(dest.uri, "w").use { output -> requireNotNull(output) { "Cannot write backup" }; output.write(encrypted.toByteArray(Charsets.UTF_8)) }
        refreshStoredFiles()
        status = "Encrypted backup written into NovaKeys folder"
    }.onFailure { status = "Folder backup failed: " + (it.message ?: "unknown error") }
    backupPassphrase = ""
}

                    }
                }
            }) { Text("Continue") }
        },
        dismissButton = { TextButton(onClick = { showBackupPassphrase = false; backupPassphrase = "" }) { Text("Cancel") } }
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
