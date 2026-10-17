package net.ubikapps.mediascreensaver

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import net.ubikapps.mediascreensaver.ui.theme.MediaScreenSaverTheme
import net.ubikapps.mediascreensaver.ui.compose.ExceptionBottomSheet
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
        } catch (e: Exception) {
            // Handle potential settings query exception
        }
    }

    private fun isNotificationServiceEnabled(context: Context): Boolean {
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(context.packageName)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(hasPermission: Boolean, modifier: Modifier = Modifier) {
    var errorMessageResId by remember { mutableStateOf<Int?>(null) }

    if (hasPermission) {
        ReadyScreen(
            modifier = modifier,
            onError = { errorMessageResId = it }
        )
    } else {
        SetupScreen(
            modifier = modifier,
            onError = { errorMessageResId = it }
        )
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
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.screen_saver_ready),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.ready_description),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = {
            try {
                context.startActivity(Intent(Settings.ACTION_DREAM_SETTINGS))
            } catch (e: Exception) {
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

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                try {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                } catch (e: Exception) {
                    onError(R.string.error_unable_to_open_notification_settings)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.agree_and_open_notification_settings))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SetupScreenPreview() {
    MediaScreenSaverTheme {
        SetupScreen()
    }
}

@Preview(showBackground = true)
@Composable
fun ReadyScreenPreview() {
    MediaScreenSaverTheme {
        ReadyScreen()
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
