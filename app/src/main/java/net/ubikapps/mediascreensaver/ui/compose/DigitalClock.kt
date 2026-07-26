package net.ubikapps.mediascreensaver.ui.compose

import android.text.format.DateFormat
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Calendar


@Composable
fun DigitalClock(fontSize: TextUnit = 80.sp) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    var timeText by remember {
        mutableStateOf(
            if (isPreview) "12:34" else ""
        )
    }

    LaunchedEffect(Unit) {
        if (isPreview) return@LaunchedEffect
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
        fontSize = fontSize
    )
}
