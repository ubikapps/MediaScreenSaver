package net.ubikapps.mediascreensaver.ui.compose

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp

@Composable
fun BatteryStatus(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    var batteryLevel by remember { mutableIntStateOf(if (isPreview) 75 else 0) }
    var isCharging by remember { mutableStateOf(if (isPreview) false else false) }

    DisposableEffect(context, isPreview) {
        if (isPreview) return@DisposableEffect onDispose {}

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                batteryLevel = (level * 100 / scale.toFloat()).toInt()

                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(receiver, filter)

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        BatteryIcon(
            level = batteryLevel,
            modifier = Modifier.size(width = 24.dp, height = 14.dp)
        )
        Text(
            text = "$batteryLevel%",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White
        )

    }
}

@Composable
fun BatteryIcon(level: Int, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val strokeWidth = 2.dp.toPx()
        val cornerRadius = 2.dp.toPx()
        val tipWidth = 4.dp.toPx()
        val tipHeight = size.height * 0.4f

        val bodyWidth = size.width - tipWidth
        val bodyHeight = size.height

        val color = Color.White

        // Battery body outline
        drawPath(
            path = Path().apply {
                moveTo(cornerRadius, 0f)
                lineTo(bodyWidth - cornerRadius, 0f)
                quadraticTo(bodyWidth, 0f, bodyWidth, cornerRadius)
                lineTo(bodyWidth, bodyHeight - cornerRadius)
                quadraticTo(bodyWidth, bodyHeight, bodyWidth - cornerRadius, bodyHeight)
                lineTo(cornerRadius, bodyHeight)
                quadraticTo(0f, bodyHeight, 0f, bodyHeight - cornerRadius)
                lineTo(0f, cornerRadius)
                quadraticTo(0f, 0f, cornerRadius, 0f)
            },
            color = color,
            style = Stroke(width = strokeWidth)
        )

        // Battery tip
        drawPath(
            path = Path().apply {
                moveTo(bodyWidth, (bodyHeight - tipHeight) / 2)
                lineTo(size.width - cornerRadius, (bodyHeight - tipHeight) / 2)
                quadraticTo(size.width, (bodyHeight - tipHeight) / 2, size.width, (bodyHeight - tipHeight) / 2 + cornerRadius)
                lineTo(size.width, (bodyHeight + tipHeight) / 2 - cornerRadius)
                quadraticTo(size.width, (bodyHeight + tipHeight) / 2, size.width - cornerRadius, (bodyHeight + tipHeight) / 2)
                lineTo(bodyWidth, (bodyHeight + tipHeight) / 2)
            },
            color = color
        )

        // Battery fill
        val fillPadding = strokeWidth + 2.dp.toPx()
        val maxFillWidth = bodyWidth - (fillPadding * 2)
        val fillWidth = maxFillWidth * (level / 100f)
        val fillHeight = bodyHeight - (fillPadding * 2)

        if (fillWidth > 0) {
            drawRect(
                color = color,
                topLeft = Offset(fillPadding, fillPadding),
                size = Size(fillWidth, fillHeight)
            )
        }
    }
}