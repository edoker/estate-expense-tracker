package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("SpendSync", appName)
  }

  @Test
  fun `delete project name match is case insensitive`() {
    val projectName = "MGM resort"
    val typedExact = "MGM resort"
    val typedUpper = "MGM RESORT"
    val typedLower = "mgm resort"
    val typedWrong = "Other project"

    org.junit.Assert.assertTrue(typedExact.trim().equals(projectName.trim(), ignoreCase = true))
    org.junit.Assert.assertTrue(typedUpper.trim().equals(projectName.trim(), ignoreCase = true))
    org.junit.Assert.assertTrue(typedLower.trim().equals(projectName.trim(), ignoreCase = true))
    org.junit.Assert.assertFalse(typedWrong.trim().equals(projectName.trim(), ignoreCase = true))
  }
}
