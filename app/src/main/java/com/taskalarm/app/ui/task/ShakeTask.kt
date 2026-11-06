package com.taskalarm.app.ui.task

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt

private const val SHAKE_THRESHOLD_G = 2.1f
private const val MIN_INTERVAL_MS = 220L

/** Counts vigorous shakes with the accelerometer and calls [onComplete] when [target] is reached. */
@Composable
fun ShakeTask(target: Int, title: String = "נער את הטלפון בחוזקה!", onComplete: () -> Unit) {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(SensorManager::class.java) }
    val accelerometer = remember { sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }
    var count by remember { mutableIntStateOf(0) }
    val done by rememberUpdatedState(onComplete)

    if (accelerometer != null) {
        DisposableEffect(accelerometer) {
            var last = 0L
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    val (x, y, z) = Triple(event.values[0], event.values[1], event.values[2])
                    val g = sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH
                    val now = SystemClock.elapsedRealtime()
                    if (g > SHAKE_THRESHOLD_G && now - last > MIN_INTERVAL_MS) {
                        last = now
                        count++
                    }
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            sensorManager?.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
            onDispose { sensorManager?.unregisterListener(listener) }
        }
    }

    val effectiveTarget = if (accelerometer == null) target * 3 else target
    LaunchedEffect(count >= effectiveTarget) {
        if (count >= effectiveTarget) done()
    }

    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { (count.toFloat() / effectiveTarget).coerceIn(0f, 1f) },
                modifier = Modifier.size(180.dp),
                strokeWidth = 14.dp,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.Vibration, contentDescription = null, modifier = Modifier.size(36.dp))
                Text(
                    "${count.coerceAtMost(effectiveTarget)} / $effectiveTarget",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
        if (accelerometer == null) {
            Text(
                "לא נמצא חיישן תאוצה במכשיר. במקום ניעור, לחץ על הכפתור $effectiveTarget פעמים.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(4.dp))
            Button(onClick = { count++ }) { Text("לחץ") }
        }
    }
}
