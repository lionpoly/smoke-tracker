package com.example

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.data.model.BetelLog
import com.example.ui.rememberBetelHomeTime
import com.example.ui.summarizeBetel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BetelHomeRefreshTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun `new log refreshes summary without waiting for minute ticker`() {
        val initialTime = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 2, 14, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        var currentTime = initialTime
        var clockTick by mutableLongStateOf(initialTime)
        var logs by mutableStateOf(emptyList<BetelLog>())
        composeTestRule.setContent {
            val snapshotTime = rememberBetelHomeTime(logs, clockTick) { currentTime }
            Text("Consumed: ${summarizeBetel(logs, snapshotTime).todayConsumed}")
        }
        composeTestRule.onNodeWithText("Consumed: 0").assertExists()

        composeTestRule.runOnIdle {
            currentTime += 1_000
            logs = listOf(BetelLog(productId = 1, productName = "B", quantity = 1,
                logType = "SELF", cost = 2.0, timestamp = currentTime))
        }
        composeTestRule.onNodeWithText("Consumed: 1").assertExists()

        composeTestRule.runOnIdle {
            currentTime += 1_000
            logs = emptyList()
        }
        composeTestRule.onNodeWithText("Consumed: 0").assertExists()
    }
}
