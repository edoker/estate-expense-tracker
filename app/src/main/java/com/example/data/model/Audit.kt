package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Audit(
    @DocumentId val id: String = "",
    val action: String = "", // "created" | "updated" | "deleted"
    val entityType: String = "expense",
    val entityId: String = "",
    val actorUid: String = "",
    val actorName: String = "",
    @ServerTimestamp val timestamp: Timestamp? = null,
    val summary: String = "",
    val changes: Map<String, Map<String, Any?>> = emptyMap(),
    val snapshot: Map<String, Any?> = emptyMap()
)
