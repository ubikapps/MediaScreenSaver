package net.ubikapps.mediascreensaver.daydream

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.drawable.AnimatedVectorDrawable
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.BatteryManager
import android.os.SystemClock
import android.service.dreams.DreamService
import android.text.format.DateFormat
import android.util.Log
import android.view.ViewGroup
import android.widget.ImageView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
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
import net.ubikapps.mediascreensaver.R
import net.ubikapps.mediascreensaver.ui.compose.ScreenSaverContent
import net.ubikapps.mediascreensaver.ui.theme.MediaScreenSaverTheme
import java.util.Calendar
import kotlin.random.Random

const val TAG = "MediaScreenSaverService"

class MediaScreenSaverService : DreamService(), LifecycleOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private lateinit var savedStateRegistryController: SavedStateRegistryController

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController = SavedStateRegistryController.create(this)
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
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
                    ) { _ ->
                        ScreenSaverContent()
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