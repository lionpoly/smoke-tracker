package com.example

import com.example.ui.TrackerDestination
import com.example.ui.TrackingSubject
import com.example.ui.destinationFor
import org.junit.Assert.assertEquals
import org.junit.Test

class TrackerDestinationTest {
    @Test fun `switching subject changes every tab without mixing pages`() {
        val tobacco = listOf(
            TrackerDestination.TOBACCO_HOME, TrackerDestination.TOBACCO_ANALYSIS,
            TrackerDestination.TOBACCO_BRANDS, TrackerDestination.TOBACCO_SETTINGS
        )
        val betel = listOf(
            TrackerDestination.BETEL_HOME, TrackerDestination.BETEL_ANALYSIS,
            TrackerDestination.BETEL_BRANDS, TrackerDestination.BETEL_SETTINGS
        )
        assertEquals(tobacco, listOf(0, 1, 3, 4).map { destinationFor(TrackingSubject.TOBACCO, it) })
        assertEquals(betel, listOf(0, 1, 3, 4).map { destinationFor(TrackingSubject.BETEL, it) })
    }
}
