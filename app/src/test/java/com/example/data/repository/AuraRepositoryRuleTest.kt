package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.UserProfile
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuraRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun testAuthenticatedUserCanSaveProfileAndCreateSession() = runBlocking {
        val uid = signInTestUser("aura_user@example.com")
        val repository = AuraRepository(firestore, auth)

        val profile = UserProfile(
            id = uid,
            userId = uid,
            displayName = "Alex",
            voicePreference = "Aoede",
            preferredLanguage = "en",
            cloudHistoryEnabled = true,
            onboarded = true
        )
        val saveResult = repository.saveUserProfile(profile)
        assertTrue("Profile save should succeed", saveResult.isSuccess)

        val sessionResult = repository.createSession(uid, "Evening Reflection", "Aoede", "en")
        assertTrue("Session creation should succeed", sessionResult.isSuccess)
        val sessionId = sessionResult.getOrNull()
        assertNotNull(sessionId)

        val msgResult = repository.addMessage(uid, sessionId!!, "user", "I had a stressful day at work", "text")
        assertTrue("Message addition should succeed", msgResult.isSuccess)

        val messages = repository.observeMessages(uid, sessionId)
            .filter { it.isNotEmpty() }
            .first()
        assertTrue("Messages list should have at least 1 message", messages.isNotEmpty())
        assertEquals("user", messages.first().role)
    }

    @Test
    fun testUnauthenticatedUserCannotCreateRemoteSession() = runBlocking {
        auth.signOut()
        val repository = AuraRepository(firestore, auth)

        // Remote write with dummy userId while unauthenticated should fail under security rules
        val result = repository.createSession("random_user_123", "Forbidden Session", "Aoede", "en")
        assertTrue("Unauthenticated remote session creation must fail", result.isFailure)
    }
}
