package net.ubikapps.mediascreensaver.ui.compose

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.SystemClock
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import net.ubikapps.mediascreensaver.MediaNotificationListenerService
import net.ubikapps.mediascreensaver.daydream.TAG
import net.ubikapps.mediascreensaver.ui.theme.MediaScreenSaverTheme
import kotlin.random.Random

@Composable
fun ScreenSaverContent(modifier: Modifier = Modifier) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current

    // --- State Hoisting ---

    // Dummy data for preview
    val dummyBitmap = remember(isPreview) {
        if (isPreview) {
            Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888).apply {
                eraseColor(android.graphics.Color.GRAY)
            }
        } else null
    }

    var mediaMetadata by remember(isPreview) { mutableStateOf(
        if (isPreview) {
            MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, "Preview Title")
                .putString(MediaMetadata.METADATA_KEY_ARTIST, "Preview Artist")
                .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, dummyBitmap)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, 180000L)
                .build()
        } else null
    ) }

    var playbackState by remember(isPreview) { mutableStateOf(
        if (isPreview) {
            PlaybackState.Builder()
                .setState(PlaybackState.STATE_PLAYING, 60000L, 1.0f, SystemClock.elapsedRealtime())
                .build()
        } else null
    ) }

    var currentController by remember { mutableStateOf<MediaController?>(null) }

    // Media Session Listener
    DisposableEffect(context, isPreview) {
        if (isPreview) return@DisposableEffect onDispose {}

        val mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        val componentName = ComponentName(context, MediaNotificationListenerService::class.java)

        val callback = object : MediaController.Callback() {
            override fun onPlaybackStateChanged(state: PlaybackState?) {
                playbackState = state
            }

            override fun onMetadataChanged(metadata: MediaMetadata?) {
                mediaMetadata = metadata
            }
        }

        fun updateController(controllers: List<MediaController>) {
            val active = controllers.firstOrNull {
                it.playbackState?.state == PlaybackState.STATE_PLAYING
            } ?: controllers.firstOrNull()

            if (currentController != active) {
                currentController?.unregisterCallback(callback)
                currentController = active
                currentController?.registerCallback(callback)
                playbackState = currentController?.playbackState
                mediaMetadata = currentController?.metadata
            }
        }

        val sessionListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            updateController(controllers ?: emptyList())
        }

        try {
            val initialSessions = mediaSessionManager.getActiveSessions(componentName)
            updateController(initialSessions)
            mediaSessionManager.addOnActiveSessionsChangedListener(sessionListener, componentName)
        } catch (e: SecurityException) {
            Log.e(TAG, "permision not granted: $e")
        }

        onDispose {
            try {
                mediaSessionManager.removeOnActiveSessionsChangedListener(sessionListener)
                currentController?.unregisterCallback(callback)
            } catch (e: Exception) { }
        }
    }

    // Control Actions
    val transportControls = currentController?.transportControls
    val onPlayPause = {
        if (playbackState?.state == PlaybackState.STATE_PLAYING)
            transportControls?.pause()
        else
            transportControls?.play()
    }
    val onSkipNext = { transportControls?.skipToNext() }
    val onSkipPrevious = { transportControls?.skipToPrevious() }
    val onForward30 = {
        val pos = playbackState?.position ?: 0L
        transportControls?.seekTo(pos + 30000)
    }
    val onRewind30 = {
        val pos = playbackState?.position ?: 0L
        transportControls?.seekTo((pos - 30000).coerceAtLeast(0L))
    }

    // Burn-in protection: Shift content every minute
    var burnInOffset by remember { mutableStateOf(IntOffset.Zero) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60000) // 1 minute
            val maxOffset = 40
            burnInOffset = IntOffset(
                Random.nextInt(-maxOffset, maxOffset),
                Random.nextInt(-maxOffset, maxOffset)
            )
        }
    }

    // --- UI Layout ---

    Box(
        modifier = modifier
            .fillMaxSize()
            .offset { burnInOffset },
        contentAlignment = Alignment.Center
    ) {
        // Battery Status at the top
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            BatteryStatus()
        }

        if (mediaMetadata == null) {
            // Standby Mode: Just the Clock
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.displayCutout)
            ) {
                DigitalClock()
            }
        } else {
            // Media Mode
            val title = mediaMetadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown Title"
            val artist = mediaMetadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Unknown Artist"
            val albumArt = mediaMetadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: mediaMetadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            val duration = mediaMetadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L

            if (isLandscape) {
                // Landscape Layout: Art Left (2x size), Details Right (Time included)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.displayCutout)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (albumArt != null) {
                        val matrix = ColorMatrix()
                        matrix.setToSaturation(0f)

                        Image(
                            bitmap = albumArt.asImageBitmap(),
                            contentDescription = "Album Art",
                            modifier = Modifier.size(240.dp),
                            colorFilter = ColorFilter.colorMatrix(matrix)
                        )
                        Spacer(modifier = Modifier.width(32.dp))
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        DigitalClock(fontSize = 48.sp)

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White,
                                textAlign = TextAlign.Start
                            )
                            Text(
                                text = artist,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.LightGray,
                                textAlign = TextAlign.Start
                            )
                        }

                        MediaControls(
                            playbackState = playbackState,
                            onPlayPause = { onPlayPause() },
                            onSkipNext = { onSkipNext() },
                            onSkipPrevious = { onSkipPrevious() },
                            onForward30 = { onForward30() },
                            onRewind30 = { onRewind30() },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )

                        if (duration > 0) {
                            PlaybackProgressBar(
                                playbackState,
                                duration,
                                Modifier.fillMaxWidth(0.9f).align(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }
            } else {
                // Portrait Layout: Stacked
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.displayCutout)
                        .padding(32.dp)
                ) {
                    DigitalClock(fontSize = 80.sp)

                    Spacer(modifier = Modifier.height(48.dp))

                    if (albumArt != null) {
                        val matrix = ColorMatrix()
                        matrix.setToSaturation(0f)

                        Image(
                            bitmap = albumArt.asImageBitmap(),
                            contentDescription = "Album Art",
                            modifier = Modifier
                                .size(200.dp)
                                .padding(bottom = 16.dp),
                            colorFilter = ColorFilter.colorMatrix(matrix)
                        )
                    }

                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = artist,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (duration > 0) {
                        PlaybackProgressBar(playbackState, duration, Modifier.fillMaxWidth(0.6f))
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    MediaControls(
                        playbackState = playbackState,
                        onPlayPause = { onPlayPause() },
                        onSkipNext = { onSkipNext() },
                        onSkipPrevious = { onSkipPrevious() },
                        onForward30 = { onForward30() },
                        onRewind30 = { onRewind30() }
                    )
                }
            }
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Preview(name = "Portrait", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun ScreenSaverPortraitPreview() {
    MediaScreenSaverTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Black
        ) {  _ ->
            ScreenSaverContent()
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Preview(name = "Landscape", showBackground = true, device = "spec:width=720dp,height=360dp,orientation=landscape")
@Composable
fun ScreenSaverLandscapePreview() {
    MediaScreenSaverTheme {
        Scaffold(
            Modifier.fillMaxSize(), containerColor = Color.Black
        ) { _ ->
            ScreenSaverContent()
        }
    }
}