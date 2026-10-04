package net.ubikapps.mediascreensaver.daydream

import android.annotation.SuppressLint
import android.service.dreams.DreamService
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import net.ubikapps.mediascreensaver.ui.compose.ScreenSaverContent
import net.ubikapps.mediascreensaver.ui.theme.MediaScreenSaverTheme

const val TAG = "MediaScreenSaverService"

class MediaScreenSaverService : DreamService(), LifecycleOwner, SavedStateRegistryOwner {

    private lateinit var savedStateRegistryController: SavedStateRegistryController

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController = SavedStateRegistryController.create(this)
        savedStateRegistryController.performRestore(null)
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
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
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)
    }

    override fun onDreamingStopped() {
        super.onDreamingStopped()
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    override val lifecycle: Lifecycle
        field = LifecycleRegistry(this)

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry
}