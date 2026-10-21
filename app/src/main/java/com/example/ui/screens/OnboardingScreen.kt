package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.OrbState
import com.example.ui.components.AIIntelligenceCore
import com.example.ui.theme.AuraBorderNavy
import com.example.ui.theme.AuraCosmicBlack
import com.example.ui.theme.AuraCyanGlow
import com.example.ui.theme.AuraDeepNavy
import com.example.ui.theme.AuraIceCyan
import com.example.ui.theme.AuraLavender
import com.example.ui.theme.AuraSurfaceElevated
import com.example.ui.theme.AuraSurfaceNavy
import com.example.ui.theme.AuraTextMuted
import com.example.ui.theme.AuraTextPrimary
import com.example.ui.theme.AuraTextSecondary
import com.example.ui.theme.TechnicalMonospace

@Composable
fun OnboardingScreen(
    onComplete: (voicePreference: String, language: String, stayGuest: Boolean) -> Unit,
    onGoogleSignInClicked: () -> Unit
) {
    var selectedVoice by remember { mutableStateOf("Aoede") }
    var selectedLanguage by remember { mutableStateOf("en") }

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(AuraCosmicBlack, AuraDeepNavy, AuraCosmicBlack)
                )
            )
            .padding(horizontal = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Living 3D Spherical Intelligence Core
            AIIntelligenceCore(
                state = OrbState.IDLE,
                size = 180.dp,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Technical Monospace Initialization Badge
            Box(
                modifier = Modifier
                    .background(AuraSurfaceNavy, RoundedCornerShape(4.dp))
                    .border(1.dp, AuraBorderNavy, RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "AURA CORE // INITIALIZING EMPATHY PROTOCOL",
                    style = TechnicalMonospace,
                    color = AuraCyanGlow
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "AURA",
                style = MaterialTheme.typography.displayLarge.copy(
                    brush = Brush.horizontalGradient(listOf(AuraIceCyan, AuraLavender))
                ),
                letterSpacing = 4.sp
            )

            Text(
                text = "Emotional Support Companion",
                style = MaterialTheme.typography.titleMedium,
                color = AuraTextSecondary,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Step 1: Voice Persona Selection
            Text(
                text = "1. Choose Your Vocal Intelligence",
                style = MaterialTheme.typography.titleMedium,
                color = AuraTextPrimary,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Change your selection anytime in settings.",
                style = MaterialTheme.typography.bodySmall,
                color = AuraTextMuted,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )

            VoiceChoiceCard(
                title = "Warm Feminine Voice",
                subtitle = "Gentle, reflective cadence (Aoede)",
                isSelected = selectedVoice == "Aoede",
                icon = Icons.Default.RecordVoiceOver,
                tag = "voice_feminine_card",
                onClick = { selectedVoice = "Aoede" }
            )

            Spacer(modifier = Modifier.height(8.dp))

            VoiceChoiceCard(
                title = "Warm Masculine Voice",
                subtitle = "Steady, grounding resonance (Puck)",
                isSelected = selectedVoice == "Puck",
                icon = Icons.Default.Mic,
                tag = "voice_masculine_card",
                onClick = { selectedVoice = "Puck" }
            )

            Spacer(modifier = Modifier.height(8.dp))

            VoiceChoiceCard(
                title = "Text Only",
                subtitle = "Quiet chat without spoken vocalization",
                isSelected = selectedVoice == "None",
                icon = Icons.AutoMirrored.Filled.Chat,
                tag = "voice_text_only_card",
                onClick = { selectedVoice = "None" }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Step 2: Language Preference
            Text(
                text = "2. Preferred Dialogue Language",
                style = MaterialTheme.typography.titleMedium,
                color = AuraTextPrimary,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LanguageChip(
                    label = "English",
                    isSelected = selectedLanguage == "en",
                    modifier = Modifier.weight(1f),
                    onClick = { selectedLanguage = "en" }
                )
                LanguageChip(
                    label = "한국어",
                    isSelected = selectedLanguage == "ko",
                    modifier = Modifier.weight(1f),
                    onClick = { selectedLanguage = "ko" }
                )
                LanguageChip(
                    label = "Hinglish",
                    isSelected = selectedLanguage == "hinglish",
                    modifier = Modifier.weight(1f),
                    onClick = { selectedLanguage = "hinglish" }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Transparent AI Disclosure Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AuraSurfaceNavy.copy(alpha = 0.85f)),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AuraBorderNavy)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Disclosure info",
                            tint = AuraIceCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Transparent AI Disclosure",
                            style = MaterialTheme.typography.titleSmall,
                            color = AuraTextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• AURA is an AI companion designed for emotional reflection, not a human, therapist, or medical device.\n• It cannot diagnose conditions or guarantee healing.\n• Not for crisis care. Dedicated help numbers (like Tele-MANAS 14416) are always available.\n• Microphone access is only requested if you begin speaking.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AuraTextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // Sign In with Google or Continue as Guest
            Button(
                onClick = onGoogleSignInClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("onboarding_google_signin_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AuraIceCyan,
                    contentColor = AuraCosmicBlack
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sign in with Google (Cloud Sync)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = { onComplete(selectedVoice, selectedLanguage, true) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("onboarding_guest_button"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AuraTextSecondary),
                border = androidx.compose.foundation.BorderStroke(1.dp, AuraBorderNavy),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Continue as Guest (Local Data Only)",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun VoiceChoiceCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) AuraSurfaceElevated else AuraSurfaceNavy
        ),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) AuraIceCyan else AuraBorderNavy
        )
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        color = if (isSelected) AuraIceCyan.copy(alpha = 0.2f) else AuraDeepNavy,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) AuraIceCyan else AuraTextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isSelected) AuraCyanGlow else AuraTextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = AuraTextSecondary
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = AuraIceCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun LanguageChip(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .background(
                color = if (isSelected) AuraSurfaceElevated else AuraSurfaceNavy,
                shape = RoundedCornerShape(10.dp)
            )
            .border(
                width = 1.dp,
                color = if (isSelected) AuraIceCyan else AuraBorderNavy,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) AuraIceCyan else AuraTextSecondary,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }
}
