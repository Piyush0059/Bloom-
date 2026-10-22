package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.Message
import com.example.data.model.OrbState
import com.example.data.model.Session
import com.example.ui.components.AIIntelligenceCore
import com.example.ui.theme.AuraBorderNavy
import com.example.ui.theme.AuraCosmicBlack
import com.example.ui.theme.AuraCrisisCoral
import com.example.ui.theme.AuraCyanGlow
import com.example.ui.theme.AuraDeepNavy
import com.example.ui.theme.AuraIceCyan
import com.example.ui.theme.AuraIceWhite
import com.example.ui.theme.AuraLavender
import com.example.ui.theme.AuraSurfaceElevated
import com.example.ui.theme.AuraSurfaceNavy
import com.example.ui.theme.AuraTextMuted
import com.example.ui.theme.AuraTextPrimary
import com.example.ui.theme.AuraTextSecondary
import com.example.ui.theme.AuraViolet
import com.example.ui.theme.AuraWarningAmber
import com.example.ui.theme.TechnicalMonospace
import com.example.ui.theme.TechnicalMonospaceLarge
import com.example.ui.viewmodel.AuraViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    viewModel: AuraViewModel,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHelpNow: () -> Unit
) {
    val context = LocalContext.current
    val orbState by viewModel.orbState.collectAsState()
    val isVoiceMode by viewModel.isVoiceMode.collectAsState()
    val isDeepThinking by viewModel.isDeepThinking.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val textInput by viewModel.textInput.collectAsState()
    val showFeedbackPrompt by viewModel.showFeedbackPrompt.collectAsState()
    val isGuest by viewModel.isGuestMode.collectAsState()
    val isListening by viewModel.voiceManager.isListening.collectAsState()
    val isPlayingAudio by viewModel.voiceManager.isPlayingAudio.collectAsState()
    val voiceError by viewModel.voiceManager.voiceError.collectAsState()
    val partialTranscript by viewModel.voiceManager.partialTranscript.collectAsState()
    val audioLevel by viewModel.voiceManager.rmsLevel.collectAsState()

    var showTranscriptSheet by remember { mutableStateOf(false) }

    // Dynamic orb state synchronization
    val currentOrbState = remember(orbState, isListening, isPlayingAudio) {
        when {
            orbState == OrbState.CRISIS -> OrbState.CRISIS
            orbState == OrbState.INTERRUPTED -> OrbState.INTERRUPTED
            isListening -> OrbState.LISTENING
            isPlayingAudio -> OrbState.SPEAKING
            orbState == OrbState.PROCESSING -> OrbState.PROCESSING
            else -> OrbState.IDLE
        }
    }

    // Microphone Permission Launcher
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startVoiceTurn()
        }
    }

    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "BLOOM",
                            style = MaterialTheme.typography.titleLarge,
                            color = AuraIceCyan,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.5.sp
                        )
                        Spacer(modifier = Modifier.width(12.dp))

                        // Connection Status Monospace Badge
                        Box(
                            modifier = Modifier
                                .background(AuraSurfaceNavy, RoundedCornerShape(4.dp))
                                .border(1.dp, AuraBorderNavy, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isGuest) "GUEST // LOCAL" else "CLOUD // FIRESTORE",
                                style = TechnicalMonospace,
                                color = if (isGuest) AuraTextMuted else AuraCyanGlow
                            )
                        }

                        if (isDeepThinking) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(AuraLavender.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .border(1.dp, AuraLavender.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "THINKING: HIGH",
                                    style = TechnicalMonospace,
                                    color = AuraLavender
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Permanently reachable Help Now emergency shield
                    IconButton(
                        onClick = onNavigateToHelpNow,
                        modifier = Modifier.testTag("top_bar_help_now_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Help Now Crisis Hotlines",
                            tint = AuraCrisisCoral
                        )
                    }

                    // Deep Thinking Mode Toggle
                    IconButton(
                        onClick = { viewModel.toggleDeepThinking() },
                        modifier = Modifier.testTag("top_bar_deep_thinking_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = "Toggle Deep Thinking Mode",
                            tint = if (isDeepThinking) AuraLavender else AuraTextMuted
                        )
                    }

                    // History
                    IconButton(
                        onClick = onNavigateToHistory,
                        modifier = Modifier.testTag("top_bar_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History",
                            tint = AuraTextSecondary
                        )
                    }

                    // Settings
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("top_bar_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = AuraTextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AuraDeepNavy)
            )
        },
        containerColor = AuraCosmicBlack
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val isWideScreen = maxWidth >= 760.dp

            if (isWideScreen) {
                // Desktop / Tablet Three-Zone Composition
                Row(modifier = Modifier.fillMaxSize()) {
                    // Zone 1: Collapsible History Sidebar
                    Surface(
                        modifier = Modifier
                            .width(260.dp)
                            .fillMaxHeight(),
                        color = AuraDeepNavy,
                        tonalElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        ) {
                            Button(
                                onClick = { viewModel.startNewSession("Reflective Dialogue") },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AuraIceCyan,
                                    contentColor = AuraCosmicBlack
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("desktop_new_session_btn")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("New Session", style = MaterialTheme.typography.labelLarge)
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "RECENT DIALOGUES",
                                style = TechnicalMonospace,
                                color = AuraTextMuted,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                            )

                            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(sessions, key = { it.id }) { s ->
                                    val isSelected = s.id == activeSession?.id
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                if (isSelected) AuraSurfaceElevated else Color.Transparent,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .border(
                                                1.dp,
                                                if (isSelected) AuraIceCyan.copy(alpha = 0.5f) else Color.Transparent,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { viewModel.selectSession(s) }
                                            .padding(horizontal = 10.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = s.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isSelected) AuraIceCyan else AuraTextSecondary,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Zone 2: Central 3D Spherical Stage
                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Mode Switcher Pill
                        ConversationModePill(
                            isVoiceMode = isVoiceMode,
                            onSetVoiceMode = { viewModel.setVoiceMode(it) }
                        )

                        // 3D Spherical Intelligence Core
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                AIIntelligenceCore(
                                    state = currentOrbState,
                                    size = 300.dp,
                                    audioLevel = audioLevel
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                StateTelemetryBadge(state = currentOrbState, isDeepThinking = isDeepThinking)
                            }
                        }

                        // Bottom Thumb-Friendly Interaction Controls
                        ConversationControlsBar(
                            isVoiceMode = isVoiceMode,
                            isListening = isListening,
                            isPlayingAudio = isPlayingAudio,
                            textInput = textInput,
                            onTextInputChange = { viewModel.setTextInput(it) },
                            onSendMessage = { text, modality -> viewModel.onUserSendMessage(text, modality) },
                            onStartVoice = {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasPermission) {
                                    viewModel.startVoiceTurn()
                                } else {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            onStopVoice = { viewModel.stopVoiceTurnManually() },
                            onInterrupt = { viewModel.interruptPlayback() },
                            onReplay = { viewModel.replayLastAssistantAudio() }
                        )
                    }

                    // Zone 3: Live Transcript & Details Panel
                    Surface(
                        modifier = Modifier
                            .width(360.dp)
                            .fillMaxHeight(),
                        color = AuraSurfaceNavy,
                        tonalElevation = 1.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "LIVE TRANSCRIPT",
                                style = TechnicalMonospaceLarge,
                                color = AuraIceCyan
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(messages, key = { it.id }) { msg ->
                                    TranscriptBubble(msg)
                                }
                            }
                        }
                    }
                }
            } else {
                // Mobile Viewport (40-50% Viewport Sphere Stage, Shrinks Gracefully)
                val orbSizeDp = if (showTranscriptSheet) 180.dp else 260.dp

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .animateContentSize(animationSpec = tween(300)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Mode Selector Pill
                    ConversationModePill(
                        isVoiceMode = isVoiceMode,
                        onSetVoiceMode = { viewModel.setVoiceMode(it) }
                    )

                    // Central 3D Spherical Stage
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            AIIntelligenceCore(
                                state = currentOrbState,
                                size = orbSizeDp,
                                audioLevel = audioLevel
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            StateTelemetryBadge(
                                state = currentOrbState,
                                isDeepThinking = isDeepThinking
                            )

                            // Live partial transcript preview while listening
                            if (isListening && partialTranscript.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "\"$partialTranscript\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AuraIceWhite,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 20.dp)
                                )
                            }

                            if ((statusMessage != null || voiceError != null) && currentOrbState == OrbState.IDLE) {
                                Text(
                                    text = voiceError ?: statusMessage ?: "",
                                    style = TechnicalMonospace,
                                    color = if (voiceError != null) AuraCrisisCoral else AuraTextMuted,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            // Support Effectiveness Rating Chips
                            AnimatedVisibility(
                                visible = showFeedbackPrompt && currentOrbState == OrbState.IDLE,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                Card(
                                    modifier = Modifier
                                        .padding(top = 10.dp)
                                        .fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = AuraSurfaceNavy),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AuraBorderNavy)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = "Did this reflection feel supportive?",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = AuraTextPrimary,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            FeedbackButton("Yes, helped", modifier = Modifier.weight(1f)) {
                                                viewModel.rateInteraction("helpful")
                                            }
                                            FeedbackButton("A little", modifier = Modifier.weight(1f)) {
                                                viewModel.rateInteraction("little")
                                            }
                                            FeedbackButton("Not yet", modifier = Modifier.weight(1f)) {
                                                viewModel.rateInteraction("not_yet")
                                            }
                                            FeedbackButton("Change approach", modifier = Modifier.weight(1f)) {
                                                viewModel.rateInteraction("change_approach")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Expandable Live Transcript Panel
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = AuraSurfaceNavy),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AuraBorderNavy)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showTranscriptSheet = !showTranscriptSheet }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Chat,
                                    contentDescription = null,
                                    tint = AuraIceCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (showTranscriptSheet) "HIDE TRANSCRIPT" else "VIEW TRANSCRIPT (${messages.size})",
                                    style = TechnicalMonospace,
                                    color = AuraTextPrimary
                                )
                            }
                            Icon(
                                imageVector = if (showTranscriptSheet) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                contentDescription = null,
                                tint = AuraTextSecondary
                            )
                        }

                        if (showTranscriptSheet) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(messages, key = { it.id }) { msg ->
                                        TranscriptBubble(msg)
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Controls Bar
                    ConversationControlsBar(
                        isVoiceMode = isVoiceMode,
                        isListening = isListening,
                        isPlayingAudio = isPlayingAudio,
                        textInput = textInput,
                        onTextInputChange = { viewModel.setTextInput(it) },
                        onSendMessage = { text, modality -> viewModel.onUserSendMessage(text, modality) },
                        onStartVoice = {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPermission) {
                                viewModel.startVoiceTurn()
                            } else {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        onStopVoice = { viewModel.stopVoiceTurnManually() },
                        onInterrupt = { viewModel.interruptPlayback() },
                        onReplay = { viewModel.replayLastAssistantAudio() }
                    )
                }
            }
        }
    }
}

