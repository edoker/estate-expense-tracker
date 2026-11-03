package com.example.data.util

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Timestamp?.formatDate(): String {
    if (this == null) return "Just now"
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return sdf.format(this.toDate())
}

fun Date?.formatDate(): String {
    if (this == null) return "Just now"
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return sdf.format(this)
}

fun Timestamp?.formatDateTime(): String {
    if (this == null) return "Just now"
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    return sdf.format(this.toDate())
}

fun Date?.formatDateTime(): String {
    if (this == null) return "Just now"
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    return sdf.format(this)
}
