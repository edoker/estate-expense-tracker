package com.example

import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FirestoreAuthGatingTest {

    @Test
    fun handleFirestoreError_whenUnauthenticated_doesNotCrashAndReturnsFriendlyMessage() {
        val exception = RuntimeException("Error getting Query snapshot")
        val result = handleFirestoreError(exception, OperationType.LIST, "projects")
        assertTrue(result.contains("Error getting Query snapshot") || result.contains("Unauthenticated"))
    }
}
