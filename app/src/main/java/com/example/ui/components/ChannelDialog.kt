package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.model.TelegramChannel
import com.example.ui.theme.TelegramBlue
import com.example.ui.theme.TelegramLightBlue
import com.example.ui.theme.VaultErrorRed
import com.example.ui.theme.VaultSuccessGreen

@Composable
fun ChannelDialog(
    initialChannel: TelegramChannel? = null,
    isTesting: Boolean,
    onTestConnection: (token: String, chatId: String, onResult: (Boolean, String) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onSave: (TelegramChannel) -> Unit
) {
    var name by remember { mutableStateOf(initialChannel?.name ?: "") }
    var botToken by remember { mutableStateOf(initialChannel?.botToken ?: "") }
    var chatId by remember { mutableStateOf(initialChannel?.chatId ?: "") }
    var topicIdStr by remember { mutableStateOf(initialChannel?.topicId?.toString() ?: "") }
    var showToken by remember { mutableStateOf(false) }

    var testStatusMessage by remember { mutableStateOf<String?>(null) }
    var isTestSuccess by remember { mutableStateOf<Boolean?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialChannel == null) "Add Telegram Channel" else "Edit Channel",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Info Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(TelegramBlue.copy(alpha = 0.1f))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = TelegramLightBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "1. Create a bot with @BotFather\n2. Add the bot as Administrator to your Telegram channel\n3. Enter the bot token and channel ID below",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Channel Label / Name") },
                    placeholder = { Text("e.g. My Secure Cloud Channel") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("channel_name_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = botToken,
                    onValueChange = {
                        botToken = it
                        testStatusMessage = null
                        isTestSuccess = null
                    },
                    label = { Text("Telegram Bot Token") },
                    placeholder = { Text("e.g. 123456789:AAHk...") },
                    visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showToken = !showToken }) {
                            Icon(
                                imageVector = if (showToken) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle Token Visibility"
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bot_token_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = chatId,
                    onValueChange = {
                        chatId = it
                        testStatusMessage = null
                        isTestSuccess = null
                    },
                    label = { Text("Channel / Chat ID") },
                    placeholder = { Text("e.g. -100192837465 or @channel_name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("chat_id_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = topicIdStr,
                    onValueChange = { topicIdStr = it },
                    label = { Text("Topic ID (Optional)") },
                    placeholder = { Text("For supergroup forum threads") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Test Connection Button
                OutlinedButton(
                    onClick = {
                        if (botToken.isNotBlank() && chatId.isNotBlank()) {
                            onTestConnection(botToken.trim(), chatId.trim()) { success, msg ->
                                isTestSuccess = success
                                testStatusMessage = msg
                            }
                        }
                    },
                    enabled = botToken.isNotBlank() && chatId.isNotBlank() && !isTesting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("test_channel_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = TelegramLightBlue
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verifying Credentials...")
                    } else {
                        Text("Test Bot & Channel Connection")
                    }
                }

                // Connection Feedback
                if (testStatusMessage != null) {
                    val isSuccess = isTestSuccess == true
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSuccess) VaultSuccessGreen.copy(alpha = 0.12f)
                                else VaultErrorRed.copy(alpha = 0.12f)
                            )
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (isSuccess) VaultSuccessGreen else VaultErrorRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = testStatusMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSuccess) VaultSuccessGreen else VaultErrorRed
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && botToken.isNotBlank() && chatId.isNotBlank()) {
                        onSave(
                            TelegramChannel(
                                id = initialChannel?.id ?: 0L,
                                name = name.trim(),
                                botToken = botToken.trim(),
                                chatId = chatId.trim(),
                                topicId = topicIdStr.toIntOrNull()
                            )
                        )
                    }
                },
                enabled = name.isNotBlank() && botToken.isNotBlank() && chatId.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("save_channel_confirm_button")
            ) {
                Text("Save Channel")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
