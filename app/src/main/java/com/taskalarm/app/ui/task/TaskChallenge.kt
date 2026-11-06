package com.taskalarm.app.ui.task

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.taskalarm.app.data.Alarm
import com.taskalarm.app.data.MissionDifficulty
import com.taskalarm.app.data.MissionLibrary
import com.taskalarm.app.data.TaskType
import com.taskalarm.app.ui.permissions.PermissionUtils
import kotlinx.coroutines.delay

/** Mission swaps allowed per challenge (e.g. can't jump right now). */
private const val MAX_SWAPS = 3

private enum class Mode { BARCODE, STEPS, MISSION, EMERGENCY }

/**
 * The get-out-of-bed task that must be completed — used both to stop a ringing alarm and to
 * authorise disabling / editing / deleting an armed alarm.
 */
@Composable
fun TaskChallenge(
    alarm: Alarm?,
    autoStartCamera: Boolean,
    onSolved: () -> Unit,
) {
    val initial = when (alarm?.taskType) {
        TaskType.BARCODE -> if (alarm?.barcodeValue.isNullOrBlank()) Mode.MISSION else Mode.BARCODE
        TaskType.STEPS -> Mode.STEPS
        else -> Mode.MISSION
    }
    var mode by rememberSaveable { mutableStateOf(initial) }
    var solved by remember { mutableStateOf(false) }
    val solveOnce = {
        if (!solved) {
            solved = true
            onSolved()
        }
    }
    val fixedMission = MissionLibrary.get(alarm?.missionId)
    var missionId by rememberSaveable {
        mutableStateOf(
            fixedMission?.id ?: MissionLibrary.random(difficulty = alarm?.missionDifficulty ?: MissionDifficulty.ANY).id
        )
    }
    var swapsLeft by rememberSaveable { mutableIntStateOf(MAX_SWAPS) }

    when (mode) {
        Mode.BARCODE -> BarcodeTask(
            expected = alarm!!.barcodeValue!!,
            autoStart = autoStartCamera,
            onSolved = solveOnce,
            onEmergency = { mode = Mode.EMERGENCY }
        )
        // No motion sensor at all: fall back to a mission so the alarm can still be stopped.
        Mode.STEPS -> StepsTask(target = alarm?.stepCount ?: 100, onComplete = solveOnce, onUnavailable = { mode = Mode.MISSION })
        Mode.MISSION -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val mission = MissionLibrary.get(missionId) ?: MissionLibrary.emergency
            Text(mission.title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            MissionTask(mission = mission, onComplete = solveOnce)
            if (swapsLeft > 0) {
                TextButton(onClick = {
                    swapsLeft--
                    missionId = MissionLibrary.random(difficulty = mission.difficulty, exclude = mission.id).id
                }) { Text("לא מתאים לי עכשיו — החלף משימה (נותרו $swapsLeft)") }
            }
        }
        Mode.EMERGENCY -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "משימת חירום: הדלק אור ולך 150 צעדים.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            MissionTask(mission = MissionLibrary.emergency, onComplete = solveOnce)
            TextButton(onClick = { mode = Mode.BARCODE }) { Text("חזרה לסריקת הברקוד הקבוע") }
        }
    }
}

@Composable
private fun BarcodeTask(
    expected: String,
    autoStart: Boolean,
    onSolved: () -> Unit,
    onEmergency: () -> Unit,
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(PermissionUtils.hasCamera(context)) }
    var denied by remember { mutableStateOf(false) }
    var scanning by rememberSaveable { mutableStateOf(false) }
    var wrongCount by remember { mutableIntStateOf(0) }
    var showWrong by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        denied = !granted
        if (granted) scanning = true
    }
    val startScan = {
        if (hasPermission) scanning = true else launcher.launch(Manifest.permission.CAMERA)
    }
    LaunchedEffect(Unit) { if (autoStart && hasPermission) scanning = true }
    LaunchedEffect(wrongCount) {
        if (wrongCount > 0) {
            showWrong = true
            delay(2000)
            showWrong = false
        }
    }

    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val hint = expected.takeLast(4)
        Text(
            "סרוק את ברקוד היעד שהגדרת (מסתיים ב־…$hint)",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
        if (scanning && hasPermission) {
            var lastWrongAt by remember { mutableStateOf(0L) }
            BarcodeScannerView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .clip(RoundedCornerShape(24.dp))
            ) { code ->
                if (code.value == expected.trim()) {
                    onSolved()
                } else {
                    val now = System.currentTimeMillis()
                    if (now - lastWrongAt > 1500) {
                        lastWrongAt = now
                        wrongCount++
                    }
                }
            }
            AnimatedVisibility(showWrong) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.ErrorOutline, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(6.dp))
                    Text("זה לא הברקוד הנכון — נסה שוב", color = MaterialTheme.colorScheme.error)
                }
            }
        } else {
            Button(
                onClick = startScan,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(Icons.Filled.QrCodeScanner, null, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(12.dp))
                Text("סרוק ברקוד לכיבוי", style = MaterialTheme.typography.titleLarge)
            }
            if (denied) {
                Text(
                    "בלי הרשאת מצלמה אי אפשר לסרוק. אפשר לאשר אותה בהגדרות האפליקציה, או לבצע משימת חירום.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.error
                )
                OutlinedButton(onClick = { PermissionUtils.open(context, PermissionUtils.appSettingsIntent(context)) }) {
                    Text("פתח הגדרות")
                }
            }
        }
        TextButton(onClick = onEmergency) { Text("המוצר נגמר או אבד? משימת חירום") }
    }
}
