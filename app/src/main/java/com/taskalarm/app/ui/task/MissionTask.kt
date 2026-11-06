package com.taskalarm.app.ui.task

import android.Manifest
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.taskalarm.app.data.Mission
import com.taskalarm.app.data.MissionStep
import com.taskalarm.app.ui.permissions.PermissionUtils
import kotlinx.coroutines.delay
import kotlin.math.max

private val RETAIL_FORMATS = setOf("EAN-13", "EAN-8", "UPC-A", "UPC-E")

/** Runs the steps of a [Mission] in order. Products scanned in one mission must all be different. */
@Composable
fun MissionTask(mission: Mission, onComplete: () -> Unit) {
    var stepIndex by rememberSaveable(mission.id) { mutableIntStateOf(0) }
    val scanned = remember(mission.id) { mutableSetOf<String>() }
    val done by rememberUpdatedState(onComplete)
    val total = mission.steps.size
    val next: () -> Unit = {
        if (stepIndex + 1 >= total) done() else stepIndex += 1
    }

    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(mission.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
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
                is MissionStep.Scan -> ScanStep(
                    instruction = "מצא וסרוק מוצר ${step.rule.text}",
                    needed = 1,
                    scanned = scanned,
                    accept = { step.rule.matches(it) },
                    onComplete = next
                )
                is MissionStep.ScanDistinct -> ScanStep(
                    instruction = "סרוק ${step.count} מוצרים שונים מהבית",
                    needed = step.count,
                    scanned = scanned,
                    accept = { true },
                    onComplete = next
                )
            }
        }
    }
}

/**
 * Scan retail product barcodes until [needed] new products pass [accept].
 * Products already used earlier in the mission don't count again.
 */
@Composable
private fun ScanStep(
    instruction: String,
    needed: Int,
    scanned: MutableSet<String>,
    accept: (String) -> Boolean,
    onComplete: () -> Unit,
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(PermissionUtils.hasCamera(context)) }
    var denied by remember { mutableStateOf(false) }
    var found by remember { mutableIntStateOf(0) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var feedbackOk by remember { mutableStateOf(false) }
    var feedbackKey by remember { mutableIntStateOf(0) }
    var lastValue by remember { mutableStateOf("") }
    var lastAt by remember { mutableStateOf(0L) }
    val done by rememberUpdatedState(onComplete)

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasPermission = it; denied = !it
    }
    LaunchedEffect(feedbackKey) {
        if (feedbackKey > 0) {
            delay(1800); feedback = null
        }
    }

    fun say(text: String, ok: Boolean) {
        feedback = text; feedbackOk = ok; feedbackKey++
    }

    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(instruction, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        if (needed > 1) {
            Text("$found / $needed", style = MaterialTheme.typography.headlineSmall)
            LinearProgressIndicator(progress = { found.toFloat() / needed }, modifier = Modifier.fillMaxWidth())
        }
        if (hasPermission) {
            BarcodeScannerView(
                Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .clip(RoundedCornerShape(24.dp))
            ) { code ->
                val now = SystemClock.elapsedRealtime()
                // The analyzer reports the same code many times per second; react once per product.
                if (code.value == lastValue && now - lastAt < 2500) return@BarcodeScannerView
                lastValue = code.value
                lastAt = now
                val digits = code.value.filter { it.isDigit() }
                when {
                    code.format !in RETAIL_FORMATS || digits.length < 8 ->
                        say("זה לא ברקוד של מוצר. סרוק ברקוד מאריזה", false)
                    digits in scanned -> say("את המוצר הזה כבר סרקת — מצא מוצר אחר", false)
                    !accept(digits) -> say("לא מתאים ($digits) — נסה מוצר אחר", false)
                    else -> {
                        scanned += digits
                        found++
                        if (found >= needed) done() else say("מעולה! עוד ${needed - found}", true)
                    }
                }
            }
            AnimatedVisibility(feedback != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (feedbackOk) Icons.Filled.CheckCircle else Icons.Filled.ErrorOutline, null,
                        tint = if (feedbackOk) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(feedback ?: "", color = if (feedbackOk) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                }
            }
        } else {
            Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }, modifier = Modifier.fillMaxWidth().height(64.dp)) {
                Icon(Icons.Filled.QrCodeScanner, null)
                Spacer(Modifier.width(8.dp))
                Text("פתח מצלמה לסריקה", style = MaterialTheme.typography.titleMedium)
            }
            if (denied) {
                Text(
                    "בלי הרשאת מצלמה אי אפשר לסרוק. אפשר להחליף למשימה בלי מצלמה.",
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Turn on the light / open the curtains: the ambient light must rise to [factor]× the level
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
