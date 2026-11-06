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
import com.taskalarm.app.data.Mission
import com.taskalarm.app.data.MissionStep
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/** Runs the steps of a [Mission] in order. */
@Composable
fun MissionTask(mission: Mission, onComplete: () -> Unit) {
    var stepIndex by rememberSaveable(mission.id) { mutableIntStateOf(0) }
    val done by rememberUpdatedState(onComplete)
    val total = mission.steps.size
    val next: () -> Unit = {
        if (stepIndex + 1 >= total) {
            done()
        } else {
            stepIndex += 1
        }
    }

    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (total > 1) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                mission.steps.forEachIndexed { i, step ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (i < stepIndex) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                            null,
                            tint = if (i <= stepIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            step.description,
                            fontWeight = if (i == stepIndex) FontWeight.Bold else FontWeight.Normal,
                            color = if (i == stepIndex) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        key(mission.id, stepIndex) {
            when (val step = mission.steps[stepIndex.coerceAtMost(total - 1)]) {
                is MissionStep.Light -> LightStep(step.factor, onComplete = next)
                is MissionStep.Walk -> StepsTask(target = step.steps, onComplete = next, onUnavailable = next)
                is MissionStep.Jumps -> JumpStep(step.count, onComplete = next)
                is MissionStep.Spins -> SpinStep(step.count, onComplete = next)
            }
        }
    }
}

/** Progress ring with an icon and "n / target". */
@Composable
private fun CounterRing(icon: ImageVector, count: Int, target: Int) {
    Box(contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = { (count.toFloat() / target).coerceIn(0f, 1f) },
            modifier = Modifier.size(180.dp),
            strokeWidth = 14.dp,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, modifier = Modifier.size(36.dp))
            Text("${count.coerceAtMost(target)} / $target", style = MaterialTheme.typography.headlineMedium)
        }
    }
}

/**
 * Jumps in place. A jump is a free-fall phase (< 0.45 g) followed within 0.8 s by a landing
 * (> 1.6 g), with the phone held upright. Without an accelerometer the step becomes a walk.
 */
@Composable
private fun JumpStep(target: Int, onComplete: () -> Unit) {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(SensorManager::class.java) }
    val accel = remember { sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }
    if (accel == null) {
        Text("אין חיישן תאוצה — במקום זה:", textAlign = TextAlign.Center)
        StepsTask(target = target * 4, onComplete = onComplete, onUnavailable = onComplete)
        return
    }
    var count by remember { mutableIntStateOf(0) }
    var upright by remember { mutableStateOf(true) }
    val done by rememberUpdatedState(onComplete)

    DisposableEffect(accel) {
        val gravity = FloatArray(3)
        var freeFallAt = 0L
        var lastJump = 0L
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                for (i in 0..2) gravity[i] = 0.92f * gravity[i] + 0.08f * e.values[i]
                val isUpright = abs(gravity[1]) > 6.5f
                upright = isUpright
                val g = sqrt(e.values[0] * e.values[0] + e.values[1] * e.values[1] + e.values[2] * e.values[2]) /
                    SensorManager.GRAVITY_EARTH
                val now = SystemClock.elapsedRealtime()
                if (g < 0.45f && isUpright) freeFallAt = now
                if (g > 1.6f && freeFallAt != 0L && now - freeFallAt < 800 && now - lastJump > 400) {
                    lastJump = now
                    freeFallAt = 0L
                    count++
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sensorManager?.registerListener(listener, accel, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sensorManager?.unregisterListener(listener) }
    }
    LaunchedEffect(count >= target) { if (count >= target) done() }

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("קום וקפוץ במקום!", style = MaterialTheme.typography.headlineSmall)
        CounterRing(Icons.Filled.SportsGymnastics, count, target)
        Text(
            if (upright) "החזק את הטלפון זקוף ביד וקפוץ" else "החזק את הטלפון זקוף (לא שוכב)",
            textAlign = TextAlign.Center,
            color = if (upright) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
        )
    }
}

/**
 * Full turns in place: rotation around the gravity axis (gyroscope projected on gravity)
 * is integrated while the phone is held upright. Without a gyroscope the step becomes a walk.
 */
@Composable
private fun SpinStep(target: Int, onComplete: () -> Unit) {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(SensorManager::class.java) }
    val gyro = remember { sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE) }
    val accel = remember { sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }
    if (gyro == null || accel == null) {
        Text("אין חיישן סיבוב — במקום זה:", textAlign = TextAlign.Center)
        StepsTask(target = target * 25, onComplete = onComplete, onUnavailable = onComplete)
        return
    }
    var count by remember { mutableIntStateOf(0) }
    var turned by remember { mutableFloatStateOf(0f) }
    var upright by remember { mutableStateOf(true) }
    val done by rememberUpdatedState(onComplete)

    DisposableEffect(gyro, accel) {
        val gravity = floatArrayOf(0f, SensorManager.GRAVITY_EARTH, 0f)
        var angle = 0.0
        var lastTs = 0L
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                if (e.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    for (i in 0..2) gravity[i] = 0.9f * gravity[i] + 0.1f * e.values[i]
                    upright = abs(gravity[1]) > 6.5f
                    return
                }
                if (lastTs != 0L && upright) {
                    val dt = (e.timestamp - lastTs) / 1e9
                    val norm = sqrt(gravity[0] * gravity[0] + gravity[1] * gravity[1] + gravity[2] * gravity[2])
                    if (norm > 0f) {
                        // Angular velocity around the vertical axis.
                        val yawRate = (e.values[0] * gravity[0] + e.values[1] * gravity[1] + e.values[2] * gravity[2]) / norm
                        angle += yawRate * dt
                        if (abs(angle) >= 2 * PI) {
                            angle -= Math.signum(angle) * 2 * PI
                            count++
                        }
                        turned = (abs(angle) / (2 * PI)).toFloat()
                    }
                }
                lastTs = e.timestamp
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sensorManager?.registerListener(listener, accel, SensorManager.SENSOR_DELAY_GAME)
        sensorManager?.registerListener(listener, gyro, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sensorManager?.unregisterListener(listener) }
    }
    LaunchedEffect(count >= target) { if (count >= target) done() }

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("קום והסתובב במקום!", style = MaterialTheme.typography.headlineSmall)
        CounterRing(Icons.Filled.ScreenRotation, count, target)
        LinearProgressIndicator(progress = { turned }, modifier = Modifier.fillMaxWidth())
        Text(
            if (upright) "עמוד, החזק את הטלפון זקוף מולך והסתובב סביב עצמך" else "החזק את הטלפון זקוף (לא שוכב)",
            textAlign = TextAlign.Center,
            color = if (upright) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
        )
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
