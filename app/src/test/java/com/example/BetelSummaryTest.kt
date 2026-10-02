package com.example

import com.example.data.model.BetelLog
import com.example.ui.summarizeBetel
import com.example.ui.betelTrendItems
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class BetelSummaryTest {
    @Test
    fun `consumption excludes shared out but spending includes it`() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 2, 14, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val logs = listOf(
            BetelLog(productId = 1, productName = "A", quantity = 2, logType = "SELF", cost = 4.0, timestamp = now - 1000),
            BetelLog(productId = 1, productName = "A", quantity = 3, logType = "SHARED_OUT", cost = 6.0, timestamp = now - 2000),
            BetelLog(productId = 1, productName = "A", quantity = 1, logType = "RECEIVED_IN", cost = 0.0, timestamp = now - 3000),
            BetelLog(productId = 1, productName = "A", quantity = 5, logType = "SELF", cost = 10.0, timestamp = now - 24 * 60 * 60 * 1000L)
        )
        val result = summarizeBetel(logs, now)
        assertEquals(3, result.todayConsumed)
        assertEquals(3, result.todayShared)
        assertEquals(10.0, result.todayCost, 0.001)
        assertEquals(20.0, result.monthCost, 0.001)
        assertEquals(3, result.weekCounts.last())
    }

    @Test
    fun `trend separates consumed shared and received across dates`() {
        val day = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 2, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = day.timeInMillis
        val nextDay = (day.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }.timeInMillis
        val logs = listOf(
            BetelLog(productId = 1, productName = "B", quantity = 2, logType = "SELF", cost = 4.0, timestamp = start),
            BetelLog(productId = 1, productName = "B", quantity = 3, logType = "SHARED_OUT", cost = 6.0, timestamp = start + 2),
            BetelLog(productId = 1, productName = "B", quantity = 1, logType = "RECEIVED_IN", cost = 0.0, timestamp = start + 3),
            BetelLog(productId = 1, productName = "B", quantity = 5, logType = "SELF", cost = 10.0, timestamp = nextDay),
        )
        val result = betelTrendItems(logs, start, nextDay)
        assertEquals(2, result.size)
        assertEquals(2, result[0].selfCount)
        assertEquals(3, result[0].sharedCount)
        assertEquals(1, result[0].receivedCount)
        assertEquals(4.0, result[0].selfCost, 0.001)
        assertEquals(6.0, result[0].sharedCost, 0.001)
        assertEquals(5, result[1].selfCount)
        assertEquals(0, result[1].sharedCount)
    }

    @Test
    fun `long custom range retains latest dates`() {
        val day = Calendar.getInstance().apply {
            set(2024, Calendar.JANUARY, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = day.timeInMillis
        day.add(Calendar.YEAR, 3)
        val end = day.timeInMillis
        val result = betelTrendItems(emptyList(), start, end)
        assertEquals(366, result.size)
        assertEquals(end, result.last().timestamp)
        assertEquals(emptyList<com.example.ui.TrendDataItem>(), betelTrendItems(emptyList(), end, start))
    }
}
