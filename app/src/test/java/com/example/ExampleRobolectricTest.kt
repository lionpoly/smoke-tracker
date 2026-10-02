package com.example

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    fun localizedName(language: String): String {
      val config = Configuration(context.resources.configuration)
      config.setLocale(Locale.forLanguageTag(language))
      return context.createConfigurationContext(config).getString(R.string.app_name)
    }
    assertEquals("榔烟记", localizedName("zh"))
    assertEquals("Betel & Smoke", localizedName("en"))
  }
}
