package net.ubikapps.mediascreensaver.ui.compose

import android.media.session.PlaybackState
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun PlaybackProgressBar(
    playbackState: PlaybackState?,
    duration: Long,
    modifier: Modifier = Modifier
) {
    PlaybackProgressBar(
        playbackState = playbackState,
        duration = duration,
        modifier = modifier,
        onSeek = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackProgressBar(
    playbackState: PlaybackState?,
    duration: Long,
    modifier: Modifier = Modifier,
    onSeek: (Long) -> Unit
) {
    var isDragging by remember { mutableStateOf(false) }
    var lastSeekTime by remember { mutableLongStateOf(0L) }
    var lastSeekPlaybackState by remember { mutableStateOf<PlaybackState?>(null) }

    val initialPos = remember {
        calculateCurrentPlaybackPosition(playbackState).coerceIn(0L, if (duration > 0) duration else Long.MAX_VALUE)
    }
    var currentPosition by remember { mutableLongStateOf(initialPos) }
    var progress by remember {
        mutableFloatStateOf(if (duration > 0) (initialPos.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f)
    }

    LaunchedEffect(playbackState, duration) {
        while (true) {
            val timeSinceSeek = SystemClock.elapsedRealtime() - lastSeekTime
            val isAwaitingSeekState = (playbackState === lastSeekPlaybackState) && (timeSinceSeek < 1500L)
            if (!isDragging && !isAwaitingSeekState) {
                val calculatedPos = calculateCurrentPlaybackPosition(playbackState)
                currentPosition = calculatedPos.coerceIn(0L, if (duration > 0) duration else Long.MAX_VALUE)
                progress = if (duration > 0) (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
            }
            delay(1000)
        }
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = formatTime(currentPosition),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White
        )
        Slider(
            value = progress,
            onValueChange = { newProgress ->
                isDragging = true
                progress = newProgress
                currentPosition = if (duration > 0) {
                    (newProgress * duration).toLong().coerceIn(0L, duration)
                } else {
                    0L
                }
            },
            onValueChangeFinished = {
                isDragging = false
                lastSeekTime = SystemClock.elapsedRealtime()
                lastSeekPlaybackState = playbackState
                onSeek(currentPosition)
            },
            enabled = duration > 0,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.DarkGray
            ),
            thumb = {
                Box(
                    modifier = Modifier
                        .size(15.dp)
                        .background(Color.White, CircleShape)
                )
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    modifier = Modifier.height(4.dp),
                    colors = SliderDefaults.colors(
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.DarkGray
                    ),
                    drawStopIndicator = null,
                    thumbTrackGapSize = 0.dp,
                    trackInsideCornerSize = 0.dp
                )
            }
        )
        Text(
            text = formatTime(duration),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun PlaybackProgressBarPreview() {
    val state = PlaybackState.Builder()
        .setState(PlaybackState.STATE_PAUSED, 90000L, 1.0f)
        .build()
    PlaybackProgressBar(
        playbackState = state,
        duration = 180000L,
        modifier = Modifier.fillMaxWidth().padding(16.dp)
    )
}


fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

fun calculateCurrentPlaybackPosition(
    playbackState: PlaybackState?,
    currentSystemTime: Long = SystemClock.elapsedRealtime()
): Long {
    if (playbackState == null) return 0L
    return calculatePlaybackPosition(
        state = playbackState.state,
        lastPosition = playbackState.position,
        lastPositionUpdateTime = playbackState.lastPositionUpdateTime,
        playbackSpeed = playbackState.playbackSpeed,
        currentSystemTime = currentSystemTime
    )
}

fun calculatePlaybackPosition(
    state: Int,
    lastPosition: Long,
    lastPositionUpdateTime: Long,
    playbackSpeed: Float,
    currentSystemTime: Long
): Long {
    return if (state == PlaybackState.STATE_PLAYING && lastPositionUpdateTime > 0L) {
        val timeDiff = (currentSystemTime - lastPositionUpdateTime).coerceAtLeast(0L)
        val calculatedPos = lastPosition + (timeDiff * playbackSpeed).toLong()
        calculatedPos.coerceAtLeast(0L)
    } else {
        lastPosition.coerceAtLeast(0L)
    }
}