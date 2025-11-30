package net.ubikapps.mediascreensaver.daydream

import android.content.ComponentName
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.dreams.DreamService
import android.text.format.DateFormat
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.delay
import net.ubikapps.mediascreensaver.MediaNotificationListenerService
import net.ubikapps.mediascreensaver.ui.theme.MediaScreenSaverTheme
import java.util.Calendar

val TAG = "MediaScreenSaverService"

class MediaScreenSaverService : DreamService(), LifecycleOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private lateinit var savedStateRegistryController: SavedStateRegistryController

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController = SavedStateRegistryController.create(this)
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = true
        isFullscreen = true

        setContentView(ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@MediaScreenSaverService)
            setViewTreeSavedStateRegistryOwner(this@MediaScreenSaverService)
            setContent {
                MediaScreenSaverTheme {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = Color.Black
                    ) { innerPadding ->
                        ScreenSaverContent(modifier = Modifier.padding(innerPadding))
                    }
                }
            }
        })
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
    }

    override fun onDreamingStopped() {
        super.onDreamingStopped()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry
}

@Composable
fun ScreenSaverContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth().padding(32.dp)
        ) {
            DigitalClock()
            Spacer(modifier = Modifier.height(48.dp))
            MediaInfoDisplay()
        }
    }
}

@Composable
fun DigitalClock() {
    var timeText by remember { mutableStateOf("") }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        while (true) {
            val calendar = Calendar.getInstance()
            timeText = DateFormat.getTimeFormat(context).format(calendar.time)
            delay(1000)
        }
    }

    Text(
        text = timeText,
        style = MaterialTheme.typography.displayLarge,
        color = Color.White,
        fontSize = 80.sp
    )
}

@Composable
fun MediaInfoDisplay() {
    val context = LocalContext.current
    var mediaMetadata by remember { mutableStateOf<MediaMetadata?>(null) }
    var playbackState by remember { mutableStateOf<PlaybackState?>(null) }
    var currentController by remember { mutableStateOf<MediaController?>(null) }

    DisposableEffect(context) {
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

    if (mediaMetadata != null) {
        val title = mediaMetadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown Title"
        val artist = mediaMetadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Unknown Artist"
        val albumArt = mediaMetadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: mediaMetadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)

        val duration = mediaMetadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L

        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        if (isLandscape) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (albumArt != null) {
                        val matrix = ColorMatrix()
                        matrix.setToSaturation(0f)
                        
                        Image(
                            bitmap = albumArt.asImageBitmap(),
                            contentDescription = "Album Art",
                            modifier = Modifier.size(200.dp),
                            colorFilter = ColorFilter.colorMatrix(matrix)
                        )
                        Spacer(modifier = Modifier.width(32.dp))
                    }

                    Column(horizontalAlignment = Alignment.Start) {
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

                    Spacer(modifier = Modifier.width(32.dp))
                    
                    MediaControls(
                        playbackState = playbackState,
                        onPlayPause = { onPlayPause() },
                        onSkipNext = { onSkipNext() },
                        onSkipPrevious = { onSkipPrevious() },
                        onForward30 = { onForward30() },
                        onRewind30 = { onRewind30() }
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))

                if (duration > 0) {
                    PlaybackProgressBar(playbackState, duration, Modifier.fillMaxWidth())
                }
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
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

@Composable
fun PlaybackProgressBar(playbackState: PlaybackState?, duration: Long, modifier: Modifier = Modifier) {
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(playbackState, duration) {
        while (true) {
            if (playbackState != null && playbackState.state == PlaybackState.STATE_PLAYING) {
                val timeDiff = System.currentTimeMillis() - playbackState.lastPositionUpdateTime
                val currentPosition = playbackState.position + (timeDiff * playbackState.playbackSpeed).toLong()
                progress = (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
            } else if (playbackState != null) {
                progress = (playbackState.position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
            }
            delay(1000)
        }
    }

    LinearProgressIndicator(
        progress = progress,
        modifier = modifier,
        color = Color.White,
        trackColor = Color.DarkGray,
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
            Text("-30s", color = Color.White, fontSize = 12.sp) 
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
        
        IconButton(onClick = onPlayPause, modifier = Modifier.size(48.dp)) {
            val isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING
            Canvas(modifier = Modifier.size(32.dp)) {
                val color = Color.White
                if (isPlaying) {
                    drawRect(color, topLeft = Offset(0f, 0f), size = Size(size.width / 3, size.height))
                    drawRect(color, topLeft = Offset(size.width * 2 / 3, 0f), size = Size(size.width / 3, size.height))
                } else {
                    val path = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(size.width, size.height / 2)
                        lineTo(0f, size.height)
                        close()
                    }
                    drawPath(path, color)
                }
            }
        }
        
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
            Text("+30s", color = Color.White, fontSize = 12.sp) 
        }
    }
}