package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.BillCategory
import com.example.data.model.PaymentStatus
import com.example.data.model.ReportStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("MediVault", appName)
  }

  @Test
  fun `verify bill and report models status`() {
    assertEquals("Consultation", BillCategory.CONSULTATION.displayName)
    assertEquals("Paid in Full", PaymentStatus.PAID.label)
    assertEquals("Normal / Healthy", ReportStatus.NORMAL.label)
  }
}
