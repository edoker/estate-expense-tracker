package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class ProjectMember(
    val name: String = "",
    val email: String = "",
    val role: String = "member" // "admin" or "member"
)

@IgnoreExtraProperties
data class Project(
    @DocumentId val id: String = "",
    val name: String = "",
    val adminUid: String = "",
    val memberUids: List<String> = emptyList(),
    val members: Map<String, ProjectMember> = emptyMap(),
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun isAdmin(uid: String): Boolean = adminUid == uid || members[uid]?.role == "admin"
}
