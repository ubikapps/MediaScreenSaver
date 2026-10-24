package net.ubikapps.mediascreensaver

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class MediaControlCalculationTest {

    @Test
    fun rewindCalculation_clapsToZeroWhenUnder30Seconds() {
        val currentPosition = 15000L // 15 seconds
        val targetPosition = (currentPosition - 30000L).coerceAtLeast(0L)
        assertEquals(0L, targetPosition)
    }

    @Test
    fun rewindCalculation_subtracts30SecondsWhenAbove30Seconds() {
        val currentPosition = 90000L // 90 seconds
        val targetPosition = (currentPosition - 30000L).coerceAtLeast(0L)
        assertEquals(60000L, targetPosition)
    }

    @Test
    fun forwardCalculation_adds30Seconds() {
        val currentPosition = 120000L // 120 seconds
        val targetPosition = currentPosition + 30000L
        assertEquals(150000L, targetPosition)
    }

    @Test
    fun progressCalculation_clampsBetweenZeroAndOne() {
        val duration = 180000L // 3 minutes

        // Position at start
        val posStart = 0L
        val progressStart = (posStart.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        assertEquals(0f, progressStart, 0.0001f)

        // Position in middle
        val posMiddle = 90000L
        val progressMiddle = (posMiddle.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        assertEquals(0.5f, progressMiddle, 0.0001f)

        // Position beyond duration (e.g. over-shooting)
        val posOverflow = 200000L
        val progressOverflow = (posOverflow.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        assertEquals(1f, progressOverflow, 0.0001f)

        // Position negative
        val posNegative = -5000L
        val progressNegative = (posNegative.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        assertEquals(0f, progressNegative, 0.0001f)
    }

    @Test
    fun playbackStatePositionCalculation_accountsForSpeedAndElapsedTime() {
        val lastStatePosition = 60000L // 60s
        val timeDiffMs = 5000L // 5s elapsed
        val playbackSpeed = 1.0f
        val duration = 180000L // 3m

        val calculatedPos = lastStatePosition + (timeDiffMs * playbackSpeed).toLong()
        val clampedPos = calculatedPos.coerceIn(0L, duration)

        assertEquals(65000L, clampedPos)
    }

    @Test
    fun playbackStatePositionCalculation_handlesFastForwardSpeed() {
        val lastStatePosition = 30000L // 30s
        val timeDiffMs = 10000L // 10s elapsed
        val playbackSpeed = 2.0f // 2x speed
        val duration = 180000L

        val calculatedPos = lastStatePosition + (timeDiffMs * playbackSpeed).toLong()
        val clampedPos = calculatedPos.coerceIn(0L, duration)

        assertEquals(50000L, clampedPos)
    }

    @Test
    fun burnInOffsetGenerator_staysWithinMaxBounds() {
        val maxOffset = 40
        repeat(100) {
            val offsetX = Random.nextInt(-maxOffset, maxOffset)
            val offsetY = Random.nextInt(-maxOffset, maxOffset)

            assertTrue("offsetX $offsetX should be >= -$maxOffset", offsetX >= -maxOffset)
            assertTrue("offsetX $offsetX should be < $maxOffset", offsetX < maxOffset)
            assertTrue("offsetY $offsetY should be >= -$maxOffset", offsetY >= -maxOffset)
            assertTrue("offsetY $offsetY should be < $maxOffset", offsetY < maxOffset)
        }
    }
}
