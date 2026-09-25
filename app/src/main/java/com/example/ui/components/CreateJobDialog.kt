package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import com.example.data.model.BackupJob
import com.example.data.model.TelegramChannel
import com.example.ui.theme.EncryptGold
import com.example.ui.theme.TelegramBlue
import com.example.ui.theme.TelegramLightBlue
import com.example.ui.theme.VaultCardBorder

@Composable
fun CreateJobDialog(
    channels: List<TelegramChannel>,
    onDismiss: () -> Unit,
    onSave: (BackupJob) -> Unit
) {
    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    var selectedFolderUri by remember { mutableStateOf<Uri?>(null) }
    var folderDisplayName by remember { mutableStateOf("") }
    var selectedChannelId by remember { mutableLongStateOf(channels.firstOrNull()?.id ?: 0L) }

    var isEncrypted by remember { mutableStateOf(true) }
    var passphrase by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    var isScheduleEnabled by remember { mutableStateOf(true) }
    var scheduleIntervalMinutes by remember { mutableLongStateOf(1440L) } // 24h
    var requireWifi by remember { mutableStateOf(true) }
    var requireCharging by remember { mutableStateOf(false) }

    var chunkSizeMb by remember { mutableFloatStateOf(45f) }

    var channelMenuExpanded by remember { mutableStateOf(false) }
    var scheduleMenuExpanded by remember { mutableStateOf(false) }

    // SAF Folder Picker launcher
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (e: Exception) {
                // Ignore if not supported
            }
            selectedFolderUri = uri
            val doc = DocumentFile.fromTreeUri(context, uri)
            folderDisplayName = doc?.name ?: uri.lastPathSegment ?: "Selected Folder"
            if (title.isBlank()) {
                title = "$folderDisplayName Backup"
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "New Folder Backup Job",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Step 1: Pick Folder
                Text(
                    text = "1. Select Local Folder",
                    style = MaterialTheme.typography.labelLarge,
                    color = TelegramLightBlue,
                    fontWeight = FontWeight.SemiBold
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, VaultCardBorder, RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable { folderPickerLauncher.launch(null) }
                        .padding(14.dp)
                        .testTag("pick_folder_button")
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
                                tint = TelegramLightBlue
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (selectedFolderUri != null) folderDisplayName else "Tap to choose folder...",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (selectedFolderUri != null) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedFolderUri != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Step 2: Job Name
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Job Title") },
                    placeholder = { Text("e.g. DCIM Camera / Work Documents") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("job_title_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Step 3: Target Channel
                Text(
                    text = "2. Target Telegram Channel",
                    style = MaterialTheme.typography.labelLarge,
                    color = TelegramLightBlue,
                    fontWeight = FontWeight.SemiBold
                )

                if (channels.isEmpty()) {
                    Text(
                        text = "⚠️ No Telegram channels configured yet! Add one in the Channels tab first.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        val currentChannel = channels.find { it.id == selectedChannelId } ?: channels.first()
                        OutlinedTextField(
                            value = "${currentChannel.name} (${currentChannel.chatId})",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Destination Channel") },
                            trailingIcon = {
                                IconButton(onClick = { channelMenuExpanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { channelMenuExpanded = true },
                            shape = RoundedCornerShape(12.dp)
                        )

                        DropdownMenu(
                            expanded = channelMenuExpanded,
                            onDismissRequest = { channelMenuExpanded = false }
                        ) {
                            channels.forEach { ch ->
                                DropdownMenuItem(
                                    text = { Text("${ch.name} (${ch.chatId})") },
                                    leadingIcon = { Icon(Icons.Default.Send, contentDescription = null, tint = TelegramLightBlue) },
                                    onClick = {
                                        selectedChannelId = ch.id
                                        channelMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Step 4: Encrypted Storage
                Text(
                    text = "3. End-to-End Encryption",
                    style = MaterialTheme.typography.labelLarge,
                    color = EncryptGold,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AES-256-GCM Encryption",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Files are encrypted before leaving your device with zero-loss integrity verification",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isEncrypted,
                        onCheckedChange = { isEncrypted = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = EncryptGold,
                            checkedTrackColor = EncryptGold.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.testTag("encryption_toggle")
                    )
                }

                if (isEncrypted) {
                    OutlinedTextField(
                        value = passphrase,
                        onValueChange = { passphrase = it },
                        label = { Text("Encryption Passphrase") },
                        placeholder = { Text("Custom passphrase (or leave empty for master key)") },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle password visibility"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                // Step 5: Large File Uploads (Chunking)
                Text(
                    text = "4. Large File Chunking",
                    style = MaterialTheme.typography.labelLarge,
                    color = TelegramLightBlue,
                    fontWeight = FontWeight.SemiBold
                )

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Split chunks at:",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "${chunkSizeMb.toInt()} MB (Telegram Bot API limit: 50MB)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = TelegramLightBlue
                        )
                    }
                    Slider(
                        value = chunkSizeMb,
                        onValueChange = { chunkSizeMb = it },
                        valueRange = 10f..48f,
                        steps = 7,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Files larger than ${chunkSizeMb.toInt()}MB are split into numbered parts + manifest for 100% loss-free backup.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Step 6: Automatic Scheduler
                Text(
                    text = "5. Automatic Scheduler",
                    style = MaterialTheme.typography.labelLarge,
                    color = TelegramLightBlue,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Automated Periodic Sync",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Background backup runs automatically without user interaction",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isScheduleEnabled,
                        onCheckedChange = { isScheduleEnabled = it },
                        modifier = Modifier.testTag("schedule_toggle")
                    )
                }

                if (isScheduleEnabled) {
                    val intervals = listOf(
                        60L to "Every 1 Hour",
                        180L to "Every 3 Hours",
                        360L to "Every 6 Hours",
                        720L to "Every 12 Hours",
                        1440L to "Every 24 Hours (Daily)",
                        10080L to "Every 7 Days (Weekly)"
                    )
                    val selectedLabel = intervals.find { it.first == scheduleIntervalMinutes }?.second ?: "Every 24 Hours"

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Backup Frequency") },
                            trailingIcon = {
                                IconButton(onClick = { scheduleMenuExpanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { scheduleMenuExpanded = true },
                            shape = RoundedCornerShape(12.dp)
                        )

                        DropdownMenu(
                            expanded = scheduleMenuExpanded,
                            onDismissRequest = { scheduleMenuExpanded = false }
                        ) {
                            intervals.forEach { (mins, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        scheduleIntervalMinutes = mins
                                        scheduleMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "WiFi Only (Save cellular data)", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = requireWifi,
                            onCheckedChange = { requireWifi = it }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Only When Charging", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = requireCharging,
                            onCheckedChange = { requireCharging = it }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedFolderUri != null && selectedChannelId != 0L) {
                        onSave(
                            BackupJob(
                                title = title.ifBlank { folderDisplayName },
                                folderUri = selectedFolderUri.toString(),
                                folderDisplayName = folderDisplayName,
                                channelId = selectedChannelId,
                                isEncrypted = isEncrypted,
                                encryptionPassphrase = passphrase,
                                isScheduleEnabled = isScheduleEnabled,
                                scheduleIntervalMinutes = scheduleIntervalMinutes,
                                requireWifi = requireWifi,
                                requireCharging = requireCharging,
                                chunkSizeMb = chunkSizeMb.toInt()
                            )
                        )
                    }
                },
                enabled = selectedFolderUri != null && selectedChannelId != 0L,
                colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("save_job_confirm_button")
            ) {
                Text("Create Backup Job")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
