package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CrisisResource
import com.example.ui.theme.AuraBorderNavy
import com.example.ui.theme.AuraCosmicBlack
import com.example.ui.theme.AuraCrisisCoral
import com.example.ui.theme.AuraDeepNavy
import com.example.ui.theme.AuraIceCyan
import com.example.ui.theme.AuraLavender
import com.example.ui.theme.AuraSurfaceElevated
import com.example.ui.theme.AuraSurfaceNavy
import com.example.ui.theme.AuraTextMuted
import com.example.ui.theme.AuraTextPrimary
import com.example.ui.theme.AuraTextSecondary
import com.example.ui.theme.AuraWarningAmber
import com.example.ui.viewmodel.AuraViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpNowScreen(
    viewModel: AuraViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var searchQuery by remember { mutableStateOf("") }
    val isSearching by viewModel.isSearchingGroundedCrisis.collectAsState()
    val searchResults by viewModel.groundedCrisisResults.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = AuraCrisisCoral,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Help Now & Safety Support",
                            style = MaterialTheme.typography.titleLarge,
                            color = AuraTextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("help_now_back_button")
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
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Calm Protective Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AuraCrisisCoral.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AuraCrisisCoral.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = AuraCrisisCoral,
                        modifier = Modifier.size(28.dp).padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "You Are Not Alone",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFFEE2E2),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "If you or someone around you is in immediate danger, distress, or having thoughts of self-harm, please connect with human support right away. These lines are completely free and confidential.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AuraTextPrimary,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // Direct Tap-to-Call Hotlines
            Text(
                text = "Tap-To-Call Crisis Resources",
                style = MaterialTheme.typography.titleMedium,
                color = AuraIceCyan,
                fontWeight = FontWeight.SemiBold
            )

            viewModel.defaultCrisisResources.forEach { resource ->
                CrisisResourceCard(
                    resource = resource,
                    onCall = { dialNumber(context, resource.phone) }
                )
            }

            // Box Breathing & Grounding Exercise
            GroundingExerciseCard()

            // 5-4-3-2-1 Sensory Grounding Technique
            SensoryGroundingCard()

            // Search Grounded Helplines Tool
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AuraSurfaceNavy),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AuraBorderNavy)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Find Local Helplines in Your State / City",
                        style = MaterialTheme.typography.titleMedium,
                        color = AuraTextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Powered by Google Search Grounding to find verified, up-to-date support groups and crisis hotlines.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AuraTextSecondary,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("e.g. Bangalore, Delhi, Mumbai, London", color = AuraTextMuted) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("crisis_search_input"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AuraIceCyan,
                                unfocusedBorderColor = AuraBorderNavy,
                                focusedTextColor = AuraTextPrimary,
                                unfocusedTextColor = AuraTextPrimary
                            )
                        )

                        Button(
                            onClick = {
                                if (searchQuery.isNotBlank()) {
                                    viewModel.searchCrisisHelplines(searchQuery)
                                }
                            },
                            enabled = !isSearching && searchQuery.isNotBlank(),
                            modifier = Modifier.testTag("crisis_search_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AuraIceCyan,
                                contentColor = AuraCosmicBlack
                            )
                        ) {
                            if (isSearching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = AuraCosmicBlack,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }
                        }
                    }

                    if (searchResults != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = AuraSurfaceElevated),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = searchResults ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AuraTextPrimary,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }
            }

            // Legal & Disclaimer
            Text(
                text = "Disclaimer: AURA is an AI conversational companion and is NOT an emergency dispatch or psychological healthcare provider. In severe crises, contact national emergency authorities immediately.",
                style = MaterialTheme.typography.labelSmall,
                color = AuraTextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 24.dp)
            )
        }
    }
}

@Composable
private fun CrisisResourceCard(
    resource: CrisisResource,
    onCall: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AuraSurfaceNavy),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AuraBorderNavy)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = resource.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = AuraTextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(AuraSurfaceElevated, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = resource.region,
                            style = MaterialTheme.typography.labelSmall,
                            color = AuraLavender
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = resource.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = AuraTextSecondary
                )
                Text(
                    text = resource.hours,
                    style = MaterialTheme.typography.labelSmall,
                    color = AuraWarningAmber,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Button(
                onClick = onCall,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AuraCrisisCoral,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("call_button_${resource.phone}")
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Call ${resource.phone}",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = resource.phone, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun GroundingExerciseCard() {
    val transition = rememberInfiniteTransition(label = "breathing")
    val scale by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath_scale"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AuraSurfaceNavy),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AuraBorderNavy)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.SelfImprovement,
                    contentDescription = null,
                    tint = AuraIceCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Box Breathing (4-4-4-4)",
                    style = MaterialTheme.typography.titleMedium,
                    color = AuraTextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .size(90.dp)
                    .scale(scale)
                    .background(
                        brush = Brush.radialGradient(
                            listOf(AuraIceCyan.copy(alpha = 0.6f), AuraLavender.copy(alpha = 0.2f), Color.Transparent)
                        ),
                        shape = CircleShape
                    )
                    .border(1.5.dp, AuraIceCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Breathe",
                    style = MaterialTheme.typography.labelLarge,
                    color = AuraTextPrimary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Inhale for 4 seconds • Hold for 4 seconds • Exhale for 4 seconds • Rest for 4 seconds",
                style = MaterialTheme.typography.bodySmall,
                color = AuraTextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SensoryGroundingCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AuraSurfaceNavy),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AuraBorderNavy)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "5-4-3-2-1 Sensory Grounding",
                style = MaterialTheme.typography.titleMedium,
                color = AuraTextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "When overwhelmed, pause and notice:\n• 5 things you can SEE around the room\n• 4 things you can physically TOUCH or feel\n• 3 things you can HEAR right now\n• 2 things you can SMELL\n• 1 positive thing you appreciate about yourself",
                style = MaterialTheme.typography.bodySmall,
                color = AuraTextSecondary,
                lineHeight = 20.sp
            )
        }
    }
}

private fun dialNumber(context: Context, number: String) {
    try {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$number")
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback
    }
}
