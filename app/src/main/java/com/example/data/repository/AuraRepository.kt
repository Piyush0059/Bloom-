package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Message
import com.example.data.model.Session
import com.example.data.model.UserProfile
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class AuraRepository(
    private val db: FirebaseFirestore?,
    private val auth: FirebaseAuth?
) {
    // Secondary constructor resolving firestore_database_id from string resources
    constructor(context: Context, auth: FirebaseAuth? = FirebaseAuth.getInstance()) : this(
        try {
            val resId = context.resources.getIdentifier("firestore_database_id", "string", context.packageName)
            val dbId = if (resId != 0) context.getString(resId) else null
            if (dbId != null) FirebaseFirestore.getInstance(dbId) else null
        } catch (e: Exception) {
            null
        },
        auth
    )

    // Local in-memory store for guest mode or offline fallback
    private var guestProfile = UserProfile(
        id = "guest_user",
        userId = "guest_user",
        displayName = "Guest Companion",
        voicePreference = "None",
        preferredLanguage = "en",
        cloudHistoryEnabled = false,
        onboarded = false,
        createdAt = Timestamp.now(),
        updatedAt = Timestamp.now()
    )
    private val guestSessions = mutableListOf<Session>()
    private val guestMessages = mutableMapOf<String, MutableList<Message>>() // sessionId -> list

    private fun requireUserId(explicitUserId: String? = null): String {
        return explicitUserId
            ?: auth?.currentUser?.uid
            ?: "guest_user"
    }

    // --- User Profile ---

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        if (userId == "guest_user" || db == null) {
            emit(guestProfile)
            return@flow
        }
        val docRef = db.collection("users").document(userId)
        emitAll(
            docRef.snapshots()
                .map { snapshot -> snapshot.toObject(UserProfile::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.GET, docRef.path)
                    emit(guestProfile.copy(userId = userId, id = userId))
                }
        )
    }

    suspend fun saveUserProfile(profile: UserProfile): Result<Unit> {
        val uid = profile.userId.ifBlank { requireUserId() }
        if (uid == "guest_user" || db == null) {
            guestProfile = profile.copy(userId = "guest_user", id = "guest_user")
            return Result.success(Unit)
        }

        val docRef = db.collection("users").document(uid)
        return try {
            val payload = mapOf(
                "id" to uid,
                "userId" to uid,
                "displayName" to profile.displayName,
                "email" to profile.email,
                "voicePreference" to profile.voicePreference,
                "customVoiceId" to profile.customVoiceId,
                "preferredLanguage" to profile.preferredLanguage,
                "cloudHistoryEnabled" to profile.cloudHistoryEnabled,
                "onboarded" to profile.onboarded,
                "createdAt" to (profile.createdAt ?: FieldValue.serverTimestamp()),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Sessions ---

    fun observeSessions(userId: String): Flow<List<Session>> = flow {
        if (userId == "guest_user" || db == null) {
            emit(guestSessions.sortedByDescending { it.createdAt?.seconds ?: 0L })
            return@flow
        }
        val path = "users/$userId/sessions"
        emitAll(
            db.collection("users").document(userId).collection("sessions")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot -> snapshot.toObjects(Session::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    emit(guestSessions)
                }
        )
    }

    suspend fun createSession(
        userId: String,
        title: String,
        voicePreference: String,
        language: String,
        customVoiceId: String = ""
    ): Result<String> {
        val uid = userId.ifBlank { requireUserId() }
        val sessionId = UUID.randomUUID().toString()

        if (uid == "guest_user" || db == null) {
            val newSession = Session(
                id = sessionId,
                userId = "guest_user",
                title = title,
                voicePreference = voicePreference,
                customVoiceId = customVoiceId,
                language = language,
                messageCount = 0L,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
            guestSessions.add(0, newSession)
            guestMessages[sessionId] = mutableListOf()
            return Result.success(sessionId)
        }

        val sessionRef = db.collection("users").document(uid).collection("sessions").document(sessionId)
        return try {
            val payload = mapOf(
                "id" to sessionId,
                "userId" to uid,
                "title" to title,
                "voicePreference" to voicePreference,
                "customVoiceId" to customVoiceId,
                "language" to language,
                "messageCount" to 0L,
                "summary" to "",
                "confirmedFeelings" to emptyList<String>(),
                "agreedNextSteps" to emptyList<String>(),
                "lastMessageSnippet" to "",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            sessionRef.set(payload).await()
            Result.success(sessionId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, sessionRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateSessionSummary(
        userId: String,
        sessionId: String,
        summary: String,
        confirmedFeelings: List<String>,
        agreedNextSteps: List<String>
    ): Result<Unit> {
        val uid = userId.ifBlank { requireUserId() }
        if (uid == "guest_user" || db == null) {
            val index = guestSessions.indexOfFirst { it.id == sessionId }
            if (index != -1) {
                guestSessions[index] = guestSessions[index].copy(
                    summary = summary,
                    confirmedFeelings = confirmedFeelings,
                    agreedNextSteps = agreedNextSteps,
                    updatedAt = Timestamp.now()
                )
            }
            return Result.success(Unit)
        }

        val sessionRef = db.collection("users").document(uid).collection("sessions").document(sessionId)
        return try {
            sessionRef.update(
                mapOf(
                    "summary" to summary,
                    "confirmedFeelings" to confirmedFeelings,
                    "agreedNextSteps" to agreedNextSteps,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, sessionRef.path)
            Result.failure(e)
        }
    }

    suspend fun renameSession(userId: String, sessionId: String, newTitle: String): Result<Unit> {
        val uid = userId.ifBlank { requireUserId() }
        if (uid == "guest_user" || db == null) {
            val index = guestSessions.indexOfFirst { it.id == sessionId }
            if (index != -1) {
                guestSessions[index] = guestSessions[index].copy(title = newTitle, updatedAt = Timestamp.now())
            }
            return Result.success(Unit)
        }

        val sessionRef = db.collection("users").document(uid).collection("sessions").document(sessionId)
        return try {
            sessionRef.update(
                mapOf(
                    "title" to newTitle,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, sessionRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteSession(userId: String, sessionId: String): Result<Unit> {
        val uid = userId.ifBlank { requireUserId() }
        if (uid == "guest_user" || db == null) {
            guestSessions.removeAll { it.id == sessionId }
            guestMessages.remove(sessionId)
            return Result.success(Unit)
        }

        val sessionRef = db.collection("users").document(uid).collection("sessions").document(sessionId)
        return try {
            sessionRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, sessionRef.path)
            Result.failure(e)
        }
    }

    suspend fun clearAllSessions(userId: String): Result<Unit> {
        val uid = userId.ifBlank { requireUserId() }
        if (uid == "guest_user" || db == null) {
            guestSessions.clear()
            guestMessages.clear()
            return Result.success(Unit)
        }

        val sessionsRef = db.collection("users").document(uid).collection("sessions")
        return try {
            val snapshot = sessionsRef.get().await()
            for (doc in snapshot.documents) {
                doc.reference.delete().await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, sessionsRef.path)
            Result.failure(e)
        }
    }

    // --- Messages ---

    fun observeMessages(userId: String, sessionId: String): Flow<List<Message>> = flow {
        if (userId == "guest_user" || db == null) {
            emit(guestMessages[sessionId]?.toList() ?: emptyList())
            return@flow
        }
        val path = "users/$userId/sessions/$sessionId/messages"
        emitAll(
            db.collection("users").document(userId).collection("sessions").document(sessionId)
                .collection("messages")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(Message::class.java).sortedBy { it.createdAt?.seconds ?: 0L } }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    emit(guestMessages[sessionId]?.toList() ?: emptyList())
                }
        )
    }

    suspend fun addMessage(
        userId: String,
        sessionId: String,
        role: String,
        text: String,
        modality: String
    ): Result<String> {
        val uid = userId.ifBlank { requireUserId() }
        val messageId = UUID.randomUUID().toString()

        if (uid == "guest_user" || db == null) {
            val newMessage = Message(
                id = messageId,
                sessionId = sessionId,
                userId = "guest_user",
                role = role,
                text = text,
                modality = modality,
                createdAt = Timestamp.now()
            )
            guestMessages.getOrPut(sessionId) { mutableListOf() }.add(newMessage)
            // Update session snippet & count
            val sessionIndex = guestSessions.indexOfFirst { it.id == sessionId }
            if (sessionIndex != -1) {
                guestSessions[sessionIndex] = guestSessions[sessionIndex].copy(
                    messageCount = guestSessions[sessionIndex].messageCount + 1,
                    lastMessageSnippet = text.take(120),
                    updatedAt = Timestamp.now()
                )
            }
            return Result.success(messageId)
        }

        val sessionRef = db.collection("users").document(uid).collection("sessions").document(sessionId)
        val messageRef = sessionRef.collection("messages").document(messageId)

        return try {
            val payload = mapOf(
                "id" to messageId,
                "sessionId" to sessionId,
                "userId" to uid,
                "role" to role,
                "text" to text,
                "modality" to modality,
                "helpfulnessRating" to "",
                "createdAt" to FieldValue.serverTimestamp()
            )
            messageRef.set(payload).await()
            // Non-blocking update to session metadata
            sessionRef.update(
                mapOf(
                    "messageCount" to FieldValue.increment(1),
                    "lastMessageSnippet" to text.take(120),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            Result.success(messageId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, messageRef.path)
            Result.failure(e)
        }
    }

    suspend fun rateMessage(
        userId: String,
        sessionId: String,
        messageId: String,
        rating: String
    ): Result<Unit> {
        val uid = userId.ifBlank { requireUserId() }
        if (uid == "guest_user" || db == null) {
            val list = guestMessages[sessionId]
            val index = list?.indexOfFirst { it.id == messageId } ?: -1
            if (index != -1 && list != null) {
                list[index] = list[index].copy(helpfulnessRating = rating)
            }
            return Result.success(Unit)
        }

        val messageRef = db.collection("users").document(uid).collection("sessions").document(sessionId)
            .collection("messages").document(messageId)
        return try {
            messageRef.update("helpfulnessRating", rating).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, messageRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteUserData(userId: String): Result<Unit> {
        val uid = userId.ifBlank { requireUserId() }
        clearAllSessions(uid)
        if (uid != "guest_user" && db != null) {
            try {
                db.collection("users").document(uid).delete().await()
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.DELETE, "users/$uid")
            }
        }
        return Result.success(Unit)
    }
}
