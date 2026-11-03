package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Invite(
    @DocumentId val id: String = "",
    val projectId: String = "",
    val projectName: String = "",
    val invitedEmail: String = "",
    val invitedByUid: String = "",
    val invitedByName: String = "",
    val status: String = "pending", // "pending", "accepted", "declined"
    @ServerTimestamp val createdAt: Timestamp? = null
)
