package net.ubikapps.mediascreensaver.ui.compose

import android.content.Context
import android.content.Intent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import net.ubikapps.mediascreensaver.R

@Composable
fun PrivacyPolicyLink(context: Context, onError: (Int) -> Unit) {
    TextButton(onClick = {
        val privacyPolicyUri = "https://ubikapps.net/pages/1398".toUri()
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, privacyPolicyUri))
        } catch (e: Exception) {
            onError(R.string.error_unable_to_open_privacy_policy)
        }
    }) {
        Text(
            text = stringResource(R.string.view_privacy_policy),
            style = MaterialTheme.typography.labelLarge
        )
    }
}