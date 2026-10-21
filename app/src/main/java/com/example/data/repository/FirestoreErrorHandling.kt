package com.example.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestoreException

enum class OperationType {
    GET,
    LIST,
    CREATE,
    UPDATE,
    DELETE
}

fun handleFirestoreError(exception: Exception, operationType: OperationType, path: String) {
    val errorCode = if (exception is FirebaseFirestoreException) exception.code.name else "UNKNOWN"
    val json = """
        {
          "status": "error",
          "operation": "${operationType.name}",
          "target": "$path",
          "code": "$errorCode",
          "message": "${exception.localizedMessage ?: "Unknown Firestore error"}"
        }
    """.trimIndent()
    Log.e("FirestoreError", json, exception)
}
