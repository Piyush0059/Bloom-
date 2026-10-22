package com.example.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AuraBorderNavy
import com.example.ui.theme.AuraCosmicBlack
import com.example.ui.theme.AuraCrisisCoral
import com.example.ui.theme.AuraCyanGlow
import com.example.ui.theme.AuraDeepNavy
import com.example.ui.theme.AuraIceCyan
import com.example.ui.theme.AuraLavender
import com.example.ui.theme.AuraSurfaceElevated
import com.example.ui.theme.AuraSurfaceNavy
import com.example.ui.theme.AuraTextMuted
import com.example.ui.theme.AuraTextPrimary
import com.example.ui.theme.AuraTextSecondary
import com.example.ui.theme.AuraViolet
import com.example.ui.viewmodel.AuraViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AuraViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isGuest by viewModel.isGuestMode.collectAsState()

    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var voiceIdDraft by remember(userProfile.customVoiceId) { mutableStateOf(userProfile.customVoiceId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = AuraIceCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Settings & Privacy",
                            style = MaterialTheme.typography.titleLarge,
                            color = AuraTextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = AuraTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AuraDeepNavy)
            )
        },
        containerColor = AuraCosmicBlack
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Account & Sync Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AuraSurfaceNavy),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AuraBorderNavy)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = AuraLavender,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Account & Cloud Sync",
                            style = MaterialTheme.typography.titleMedium,
                            color = AuraTextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    if (currentUser != null) {
                        Text(
                            text = "Signed in as: ${currentUser?.displayName ?: currentUser?.email ?: "Google User"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AuraTextPrimary
                        )
                        Text(
                            text = "Cloud Firestore sync is active with user isolation.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AuraCyanGlow,
                            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                        )

                        OutlinedButton(
                            onClick = { viewModel.signOutUser(context) },
                            modifier = Modifier.testTag("sign_out_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AuraTextSecondary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AuraBorderNavy)
                        ) {
                            Text("Sign Out")
                        }
                    } else {
                        Text(
                            text = "Operating in Guest Mode. Data stays in local memory on this device.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AuraTextSecondary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Button(
                            onClick = {
                                if (context is Activity) {
                                    viewModel.signInWithGoogle(
                                        context = context,
                                        activity = context,
                                        onSuccess = { Toast.makeText(context, "Signed in successfully!", Toast.LENGTH_SHORT).show() },
                                        onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_google_signin_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AuraIceCyan,
                                contentColor = AuraCosmicBlack
                            )
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign in with Google")
                        }
                    }
                }
            }

            // Friend's Voice — no prebuilt/system fallback
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AuraSurfaceNavy),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AuraBorderNavy)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Friend's Voice",
                        style = MaterialTheme.typography.titleMedium,
                        color = AuraTextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Bloom will speak only with the friend voice ID you configure below. If it is missing or fails, Bloom stays silent and keeps the text reply.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AuraTextMuted,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )
                    SettingOptionRow(
                        title = "Use Friend's Replicated Voice",
                        isSelected = userProfile.voicePreference == "Friend",
                        onClick = {
                            viewModel.updatePreferences("Friend", userProfile.preferredLanguage, userProfile.cloudHistoryEnabled)
                            if (!userProfile.customVoiceId.matches(Regex("^(voice_[A-Za-z0-9_-]{6,}|voicekey_[A-Za-z0-9_-]{6,})$"))) {
                                Toast.makeText(context, "Configure the friend's verified Gemini voice ID below first.", Toast.LENGTH_LONG).show()
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingOptionRow(
                        title = "Text Only (No Spoken Replies)",
                        isSelected = userProfile.voicePreference != "Friend",
                        onClick = { viewModel.updatePreferences("None", userProfile.preferredLanguage, userProfile.cloudHistoryEnabled) }
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = voiceIdDraft,
                        onValueChange = { voiceIdDraft = it },
                        modifier = Modifier.fillMaxWidth().testTag("custom_voice_id_input"),
                        label = { Text("Gemini replicated voice ID") },
                        placeholder = { Text("voice_…") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val message = viewModel.updateCustomVoiceId(voiceIdDraft)
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AuraIceCyan,
                            contentColor = AuraCosmicBlack
                        )
                    ) {
                        Text("Save Friend's Voice ID")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Google AI Studio requires a clean 10–30 second reference recording from the same adult speaker and a separate consent recording spoken by that person. The uploaded 8-second clip alone is not enough. Do not use this voice without your friend's explicit consent.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AuraTextMuted,
                        lineHeight = 18.sp
                    )
                }
            }

            // Language Selection
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AuraSurfaceNavy),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AuraBorderNavy)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Preferred Language",
                        style = MaterialTheme.typography.titleMedium,
                        color = AuraTextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    SettingOptionRow(
                        title = "English",
                        isSelected = userProfile.preferredLanguage == "en",
                        onClick = { viewModel.updatePreferences(userProfile.voicePreference, "en", userProfile.cloudHistoryEnabled) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingOptionRow(
                        title = "한국어 (Korean)",
                        isSelected = userProfile.preferredLanguage == "ko",
                        onClick = { viewModel.updatePreferences(userProfile.voicePreference, "ko", userProfile.cloudHistoryEnabled) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingOptionRow(
                        title = "Hinglish (Natural conversational blend)",
                        isSelected = userProfile.preferredLanguage == "hinglish",
                        onClick = { viewModel.updatePreferences(userProfile.voicePreference, "hinglish", userProfile.cloudHistoryEnabled) }
                    )
                }
            }

            // Privacy & Data Rights
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AuraSurfaceNavy),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AuraBorderNavy)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = AuraIceCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Privacy Controls & Data Ownership",
                            style = MaterialTheme.typography.titleMedium,
                            color = AuraTextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Transcripts are stored securely under your authenticated Firebase ID with zero-trust security rules.\n• Raw microphone audio is never stored or retained.\n• You can delete all your conversations and account profile at any moment.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AuraTextSecondary,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { showDeleteAccountDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AuraCrisisCoral.copy(alpha = 0.15f),
                            contentColor = AuraCrisisCoral
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("delete_all_data_button")
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete All My Saved Data")
                    }
                }
            }
        }
    }

    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { Text("Delete All Saved Data?", color = AuraCrisisCoral) },
            text = {
                Text(
                    text = "This will permanently wipe your profile, settings, and all conversation histories from cloud storage and local memory.",
                    color = AuraTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uid = currentUser?.uid ?: "guest_user"
                        viewModel.clearAllHistory()
                        showDeleteAccountDialog = false
                        Toast.makeText(context, "All data wiped successfully.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AuraCrisisCoral, contentColor = Color.White)
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text("Cancel", color = AuraTextSecondary)
                }
            },
            containerColor = AuraDeepNavy
        )
    }
}

@Composable
private fun SettingOptionRow(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isSelected) AuraSurfaceElevated else AuraSurfaceNavy,
                shape = RoundedCornerShape(10.dp)
            )
            .border(
                width = 1.dp,
                color = if (isSelected) AuraIceCyan else AuraBorderNavy,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) AuraCyanGlow else AuraTextPrimary,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = AuraIceCyan,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
