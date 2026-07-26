package net.ubikapps.mediascreensaver.ui.compose

import android.media.session.PlaybackState
import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun PlaybackProgressBar(playbackState: PlaybackState?, duration: Long, modifier: Modifier = Modifier) {
    var currentPosition by remember { androidx.compose.runtime.mutableLongStateOf(0L) }
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(playbackState, duration) {
        while (true) {
            if (playbackState != null && playbackState.state == PlaybackState.STATE_PLAYING) {
                val timeDiff = SystemClock.elapsedRealtime() - playbackState.lastPositionUpdateTime
                val calculatedPos = playbackState.position + (timeDiff * playbackState.playbackSpeed).toLong()
                currentPosition = calculatedPos.coerceIn(0L, duration)
                progress = (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
            } else if (playbackState != null) {
                currentPosition = playbackState.position.coerceIn(0L, duration)
                progress = (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
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
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.weight(1f),
            color = Color.White,
            trackColor = Color.DarkGray,
        )
        Text(
            text = formatTime(duration),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White
        )
    }
}

fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}