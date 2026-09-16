package dev.aten.rssh.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.aten.rssh.appContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val keyManager = context.appContainer.keyManager
    val scope = rememberCoroutineScope()
    var publicKey by remember { mutableStateOf(keyManager.openSshPublicKey()) }
    var fingerprint by remember { mutableStateOf(keyManager.fingerprint()) }
    var confirmRegenerate by remember { mutableStateOf(false) }

    fun generate() {
        scope.launch {
            withContext(Dispatchers.Default) { keyManager.generate() }
            publicKey = keyManager.openSshPublicKey()
            fingerprint = keyManager.fingerprint()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SSH key") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val key = publicKey
            if (key == null) {
                Text("No key yet. Generate one, then add its public half to ~/.ssh/authorized_keys on every host.")
                Button(onClick = ::generate) { Text("Generate Ed25519 key") }
            } else {
                Text("Fingerprint", style = MaterialTheme.typography.labelMedium)
                Text(fingerprint.orEmpty(), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                Text("Public key — add this line to ~/.ssh/authorized_keys", style = MaterialTheme.typography.labelMedium)
                SelectionContainer {
                    Text(key, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = {
                        context.getSystemService(ClipboardManager::class.java)
                            .setPrimaryClip(ClipData.newPlainText("rssh public key", key))
                        Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                    }) { Text("Copy") }
                    OutlinedButton(onClick = {
                        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, key)
                        context.startActivity(Intent.createChooser(send, "Share public key"))
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = null)
                        Text(" Share")
                    }
                }
                TextButton(onClick = { confirmRegenerate = true }) { Text("Regenerate key") }
            }
            Text(
                "The private key never leaves this device: it is stored encrypted with a key kept in the Android Keystore.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }

    if (confirmRegenerate) {
        AlertDialog(
            onDismissRequest = { confirmRegenerate = false },
            title = { Text("Replace the key?") },
            text = { Text("Hosts will reject this device until you install the new public key.") },
            confirmButton = { TextButton(onClick = { confirmRegenerate = false; generate() }) { Text("Replace") } },
            dismissButton = { TextButton(onClick = { confirmRegenerate = false }) { Text("Cancel") } },
        )
    }
}
