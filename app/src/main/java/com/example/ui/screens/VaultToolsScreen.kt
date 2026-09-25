package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.EncryptGold
import com.example.ui.theme.TelegramBlue
import com.example.ui.theme.TelegramCyan
import com.example.ui.theme.TelegramLightBlue
import com.example.ui.theme.VaultCardBorder
import com.example.ui.theme.VaultErrorRed
import com.example.ui.theme.VaultSuccessGreen
import com.example.ui.viewmodel.TeleVaultViewModel

@Composable
fun VaultToolsScreen(
    viewModel: TeleVaultViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDecrypting by viewModel.isDecrypting.collectAsStateWithLifecycle()
    val decryptResult by viewModel.decryptResult.collectAsStateWithLifecycle()

    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    var decryptPassphrase by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedFileUri = uri
            val doc = DocumentFile.fromSingleUri(context, uri)
            selectedFileName = doc?.name ?: "encrypted_file.enc"
            viewModel.clearDecryptResult()
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Vault & Security Tools",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Decrypt backups, verify SHA-256 integrity, and test zero-loss recovery",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Section: File Decryption & Restorer Tool
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, EncryptGold.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(EncryptGold.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockOpen,
                                contentDescription = null,
                                tint = EncryptGold,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Decrypt & Restore File",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Restore any .enc file downloaded from your Telegram channels",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Step 1: Pick Encrypted File
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, VaultCardBorder, RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { filePickerLauncher.launch("*/*") }
                            .padding(14.dp)
                            .testTag("pick_encrypted_file_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    tint = EncryptGold
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (selectedFileUri != null) selectedFileName else "Select Encrypted File (.enc)...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (selectedFileUri != null) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedFileUri != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Step 2: Enter Passphrase
                    OutlinedTextField(
                        value = decryptPassphrase,
                        onValueChange = { decryptPassphrase = it },
                        label = { Text("Decryption Passphrase") },
                        placeholder = { Text("Enter passphrase (or blank if default key)") },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle password visibility"
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("decrypt_passphrase_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    // Decrypt Action Button
                    Button(
                        onClick = {
                            val uri = selectedFileUri
                            if (uri != null) {
                                val outName = selectedFileName.removeSuffix(".enc")
                                val pass = decryptPassphrase.ifBlank { "TeleVaultDefaultKey2026" }
                                viewModel.decryptAndRestoreFile(uri, outName, pass)
                            }
                        },
                        enabled = selectedFileUri != null && !isDecrypting,
                        colors = ButtonDefaults.buttonColors(containerColor = EncryptGold, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("decrypt_file_button")
                    ) {
                        if (isDecrypting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Decrypting & Verifying SHA-256...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Decrypt & Verify Integrity", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Result Banner
                    if (decryptResult != null) {
                        val res = decryptResult!!
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (res.isSuccess) VaultSuccessGreen.copy(alpha = 0.15f)
                                    else VaultErrorRed.copy(alpha = 0.15f)
                                )
                                .padding(14.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (res.isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                        contentDescription = null,
                                        tint = if (res.isSuccess) VaultSuccessGreen else VaultErrorRed,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (res.isSuccess) "Decryption Successful!" else "Decryption Failed",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (res.isSuccess) VaultSuccessGreen else VaultErrorRed
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = res.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                if (res.sha256 != null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Restored SHA-256:\n${res.sha256}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = VaultSuccessGreen
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: System Architecture Cards
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Card 1: Zero Loss Large File Chunking
                InfoFeatureCard(
                    title = "Support for Large File Uploads (No Limits)",
                    description = "Telegram Bot API restricts single document uploads to 50MB. TeleVault automatically slices large files into 45MB encrypted chunks + manifests. Files of any size (up to tens of gigabytes) are uploaded seamlessly without data loss.",
                    icon = Icons.Default.CloudUpload,
                    accentColor = TelegramLightBlue
                )

                // Card 2: End-to-End Cryptography
                InfoFeatureCard(
                    title = "Military-Grade AES-256-GCM Storage",
                    description = "Files are encrypted client-side using authenticated AES-GCM with PBKDF2 (10,000 iterations). Each file receives a unique 96-bit IV and 128-bit authentication tag. Not even Telegram's servers can read your backed up files.",
                    icon = Icons.Default.Shield,
                    accentColor = EncryptGold
                )

                // Card 3: Automatic Background Scheduler
                InfoFeatureCard(
                    title = "Automated WorkManager Scheduling",
                    description = "Background backups run on your defined schedule (e.g. daily, hourly) with power and network awareness. Backups can automatically wait for unmetered Wi-Fi and charging conditions.",
                    icon = Icons.Default.Schedule,
                    accentColor = TelegramCyan
                )
            }
        }
    }
}

@Composable
fun InfoFeatureCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, VaultCardBorder.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
