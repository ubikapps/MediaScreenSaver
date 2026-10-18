package net.ubikapps.mediascreensaver

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.vectordrawable.graphics.drawable.SeekableAnimatedVectorDrawable
import net.ubikapps.mediascreensaver.daydream.MediaScreenSaverService
import net.ubikapps.mediascreensaver.ui.theme.MediaScreenSaverTheme
import net.ubikapps.mediascreensaver.ui.compose.ExceptionBottomSheet
import net.ubikapps.mediascreensaver.ui.screens.HelpScreen
import net.ubikapps.mediascreensaver.ui.compose.PrivacyPolicyLink


class MainActivity : ComponentActivity() {

    private var hasPermission = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MediaScreenSaverTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainContent(
                        hasPermission = hasPermission.value,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
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
fun MainContent(hasPermission: Boolean, modifier: Modifier = Modifier) {
    var showHelpScreen by rememberSaveable { mutableStateOf(false) }
    var errorMessageResId by rememberSaveable { mutableStateOf<Int?>(null) }
    val configuration = LocalConfiguration.current
    val screenDensity = configuration.densityDpi / 160f
    val screenWidth = configuration.screenWidthDp.toFloat() * screenDensity
    if (showHelpScreen) {
        HelpScreen(
            onBack = { showHelpScreen = false },
            onError = { errorMessageResId = it },
            modifier = modifier
        )
    } else {
        Box(modifier = modifier.fillMaxSize()) {
            val context = LocalContext.current
            val seekableAvd = remember(context) {
                SeekableAnimatedVectorDrawable.create(context, R.drawable.pager_background)
            }

            val pagerState = rememberPagerState(
                initialPage = if (hasPermission) 1 else 0,
                pageCount = { 2 }
            )

            LaunchedEffect(hasPermission) {
                if (hasPermission) {
                    pagerState.animateScrollToPage(1)
                } else {
                    pagerState.animateScrollToPage(0)
                }
            }

            // Background layer: AVD in top half
            Column(modifier = Modifier.fillMaxSize()) {
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
                                setImageDrawable(seekableAvd)
                            }
                        },
                        update = { imageView ->
                            val progress = (pagerState.currentPage + pagerState.currentPageOffsetFraction).coerceIn(0f, 1f)
                            val totalDuration = seekableAvd?.totalDuration ?: 1000L
                            val targetPlayTime = (progress * totalDuration).toLong().coerceIn(0L, totalDuration)
                            seekableAvd?.currentPlayTime = targetPlayTime
                            imageView.invalidate()
                        },
                        modifier = Modifier.fillMaxSize().absoluteOffset(y=2.dp)
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
                            .background(MaterialTheme.colorScheme.background)
                            .weight(1f)
                    ) {
                        when (page) {
                            0 -> SetupScreen(onError = { errorMessageResId = it })
                            1 -> ReadyScreen(onError = { errorMessageResId = it })
                        }
                    }
                }
            }

            IconButton(
                onClick = { showHelpScreen = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_help),
                    contentDescription = stringResource(R.string.help),
                    tint = MaterialTheme.colorScheme.onSurface
                )
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

@Composable
fun ReadyScreen(
    modifier: Modifier = Modifier,
    onError: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if(isOurScreenSaverSet(context)) stringResource(R.string.screen_saver_ready) else stringResource(R.string.nearly_there),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if(isOurScreenSaverSet(context)) stringResource(R.string.done_description) else stringResource(R.string.ready_description),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = {
            try {
                context.startActivity(Intent(Settings.ACTION_DREAM_SETTINGS))
            } catch (_: Exception) {
                onError(R.string.error_unable_to_open_screen_saver_settings)
            }
        }) {
            Text(stringResource(R.string.open_screen_saver_settings))
        }
        Spacer(modifier = Modifier.height(24.dp))
        PrivacyPolicyLink(context, onError)
    }
}

@Composable
fun SetupScreen(
    modifier: Modifier = Modifier,
    onError: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.notification_access_required),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.permission_instructions),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                try {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                } catch (_: Exception) {
                    onError(R.string.error_unable_to_open_notification_settings)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.agree_and_open_notification_settings))
        }
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.prominent_disclosure),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = stringResource(R.string.prominent_disclosure_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = stringResource(R.string.prominent_disclosure_data_accessed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = stringResource(R.string.prominent_disclosure_privacy_safety),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        PrivacyPolicyLink(context, onError)

    }
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
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                ReadyScreen()
                ExceptionBottomSheet(
                    errorMessageResId = R.string.error_unable_to_open_screen_saver_settings,
                    onDismiss = {}
                )
            }
        }
    }
}
