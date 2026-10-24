package net.ubikapps.mediascreensaver.ui.compose

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackProgressBarTest {

    @Test
    fun formatTime_zeroMillis_returnsZeroTime() {
        val result = formatTime(0L)
        assertEquals("00:00", result)
    }

    @Test
    fun formatTime_underOneSecond_returnsZeroTime() {
        val result = formatTime(999L)
        assertEquals("00:00", result)
    }

    @Test
    fun formatTime_exactSeconds_returnsFormattedTime() {
        val result = formatTime(5000L)
        assertEquals("00:05", result)
    }

    @Test
    fun formatTime_minutesAndSeconds_returnsFormattedTime() {
        val result = formatTime(65000L)
        assertEquals("01:05", result)
    }

    @Test
    fun formatTime_exactMinutes_returnsFormattedTime() {
        val result = formatTime(180000L) // 3 minutes
        assertEquals("03:00", result)
    }

    @Test
    fun formatTime_negativeMillis_returnsZeroTime() {
        val result = formatTime(-5000L)
        assertEquals("00:00", result)
    }

    @Test
    fun formatTime_largeDuration_returnsCorrectMinutesAndSeconds() {
        val result = formatTime(3661000L) // 61 minutes and 1 second
        assertEquals("61:01", result)
    }
}
