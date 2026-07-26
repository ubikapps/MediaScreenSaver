package net.ubikapps.mediascreensaver.ui.compose

import android.graphics.drawable.AnimatedVectorDrawable
import android.media.session.PlaybackState
import android.view.ViewGroup
import android.widget.ImageView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import net.ubikapps.mediascreensaver.R


@Composable
fun PlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var previousIsPlaying by remember { mutableStateOf<Boolean?>(null) }

    AndroidView(
        modifier = modifier.clickable(onClick = onClick),
        factory = { context ->
            ImageView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        update = { view ->
            if (previousIsPlaying == null) {
                // Initial State
                view.setImageResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
            } else if (previousIsPlaying != isPlaying) {
                // State Changed - Animate
                val res = if (isPlaying) R.drawable.avd_play_to_pause else R.drawable.avd_pause_to_play
                view.setImageResource(res)
                (view.drawable as? AnimatedVectorDrawable)?.start()
            }
            previousIsPlaying = isPlaying
        }
    )
}

@Composable
fun MediaControls(
    playbackState: PlaybackState?,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onForward30: () -> Unit,
    onRewind30: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        IconButton(onClick = onRewind30) {
            Canvas(modifier = Modifier.size(24.dp)) {
                val color = Color.White
                val path = Path().apply {
                    moveTo(size.width, 0f)
                    lineTo(size.width / 2, size.height / 2)
                    lineTo(size.width, size.height)
                    close()

                    moveTo(size.width / 2, 0f)
                    lineTo(0f, size.height / 2)
                    lineTo(size.width / 2, size.height)
                    close()
                }
                drawPath(path, color)
            }
        }

        IconButton(onClick = onSkipPrevious) {
            Canvas(modifier = Modifier.size(24.dp)) {
                val color = Color.White
                drawRect(color, topLeft = Offset(0f, 0f), size = Size(size.width / 5, size.height))
                val path = Path().apply {
                    moveTo(size.width, 0f)
                    lineTo(size.width / 4, size.height / 2)
                    lineTo(size.width, size.height)
                    close()
                }
                drawPath(path, color)
            }
        }

        PlayPauseButton(
            isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING,
            onClick = onPlayPause,
            modifier = Modifier.size(48.dp)
        )

        IconButton(onClick = onSkipNext) {
            Canvas(modifier = Modifier.size(24.dp)) {
                val color = Color.White
                val path = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width * 0.75f, size.height / 2)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(path, color)
                drawRect(color, topLeft = Offset(size.width * 0.8f, 0f), size = Size(size.width / 5, size.height))
            }
        }

        IconButton(onClick = onForward30) {
            Canvas(modifier = Modifier.size(24.dp)) {
                val color = Color.White
                val path = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width / 2, size.height / 2)
                    lineTo(0f, size.height)
                    close()

                    moveTo(size.width / 2, 0f)
                    lineTo(size.width, size.height / 2)
                    lineTo(size.width / 2, size.height)
                    close()
                }
                drawPath(path, color)
            }
        }
    }
}