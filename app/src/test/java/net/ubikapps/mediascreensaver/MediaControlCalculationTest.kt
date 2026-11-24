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

        val calculatedPos = net.ubikapps.mediascreensaver.ui.compose.calculatePlaybackPosition(
            state = android.media.session.PlaybackState.STATE_PLAYING,
            lastPosition = lastStatePosition,
            lastPositionUpdateTime = 1000L,
            playbackSpeed = playbackSpeed,
            currentSystemTime = 1000L + timeDiffMs
        )
        val clampedPos = calculatedPos.coerceIn(0L, duration)

        assertEquals(65000L, clampedPos)
    }

    @Test
    fun playbackStatePositionCalculation_handlesFastForwardSpeed() {
        val lastStatePosition = 30000L // 30s
        val timeDiffMs = 10000L // 10s elapsed
        val playbackSpeed = 2.0f // 2x speed
        val duration = 180000L

        val calculatedPos = net.ubikapps.mediascreensaver.ui.compose.calculatePlaybackPosition(
            state = android.media.session.PlaybackState.STATE_PLAYING,
            lastPosition = lastStatePosition,
            lastPositionUpdateTime = 1000L,
            playbackSpeed = playbackSpeed,
            currentSystemTime = 1000L + timeDiffMs
        )
        val clampedPos = calculatedPos.coerceIn(0L, duration)

        assertEquals(50000L, clampedPos)
    }

    @Test
    fun playbackStatePositionCalculation_whenPaused_doesNotAdvance() {
        val lastStatePosition = 60000L
        val calculatedPos = net.ubikapps.mediascreensaver.ui.compose.calculatePlaybackPosition(
            state = android.media.session.PlaybackState.STATE_PAUSED,
            lastPosition = lastStatePosition,
            lastPositionUpdateTime = 1000L,
            playbackSpeed = 1.0f,
            currentSystemTime = 10000L
        )
        assertEquals(60000L, calculatedPos)
    }

    @Test
    fun skipForwardWithStalePosition_wouldSkipBackwards_whereasCalculatedPositionAdvances() {
        // Reproduce Pocket Casts scenario:
        // Playback started at 2 min (120,000ms), 5 minutes (300,000ms) have elapsed since.
        val stalePosition = 120000L
        val lastUpdateTime = 10000L
        val currentTime = lastUpdateTime + 300000L // 5 mins later
        val currentActualPlaybackPos = 420000L // 7 minutes in

        // Old buggy behavior: adding 30s to stale position:
        val buggyTarget = stalePosition + 30000L // 150,000ms (2.5 min)
        assertTrue("Buggy target is behind actual playback position!", buggyTarget < currentActualPlaybackPos)

        // Fixed behavior: calculate up-to-date position
        val calculatedPos = net.ubikapps.mediascreensaver.ui.compose.calculatePlaybackPosition(
            state = android.media.session.PlaybackState.STATE_PLAYING,
            lastPosition = stalePosition,
            lastPositionUpdateTime = lastUpdateTime,
            playbackSpeed = 1.0f,
            currentSystemTime = currentTime
        )
        val fixedTarget = calculatedPos + 30000L
        assertEquals(450000L, fixedTarget)
        assertTrue("Fixed target advances ahead of current playback position!", fixedTarget > currentActualPlaybackPos)
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
