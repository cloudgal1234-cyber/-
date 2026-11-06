package com.taskalarm.app.ui.task

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SportsGymnastics
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/** Emergency when photos can't be matched: turn on the light, then walk [steps] steps. */
@Composable
fun EmergencyTask(steps: Int = 150, onComplete: () -> Unit) {
    var stage by rememberSaveable { mutableIntStateOf(0) }
    val done by rememberUpdatedState(onComplete)
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        listOf("הדלק את האור או פתח את התריס", "לך $steps צעדים עם הטלפון").forEachIndexed { i, text ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (i < stage) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked, null,
                    tint = if (i <= stage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(text, fontWeight = if (i == stage) FontWeight.Bold else FontWeight.Normal)
            }
        }
        key(stage) {
            if (stage == 0) LightStep(3f, onComplete = { stage = 1 })
            else StepsTask(target = steps, onComplete = { done() }, onUnavailable = { done() })
        }
    }
}

/**
 * Turn on the light / open the shutter: the ambient light must rise to [factor]× the level
 * measured when the step started (at least 40 lux) and stay there for 2 seconds.
 * Phones without a light sensor get a short walk instead.
 */
@Composable
private fun LightStep(factor: Float, onComplete: () -> Unit) {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(SensorManager::class.java) }
    val sensor = remember { sensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT) }
    if (sensor == null) {
        Text("אין חיישן אור במכשיר — במקום זה:", textAlign = TextAlign.Center)
        StepsTask(target = 40, onComplete = onComplete, onUnavailable = onComplete)
        return
    }
    var lux by remember { mutableFloatStateOf(0f) }
    var baseline by remember { mutableFloatStateOf(-1f) }
    val done by rememberUpdatedState(onComplete)

    DisposableEffect(sensor) {
        var brightSince = 0L
        var finished = false
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val value = event.values[0]
                lux = value
                if (baseline < 0) baseline = value
                val target = max(40f, baseline * factor)
                val now = SystemClock.elapsedRealtime()
                if (value >= target) {
                    if (brightSince == 0L) brightSince = now
                    if (!finished && now - brightSince >= 2000) {
                        finished = true
                        done()
                    }
                } else {
                    brightSince = 0L
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sensorManager?.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        onDispose { sensorManager?.unregisterListener(listener) }
    }

    val target = if (baseline < 0) 40f else max(40f, baseline * factor)
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Filled.LightMode, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(72.dp))
        Text("קום, הדלק את האור או פתח את התריס", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        LinearProgressIndicator(progress = { (lux / target).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
        Text(
            "החזק את הטלפון עם המסך כלפי מעלה, ליד מקור האור, למשך 2 שניות",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
