package net.ubikapps.mediascreensaver

import android.content.ComponentName
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.provider.Settings
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.vectordrawable.graphics.drawable.SeekableAnimatedVectorDrawable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.ubikapps.mediascreensaver.daydream.MediaScreenSaverService
import net.ubikapps.mediascreensaver.ui.theme.MediaScreenSaverTheme
import net.ubikapps.mediascreensaver.ui.compose.ExceptionBottomSheet
import net.ubikapps.mediascreensaver.ui.screens.HelpScreen
import net.ubikapps.mediascreensaver.ui.screens.ReadyScreen
import net.ubikapps.mediascreensaver.ui.screens.SetupNotificationScreen
import net.ubikapps.mediascreensaver.ui.screens.SetupScreenSaverScreen


class MainActivity : ComponentActivity() {

    private var hasPermission = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MediaScreenSaverTheme {
                MainContent(
                    hasPermission = hasPermission.value,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            hasPermission.value = isNotificationServiceEnabled(this)
        } catch (_: Exception) {
            // Handle potential settings query exception
        }
    }

    private fun isNotificationServiceEnabled(context: Context): Boolean {
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(context.packageName)
    }
}

/**
 * Checks if the screen saver feature is enabled globally in Android settings.
 */
fun isScreenSaverEnabled(context: Context): Boolean {
    return Settings.Secure.getInt(
        context.contentResolver,
        "screensaver_enabled",
        0
    ) == 1
}

/**
 * Checks if MediaScreenSaverService is currently set as the active screen saver.
 */
fun isOurScreenSaverSet(context: Context): Boolean {
    if (!isScreenSaverEnabled(context)) return false

    val componentsString = Settings.Secure.getString(
        context.contentResolver,
        "screensaver_components"
    ) ?: return false

    val myComponent = ComponentName(context, MediaScreenSaverService::class.java)

    return componentsString.split(",").any { component ->
        ComponentName.unflattenFromString(component.trim()) == myComponent
    }
}

