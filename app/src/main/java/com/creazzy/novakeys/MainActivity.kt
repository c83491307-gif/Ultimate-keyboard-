package com.creazzy.novakeys

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.creazzy.novakeys.data.PasswordDatabase
import com.creazzy.novakeys.data.PasswordEntry
import com.creazzy.novakeys.security.PasswordTools
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { VaultScreen() } }
    }
}

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
    var showForm by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    Scaffold(topBar = { TopAppBar(title = { Text("NovaKeys Password Vault") }) },
        floatingActionButton = { FloatingActionButton(onClick = { showForm = true }) { Text("+") } }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
            Text("محلي ومشفّر • Local encrypted vault", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(query, { query = it }, label = { Text("بحث / Search") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Button(onClick = { runCatching { context.startActivity(android.content.Intent(android.provider.Settings.ACTION_AUTOFILL_SERVICE_SETTINGS)) } },
                modifier = Modifier.fillMaxWidth()) { Text("Enable Autofill in Android Settings") }
            Button(onClick = { runCatching { context.startActivity(android.content.Intent(android.provider.Settings.ACTION_INPUT_METHOD_SETTINGS)) } },
                modifier = Modifier.fillMaxWidth()) { Text("Enable NovaKeys keyboard") }
            Button(onClick = { password = PasswordTools.generate(); showForm = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Generate strong password")
            }
            val shown = entries.filter { query.isBlank() || it.label.contains(query, true) || it.website.contains(query, true) || it.username.contains(query, true) }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(shown, key = { it.id }) { entry ->
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(entry.label, style = MaterialTheme.typography.titleMedium)
                            Text(entry.website)
                            Text(entry.username)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = {
                                    label = entry.label; website = entry.website; username = entry.username
                                    password = entry.password; folder = entry.folder; showForm = true
                                }) { Text("Edit") }
                                TextButton(onClick = { scope.launch { dao.delete(entry) } }) { Text("Delete") }
                            }
                        }
                    }
                }
            }
        }
    }
    if (showForm) AlertDialog(
        onDismissRequest = { showForm = false },
        title = { Text("Credential") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(label, { label = it }, label = { Text("Name") })
                OutlinedTextField(website, { website = it }, label = { Text("Website / domain") })
                OutlinedTextField(username, { username = it }, label = { Text("Username / email") })
                OutlinedTextField(password, { password = it }, label = { Text("Password") })
                OutlinedTextField(folder, { folder = it }, label = { Text("Folder") })
                TextButton(onClick = { password = PasswordTools.generate(); showPassword = true }) { Text("Generate password") }
                if (showPassword) Text("Generated. Save it to keep it.")
            }
        },
        confirmButton = { TextButton(onClick = {
            scope.launch {
                dao.upsert(PasswordEntry(label = label.ifBlank { website }, website = website.trim(),
                    username = username, password = password, folder = folder.ifBlank { "General" }))
                label = ""; website = ""; username = ""; password = ""; showPassword = false; showForm = false
            }
        }) { Text("Save") } },
        dismissButton = { TextButton(onClick = { showForm = false }) { Text("Cancel") } }
    )
}
