package com.taskalarm.app.ui.task

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
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
import androidx.core.content.ContextCompat
import kotlin.math.sqrt

/**
 * Walk [target] steps. Uses the hardware step detector when available and permitted
 * (ACTIVITY_RECOGNITION on Android 10+), otherwise a simple accelerometer peak detector.
 * Calls [onUnavailable] if the device has neither sensor.
 */
@Composable
fun StepsTask(target: Int, onComplete: () -> Unit, onUnavailable: () -> Unit) {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(SensorManager::class.java) }
    val canUseDetector = remember {
        Build.VERSION.SDK_INT < 29 || ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACTIVITY_RECOGNITION
        ) == PackageManager.PERMISSION_GRANTED
    }
    val stepDetector = remember {
        if (canUseDetector) sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) else null
    }
    val accelerometer = remember { sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }
    var steps by remember { mutableIntStateOf(0) }
    val done by rememberUpdatedState(onComplete)
    val unavailable by rememberUpdatedState(onUnavailable)

    DisposableEffect(stepDetector, accelerometer) {
        val sensor = stepDetector ?: accelerometer
        if (sensor == null) {
            unavailable()
            return@DisposableEffect onDispose { }
        }
        val listener = if (stepDetector != null) {
            object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) { steps++ }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
        } else {
            AccelerometerStepListener { steps++ }
        }
        sensorManager?.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sensorManager?.unregisterListener(listener) }
    }

    LaunchedEffect(steps >= target) { if (steps >= target) done() }

    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("קום ותתחיל ללכת!", style = MaterialTheme.typography.headlineSmall)
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { (steps.toFloat() / target).coerceIn(0f, 1f) },
                modifier = Modifier.size(180.dp),
                strokeWidth = 14.dp,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.AutoMirrored.Filled.DirectionsWalk, null, modifier = Modifier.size(36.dp))
                Text("${steps.coerceAtMost(target)} / $target", style = MaterialTheme.typography.headlineMedium)
            }
        }
        Text(
            "החזק את הטלפון ביד או בכיס ולך ברחבי הבית.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Counts a step on each acceleration peak above ~1.25 g followed by a dip, at most one per 300 ms. */
private class AccelerometerStepListener(private val onStep: () -> Unit) : SensorEventListener {
    private var armed = true
    private var lastStep = 0L

    override fun onSensorChanged(event: SensorEvent) {
        val (x, y, z) = Triple(event.values[0], event.values[1], event.values[2])
        val g = sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH
        val now = SystemClock.elapsedRealtime()
        if (armed && g > 1.25f && now - lastStep > 300) {
            armed = false
            lastStep = now
            onStep()
        } else if (!armed && g < 1.05f) {
            armed = true
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
