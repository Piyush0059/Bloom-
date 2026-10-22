package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.audio.VoiceManager
import com.example.data.repository.AuraRepository
import com.example.ui.screens.ConversationScreen
import com.example.ui.screens.HelpNowScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AuraCosmicBlack
import com.example.ui.theme.AuraTheme
import com.example.ui.viewmodel.AuraViewModel
import com.google.firebase.FirebaseApp

enum class AuraScreen {
    ONBOARDING,
    CONVERSATION,
    HELP_NOW,
    HISTORY,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    private lateinit var voiceManager: VoiceManager
    private lateinit var repository: AuraRepository
    private lateinit var viewModel: AuraViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        try {
            FirebaseApp.initializeApp(this)
        } catch (_: Exception) {}

        voiceManager = VoiceManager(this, lifecycleScope)
        repository = AuraRepository(this)
        viewModel = AuraViewModel(repository, voiceManager)

        setContent {
            AuraTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AuraCosmicBlack
                ) {
                    AuraApp(
                        viewModel = viewModel,
                        activity = this
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceManager.release()
    }
}

@Composable
fun AuraApp(
    viewModel: AuraViewModel,
    activity: ComponentActivity
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val isHelpNowVisible by viewModel.isHelpNowVisible.collectAsState()

    var currentScreen by remember { mutableStateOf<AuraScreen?>(null) }

    // If help now is triggered from crisis alert, switch screen
    if (isHelpNowVisible && currentScreen != AuraScreen.HELP_NOW) {
        currentScreen = AuraScreen.HELP_NOW
    }

    val activeScreen = currentScreen ?: if (!userProfile.onboarded) AuraScreen.ONBOARDING else AuraScreen.CONVERSATION

    when (activeScreen) {
        AuraScreen.ONBOARDING -> {
            OnboardingScreen(
                onComplete = { voice, lang, guest ->
                    viewModel.completeOnboarding(voice, lang, guest)
                    currentScreen = AuraScreen.CONVERSATION
                },
                onGoogleSignInClicked = {
                    viewModel.signInWithGoogle(
                        context = activity,
                        activity = activity,
                        onSuccess = {
                            viewModel.completeOnboarding("None", "en", false)
                            currentScreen = AuraScreen.CONVERSATION
                            Toast.makeText(activity, "Welcome to Bloom!", Toast.LENGTH_SHORT).show()
                        },
                        onError = { err ->
                            Toast.makeText(activity, err, Toast.LENGTH_LONG).show()
                        }
                    )
                }
            )
        }

        AuraScreen.CONVERSATION -> {
            ConversationScreen(
                viewModel = viewModel,
                onNavigateToHistory = { currentScreen = AuraScreen.HISTORY },
                onNavigateToSettings = { currentScreen = AuraScreen.SETTINGS },
                onNavigateToHelpNow = { currentScreen = AuraScreen.HELP_NOW }
            )
        }

        AuraScreen.HELP_NOW -> {
            BackHandler {
                viewModel.setHelpNowVisible(false)
                currentScreen = AuraScreen.CONVERSATION
            }
            HelpNowScreen(
                viewModel = viewModel,
                onBack = {
                    viewModel.setHelpNowVisible(false)
                    currentScreen = AuraScreen.CONVERSATION
                }
            )
        }

        AuraScreen.HISTORY -> {
            BackHandler {
                currentScreen = AuraScreen.CONVERSATION
            }
            HistoryScreen(
                viewModel = viewModel,
                onBack = { currentScreen = AuraScreen.CONVERSATION },
                onSessionSelected = { currentScreen = AuraScreen.CONVERSATION }
            )
        }

        AuraScreen.SETTINGS -> {
            BackHandler {
                currentScreen = AuraScreen.CONVERSATION
            }
            SettingsScreen(
                viewModel = viewModel,
                onBack = { currentScreen = AuraScreen.CONVERSATION }
            )
        }
    }
}
