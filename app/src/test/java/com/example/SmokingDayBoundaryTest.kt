package com.example

import com.example.ui.millisUntilNextDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class SmokingDayBoundaryTest {
    @Test fun `ticks at next local midnight`() {
        val almostMidnight = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 2, 23, 59, 59)
            set(Calendar.MILLISECOND, 500)
        }
        assertEquals(500L, millisUntilNextDay(almostMidnight.timeInMillis))
    }

    @Test fun `ticks once per day after midnight`() {
        val midnight = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 2, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        assertTrue(millisUntilNextDay(midnight.timeInMillis) > 0)
        assertEquals(midnight.timeInMillis + millisUntilNextDay(midnight.timeInMillis),
            Calendar.getInstance().apply {
                timeInMillis = midnight.timeInMillis
                add(Calendar.DAY_OF_YEAR, 1)
            }.timeInMillis)
    }
}