@Composable
private fun StateTelemetryBadge(state: OrbState, isDeepThinking: Boolean) {
    val (label, color) = when (state) {
        OrbState.CRISIS -> "SAFETY PROTOCOL // IMMEDIATE ASSISTANCE ENGAGED" to AuraCrisisCoral
        OrbState.LISTENING -> "LISTENING // AUDIO INPUT ACTIVE" to AuraCyanGlow
        OrbState.PROCESSING -> (if (isDeepThinking) "THINKING // DEEP NEURAL SYNTHESIS" else "PROCESSING // REFLECTIVE COGNITION") to AuraLavender
        OrbState.SPEAKING -> "TRANSMITTING // SPOKEN VOCALIZATION" to AuraIceCyan
        OrbState.INTERRUPTED -> "HALTED // PLAYBACK INTERRUPTED" to AuraWarningAmber
        OrbState.IDLE -> "IDLE // READY" to AuraTextSecondary
    }

    Box(
        modifier = Modifier
            .background(AuraSurfaceNavy, RoundedCornerShape(6.dp))
            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = TechnicalMonospace,
            color = color,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ConversationModePill(
    isVoiceMode: Boolean,
    onSetVoiceMode: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .padding(top = 8.dp)
            .background(AuraSurfaceNavy, RoundedCornerShape(20.dp))
            .border(1.dp, AuraBorderNavy, RoundedCornerShape(20.dp))
            .padding(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .background(
                    if (isVoiceMode) AuraIceCyan else Color.Transparent,
                    RoundedCornerShape(16.dp)
                )
                .clickable { onSetVoiceMode(true) }
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .testTag("mode_switch_voice"),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = if (isVoiceMode) AuraCosmicBlack else AuraTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "VOICE",
                    style = TechnicalMonospace,
                    color = if (isVoiceMode) AuraCosmicBlack else AuraTextSecondary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Box(
            modifier = Modifier
                .background(
                    if (!isVoiceMode) AuraIceCyan else Color.Transparent,
                    RoundedCornerShape(16.dp)
                )
                .clickable { onSetVoiceMode(false) }
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .testTag("mode_switch_text"),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Chat,
                    contentDescription = null,
                    tint = if (!isVoiceMode) AuraCosmicBlack else AuraTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "TEXT",
                    style = TechnicalMonospace,
                    color = if (!isVoiceMode) AuraCosmicBlack else AuraTextSecondary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ConversationControlsBar(
    isVoiceMode: Boolean,
    isListening: Boolean,
    isPlayingAudio: Boolean,
    textInput: String,
    onTextInputChange: (String) -> Unit,
    onSendMessage: (String, String) -> Unit,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit,
    onInterrupt: () -> Unit,
    onReplay: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AuraDeepNavy,
        tonalElevation = 8.dp
    ) {
        if (isVoiceMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Replay Button
                IconButton(
                    onClick = onReplay,
                    modifier = Modifier
                        .size(48.dp)
                        .background(AuraSurfaceElevated, CircleShape)
                        .border(1.dp, AuraBorderNavy, CircleShape)
                        .testTag("replay_audio_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = "Replay Response",
                        tint = AuraTextSecondary
                    )
                }

                // Main Microphone / Done Button
                if (isListening) {
                    Button(
                        onClick = onStopVoice,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AuraIceCyan,
                            contentColor = AuraCosmicBlack
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .height(56.dp)
                            .testTag("voice_done_button")
                    ) {
                        Icon(Icons.Default.Done, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "FINISH TURN",
                            style = TechnicalMonospaceLarge,
                            color = AuraCosmicBlack
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    listOf(AuraIceCyan, AuraViolet)
                                ),
                                shape = CircleShape
                            )
                            .border(2.dp, AuraCyanGlow, CircleShape)
                            .clickable(onClick = onStartVoice)
                            .testTag("voice_mic_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Tap to speak",
                            tint = AuraCosmicBlack,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // Immediate Interruption Control
                IconButton(
                    onClick = onInterrupt,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (isPlayingAudio) AuraWarningAmber.copy(alpha = 0.25f) else AuraSurfaceElevated,
                            CircleShape
                        )
                        .border(
                            1.dp,
                            if (isPlayingAudio) AuraWarningAmber else AuraBorderNavy,
                            CircleShape
                        )
                        .testTag("interrupt_audio_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Interrupt Playback",
                        tint = if (isPlayingAudio) AuraWarningAmber else AuraTextSecondary
                    )
                }
            }
        } else {
            // Text Input Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = onTextInputChange,
                    placeholder = { Text("Share what you are feeling...", color = AuraTextMuted) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("text_mode_input"),
                    shape = RoundedCornerShape(20.dp),
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AuraIceCyan,
                        unfocusedBorderColor = AuraBorderNavy,
                        focusedTextColor = AuraTextPrimary,
                        unfocusedTextColor = AuraTextPrimary
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (textInput.isNotBlank()) {
                                onSendMessage(textInput, "text")
                            }
                        }
                    )
                )

                IconButton(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            onSendMessage(textInput, "text")
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(AuraIceCyan, CircleShape)
                        .testTag("text_mode_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send message",
                        tint = AuraCosmicBlack,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedbackButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(34.dp)
            .background(AuraSurfaceElevated, RoundedCornerShape(6.dp))
            .border(1.dp, AuraBorderNavy, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = AuraTextPrimary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TranscriptBubble(message: Message) {
    val isUser = message.role == "user"
    val isCrisis = message.role == "crisis_alert"

    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bubbleColor = when {
        isCrisis -> AuraCrisisCoral.copy(alpha = 0.18f)
        isUser -> AuraSurfaceElevated
        else -> AuraSurfaceNavy
    }
    val borderColor = when {
        isCrisis -> AuraCrisisCoral
        isUser -> AuraIceCyan.copy(alpha = 0.45f)
        else -> AuraBorderNavy
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = bubbleColor),
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (isUser) 14.dp else 2.dp,
                bottomEnd = if (isUser) 2.dp else 14.dp
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isUser) "YOU" else if (isCrisis) "SAFETY DIRECTIVE" else "BLOOM CORE",
                        style = TechnicalMonospace,
                        color = if (isUser) AuraIceCyan else if (isCrisis) AuraCrisisCoral else AuraLavender,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = message.modality.uppercase(),
                        style = TechnicalMonospace,
                        color = AuraTextMuted
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AuraTextPrimary,
                    lineHeight = 22.sp
                )
            }
        }
    }
}
