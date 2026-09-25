package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BackupJob
import com.example.data.model.TelegramChannel
import com.example.ui.components.ActiveBackupBanner
import com.example.ui.components.CreateJobDialog
import com.example.ui.components.JobCard
import com.example.ui.components.StatCard
import com.example.ui.theme.EncryptGold
import com.example.ui.theme.TelegramBlue
import com.example.ui.theme.TelegramCyan
import com.example.ui.theme.TelegramLightBlue
import com.example.ui.theme.VaultSuccessGreen
import com.example.ui.viewmodel.TeleVaultViewModel

@Composable
fun DashboardScreen(
    viewModel: TeleVaultViewModel,
    onNavigateToChannels: () -> Unit,
    modifier: Modifier = Modifier
) {
    val jobs by viewModel.allJobs.collectAsStateWithLifecycle()
    val channels by viewModel.allChannels.collectAsStateWithLifecycle()
    val progressState by viewModel.currentProgress.collectAsStateWithLifecycle()
    val totalFiles by viewModel.totalFilesCount.collectAsStateWithLifecycle()
    val totalBytes by viewModel.totalBytesCount.collectAsStateWithLifecycle()

    var showCreateJobDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "TeleVault",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Encrypted Local-to-Telegram Backup",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TelegramLightBlue
                            )
                        }

                        if (jobs.isNotEmpty()) {
                            Button(
                                onClick = { viewModel.runAllJobsNow() },
                                enabled = !progressState.isRunning,
                                colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("backup_all_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Backup All")
                            }
                        }
                    }
                }
            }

            // Ongoing Backup Banner
            item {
                ActiveBackupBanner(progressState = progressState)
            }

            // Stat Cards Grid
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "Active Folders",
                            value = "${jobs.size} Folders",
                            icon = Icons.Default.Folder,
                            accentColor = TelegramLightBlue,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Files Backed Up",
                            value = "$totalFiles Files",
                            icon = Icons.Default.CloudDone,
                            accentColor = VaultSuccessGreen,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "Total Synced",
                            value = formatBytes(totalBytes),
                            icon = Icons.Default.Storage,
                            accentColor = TelegramCyan,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Encryption",
                            value = "AES-256-GCM",
                            subtitle = "Zero-Loss SHA-256",
                            icon = Icons.Default.Security,
                            accentColor = EncryptGold,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Section Header: Backup Jobs
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Backup Jobs (${jobs.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (channels.isEmpty()) {
                        OutlinedButton(
                            onClick = onNavigateToChannels,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Configure Channel First")
                        }
                    }
                }
            }

            // Empty State
            if (jobs.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        tonalElevation = 1.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = TelegramLightBlue,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "No Backup Jobs Configured",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (channels.isEmpty()) {
                                    "First add your Telegram Bot and Target Channel in the Channels tab, then link your folders here!"
                                } else {
                                    "Tap '+' below to select a local folder, set up scheduled sync, and start backing up to Telegram!"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            if (channels.isEmpty()) {
                                Button(
                                    onClick = onNavigateToChannels,
                                    colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Add Telegram Channel")
                                }
                            } else {
                                Button(
                                    onClick = { showCreateJobDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("empty_state_add_job_button")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Create First Backup Job")
                                }
                            }
                        }
                    }
                }
            } else {
                items(jobs, key = { it.id }) { job ->
                    val targetChannel = channels.find { it.id == job.channelId }
                    JobCard(
                        job = job,
                        channel = targetChannel,
                        isRunning = progressState.isRunning,
                        onRunNow = { viewModel.runBackupJobNow(job.id) },
                        onDelete = { viewModel.deleteJob(job) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // FAB to add job
        FloatingActionButton(
            onClick = {
                if (channels.isEmpty()) {
                    onNavigateToChannels()
                } else {
                    showCreateJobDialog = true
                }
            },
            containerColor = TelegramBlue,
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_job_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Backup Job")
        }
    }

    if (showCreateJobDialog) {
        CreateJobDialog(
            channels = channels,
            onDismiss = { showCreateJobDialog = false },
            onSave = { newJob ->
                viewModel.saveJob(newJob)
                showCreateJobDialog = false
            }
        )
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
    return "%.1f %s".format(bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}
