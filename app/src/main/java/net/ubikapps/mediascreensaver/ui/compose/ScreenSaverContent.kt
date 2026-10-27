package net.ubikapps.mediascreensaver.ui.compose

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.SystemClock
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ubikapps.mediascreensaver.MediaNotificationListenerService
import net.ubikapps.mediascreensaver.R
import net.ubikapps.mediascreensaver.daydream.TAG
import net.ubikapps.mediascreensaver.ui.theme.MediaScreenSaverTheme
import kotlin.random.Random

@Composable
fun ScreenSaverContent(
    modifier: Modifier = Modifier,
    showMedia: Boolean = true
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current

    // --- State Hoisting ---


    var mediaMetadata by remember(isPreview, showMedia) { mutableStateOf(
        if (isPreview && showMedia) {
            val builder = MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, "Song Title")
                .putString(MediaMetadata.METADATA_KEY_ARTIST, "Artist Name")
                .putLong(MediaMetadata.METADATA_KEY_DURATION, 180000L)
            builder.build()
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
            }
            playbackState = active?.playbackState
            mediaMetadata = active?.metadata
        }

        val sessionListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            updateController(controllers ?: emptyList())
        }

        var isListenerAdded = false

        val scope = CoroutineScope(Dispatchers.Main)
        val initJob = scope.launch {
            var attempts = 0
            while (isActive && !isListenerAdded && attempts < 10) {
                try {
                    MediaNotificationListenerService.ensureRebound(context)
                    val initialSessions = mediaSessionManager.getActiveSessions(componentName)
                    updateController(initialSessions)
                    mediaSessionManager.addOnActiveSessionsChangedListener(sessionListener, componentName)
                    isListenerAdded = true
                } catch (e: SecurityException) {
                    Log.w(TAG, "Notification listener not connected yet, retrying... (attempt ${attempts + 1}): ${e.message}")
                    attempts++
                    delay(500)
                } catch (e: Throwable) {
                    Log.e(TAG, "Failed to initialize active media sessions", e)
                    break
                }
            }
        }

        onDispose {
            initJob.cancel()
            try {
                if (isListenerAdded) {
                    mediaSessionManager.removeOnActiveSessionsChangedListener(sessionListener)
                }
                currentController?.unregisterCallback(callback)
            } catch (_: Exception) { }
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .offset { burnInOffset },
        contentAlignment = Alignment.Center
    ) {
        if (mediaMetadata == null) {
            // Standby Mode: Just the Clock
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.displayCutout)
            ) {
                StatusBar()
                AutosizeDigitalClock()
                Text(
                    text = stringResource(R.string.no_media_session),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.LightGray,
                    textAlign = TextAlign.Start
                )
            }
        } else {
            // Media Mode
            val title = mediaMetadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown Title"
            val artist = mediaMetadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Unknown Artist"
            val directAlbumArt = mediaMetadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: mediaMetadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            val artUriString = remember(mediaMetadata) {
                mediaMetadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
                    ?: mediaMetadata?.getString(MediaMetadata.METADATA_KEY_ART_URI)
            }

            var uriAlbumArt by remember { mutableStateOf<Bitmap?>(null) }

            LaunchedEffect(artUriString, directAlbumArt) {
                uriAlbumArt = if (directAlbumArt != null || artUriString.isNullOrBlank()) {
                    null
                } else {
                    loadBitmapFromUri(context, artUriString)
                }
            }

            val albumArt = directAlbumArt ?: uriAlbumArt
            val duration = mediaMetadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L

            if (isLandscape) {
                // Landscape Layout: Art Left (dynamic height), Details Right (Time included)
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.displayCutout)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    AlbumArt(
                        albumArt = albumArt,
                        modifier = Modifier
                            .fillMaxHeight(0.75f)
                            .aspectRatio(1f)
                    )
                    Spacer(modifier = Modifier.width(16.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth().padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly) {
                            StatusBar()
                            DigitalClock(fontSize = 48.sp)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
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
                        }

                        MediaControls(
                            playbackState = playbackState,
                            onPlayPause = { onPlayPause() },
                            onSkipNext = { onSkipNext() },
                            onSkipPrevious = { onSkipPrevious() },
                            onForward30 = { onForward30() },
                            onRewind30 = { onRewind30() },
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (duration > 0) {
                            PlaybackProgressBar(
                                playbackState,
                                duration,
                                Modifier.fillMaxWidth(0.95f).align(Alignment.CenterHorizontally)
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
                        .padding(16.dp)
                ) {
                    StatusBar()
                    DigitalClock(fontSize = 80.sp)

                    Spacer(modifier = Modifier.height(24.dp))

                    AlbumArt(albumArt, modifier = Modifier
                        .aspectRatio(1f)
                        .fillMaxWidth()
                        .padding(bottom = 16.dp))

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

                    MediaControls(
                        playbackState = playbackState,
                        onPlayPause = { onPlayPause() },
                        onSkipNext = { onSkipNext() },
                        onSkipPrevious = { onSkipPrevious() },
                        onForward30 = { onForward30() },
                        onRewind30 = { onRewind30() },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    if (duration > 0) {
                        PlaybackProgressBar(playbackState, duration, Modifier.fillMaxWidth(0.95f))
                    }

                }
            }
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Preview(name = "Portrait", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
fun ScreenSaverPortraitPreview() {
    MediaScreenSaverTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Black
        ) { _ ->
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
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Black
        ) { _ ->
            ScreenSaverContent()
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Preview(name = "Tablet Landscape", showBackground = true, device = "spec:width=1280dp,height=800dp,orientation=landscape")
@Composable
fun ScreenSaverTabletLandscapePreview() {
    MediaScreenSaverTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Black
        ) { _ ->
            ScreenSaverContent()
        }
    }
}


@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Preview(name = "Portrait", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
fun ClockPortraitPreview() {
    MediaScreenSaverTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Black
        ) { _ ->
            ScreenSaverContent(showMedia = false)
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Preview(name = "Landscape", showBackground = true, device = "spec:width=720dp,height=360dp,orientation=landscape")
@Composable
fun ClockLandscapePreview(){
    MediaScreenSaverTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Black
        ) {  _ ->
            ScreenSaverContent(showMedia = false)
        }
    }
}

private suspend fun loadBitmapFromUri(context: Context, uriString: String): Bitmap? {
    return withContext(Dispatchers.IO) {
        try {
            val uri = uriString.toUri()
            if (uri.scheme == "http" || uri.scheme == "https") {
                Log.d(TAG, "Skipping web album art URI (app is offline-only): $uriString")
                return@withContext null
            }
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to load album art from local URI: $uriString", e)
            null
        }
    }
}