@Composable
fun MainContent(
    hasPermission: Boolean,
    modifier: Modifier = Modifier
) {
    var showHelpScreen by rememberSaveable { mutableStateOf(false) }
    var errorMessageResId by rememberSaveable { mutableStateOf<Int?>(null) }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val screenDensity = configuration.densityDpi / 160f
    val screenWidth = configuration.screenWidthDp.toFloat() * screenDensity
    val density = LocalDensity.current
    val statusBarHeightPx = WindowInsets.statusBars.getTop(density).toFloat()

    var landscapeTranslateX by rememberSaveable { mutableFloatStateOf(0F) }

    if (showHelpScreen) {
        HelpScreen(
            onBack = { showHelpScreen = false },
            onError = { errorMessageResId = it },
            modifier = modifier
        )
    } else {
        Box(modifier = modifier.fillMaxSize()) {
            val context = LocalContext.current
            val lifecycleOwner = LocalLifecycleOwner.current
            val coroutineScope = rememberCoroutineScope()
            val seekableAvd = remember(context) {
                SeekableAnimatedVectorDrawable.create(context, R.drawable.pager_background)
            }

            val pagerState = rememberPagerState(
                initialPage = if (hasPermission && isOurScreenSaverSet(context)) 2 else if (hasPermission) 1 else 0,
                pageCount = { 3 }
            )

            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        if (pagerState.currentPage == 1 && isOurScreenSaverSet(context)) {
                            coroutineScope.launch {
                                scrollPage(pagerState, 2)
                            }
                        }
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            LaunchedEffect(hasPermission) {
                if (hasPermission && isOurScreenSaverSet(context)) {
                    scrollPage(pagerState, 2)
                } else if (hasPermission) {
                    scrollPage(pagerState,1)
                } else {
                    scrollPage(pagerState,0)
                }
            }

            if (isLandscape) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(1f)
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                ImageView(ctx).apply {
                                    scaleX = 2F
                                    scaleY = 2F
                                    scaleType = ImageView.ScaleType.FIT_END
                                    translationX = landscapeTranslateX /2F
                                    setImageDrawable(seekableAvd)
                                }
                            },
                            update = { imageView ->
                                imageView.translationX = landscapeTranslateX
                                val maxPage = (pagerState.pageCount - 1).coerceAtLeast(1).toFloat()
                                val progress = ((pagerState.currentPage + pagerState.currentPageOffsetFraction) / maxPage).coerceIn(0f, 1f)
                                val totalDuration = seekableAvd?.totalDuration ?: 1000L
                                val targetPlayTime = (progress * totalDuration).toLong().coerceIn(0L, totalDuration)
                                seekableAvd?.currentPlayTime = targetPlayTime
                                imageView.invalidate()
                            },
                            modifier = Modifier.fillMaxSize().onGloballyPositioned({ coordinates ->
                                landscapeTranslateX = coordinates.size.width.toFloat() /2F
                            })
                        )
                    }
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) { page ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                                .statusBarsPadding()
                        ) {
                            when (page) {
                                0 -> SetupNotificationScreen(onError = { errorMessageResId = it })
                                1 -> SetupScreenSaverScreen(onError = { errorMessageResId = it })
                                2 -> ReadyScreen(onError = { errorMessageResId = it }, onMoreInfoClick = { showHelpScreen = true })
                            }
                        }
                    }
                }
            } else {
                // Portrait
                Column(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                ImageView(ctx).apply {
                                    scaleX = 2F
                                    scaleY = 2F
                                    scaleType = ImageView.ScaleType.FIT_END
                                    translationX = screenWidth / 2F
                                    translationY = -statusBarHeightPx
                                    setImageDrawable(seekableAvd)
                                }
                            },
                            update = { imageView ->
                                imageView.translationY = -statusBarHeightPx
                                val maxPage = (pagerState.pageCount - 1).coerceAtLeast(1).toFloat()
                                val progress = ((pagerState.currentPage + pagerState.currentPageOffsetFraction) / maxPage).coerceIn(0f, 1f)
                                val totalDuration = seekableAvd?.totalDuration ?: 1000L
                                val targetPlayTime = (progress * totalDuration).toLong().coerceIn(0L, totalDuration)
                                seekableAvd?.currentPlayTime = targetPlayTime
                                imageView.invalidate()
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }

                // Foreground layer: HorizontalPager spans the full screen so swipes anywhere drive the pager
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    Column(modifier = Modifier.fillMaxSize()) {
                        Spacer(modifier = Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .layout { measurable, constraints ->
                                    val offsetPx = statusBarHeightPx.toInt()
                                    val targetHeight = constraints.maxHeight + offsetPx
                                    val placeable = measurable.measure(
                                        constraints.copy(
                                            minHeight = targetHeight,
                                            maxHeight = targetHeight
                                        )
                                    )
                                    layout(constraints.maxWidth, constraints.maxHeight) {
                                        placeable.placeRelative(0, -offsetPx)
                                    }
                                }
                                .background(MaterialTheme.colorScheme.background)
                        ) {
                            when (page) {
                                0 -> SetupNotificationScreen(onError = { errorMessageResId = it })
                                1 -> SetupScreenSaverScreen(onError = { errorMessageResId = it })
                                2 -> ReadyScreen(onError = { errorMessageResId = it }, onMoreInfoClick = { showHelpScreen = true })
                            }
                        }
                    }
                }
            }
        }
    }

    errorMessageResId?.let { resId ->
        ExceptionBottomSheet(
            errorMessageResId = resId,
            onDismiss = { errorMessageResId = null }
        )
    }
}

suspend fun scrollPage(pagerState: PagerState, page: Int) {
    delay(300L)
    pagerState.animateScrollToPage(page, animationSpec = tween(
        durationMillis = 900, // triple the default
        easing = FastOutSlowInEasing
    ))
}


@Preview(showBackground = true)
@Composable
fun MainContentPreview() {
    MediaScreenSaverTheme {
        MainContent(false)
    }
}


@Preview(showBackground = true)
@Composable
fun MainContentWithErrorPreview() {
    MediaScreenSaverTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            ReadyScreen()
            ExceptionBottomSheet(
                errorMessageResId = R.string.error_unable_to_open_screen_saver_settings,
                onDismiss = {}
            )
        }
    }
}
