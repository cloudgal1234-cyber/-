package com.taskalarm.app.ui.edit

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.IntentCompat
import androidx.compose.foundation.BorderStroke
import android.os.Build
import com.taskalarm.app.ui.task.TaskChallenge
import com.taskalarm.app.ui.task.icon
import com.taskalarm.app.alarm.AlarmSoundPlayer
import com.taskalarm.app.alarm.SoundLibrary
import com.taskalarm.app.data.Alarm
import com.taskalarm.app.data.Days
import com.taskalarm.app.data.TaskType
import com.taskalarm.app.ui.permissions.PermissionUtils
import com.taskalarm.app.ui.task.BarcodeScannerView
import com.taskalarm.app.ui.theme.Success
import kotlin.math.roundToInt

/** Loads the alarm (or a new default) and shows the editor. */
@Composable
fun EditAlarmRoute(
    alarmId: Long?,
    load: suspend (Long) -> Alarm?,
    onSave: (Alarm) -> Unit,
    onBack: () -> Unit,
) {
    val initial by produceState<Alarm?>(initialValue = null, alarmId) {
        value = alarmId?.let { load(it) } ?: Alarm()
    }
    initial?.let { EditAlarmScreen(it, isNew = alarmId == null, onSave = onSave, onBack = onBack) }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditAlarmScreen(initial: Alarm, isNew: Boolean, onSave: (Alarm) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val timeState = rememberTimePickerState(initial.hour, initial.minute, is24Hour = true)
    var label by rememberSaveable { mutableStateOf(initial.label) }
    var daysMask by rememberSaveable { mutableIntStateOf(initial.daysMask) }
    var taskType by rememberSaveable { mutableStateOf(initial.taskType) }
    var barcodeValue by rememberSaveable { mutableStateOf(initial.barcodeValue) }
    var barcodeFormat by rememberSaveable { mutableStateOf(initial.barcodeFormat) }
    var stepCount by rememberSaveable { mutableFloatStateOf(initial.stepCount.toFloat()) }
    var missionId by rememberSaveable { mutableStateOf(initial.missionId) }
    var missionDifficulty by rememberSaveable { mutableIntStateOf(initial.missionDifficulty) }
    var showLibrary by remember { mutableStateOf(false) }
    var showTry by remember { mutableStateOf(false) }
    var sound by rememberSaveable { mutableStateOf(initial.sound) }
    var volume by rememberSaveable { mutableFloatStateOf(initial.volumePercent.toFloat()) }
    var vibrate by rememberSaveable { mutableStateOf(initial.vibrate) }
    var showScanner by remember { mutableStateOf(false) }
    var showCameraRationale by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) showScanner = true else showCameraRationale = true
    }

    // Optional: without it the step task falls back to the accelerometer.
    val activityLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    // Missions mostly scan product barcodes; without the camera only walking/light missions are used.
    val missionCameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    val canSave = taskType != TaskType.BARCODE || !barcodeValue.isNullOrBlank()

    fun draft() = initial.copy(
        hour = timeState.hour,
        minute = timeState.minute,
        daysMask = daysMask,
        label = label.trim(),
        enabled = true,
        taskType = taskType,
        barcodeValue = barcodeValue,
        barcodeFormat = barcodeFormat,
        stepCount = stepCount.roundToInt(),
        missionId = missionId,
        missionDifficulty = missionDifficulty,
        sound = sound,
        volumePercent = volume.roundToInt(),
        vibrate = vibrate,
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (isNew) "שעון מעורר חדש" else "עריכת שעון") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "חזרה") }
                }
            )
        },
        bottomBar = {
            Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) {
                Button(
                    onClick = { onSave(draft()) },
                    enabled = canSave,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(if (canSave) "שמור ודרוך" else "יש לסרוק ברקוד יעד", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                // Clock faces and HH:MM read left-to-right even in Hebrew UIs.
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    TimePicker(state = timeState)
                }
            }

            Section("ימי חזרה") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Days.shortNames.forEachIndexed { index, name ->
                        FilterChip(
                            selected = Days.isSet(daysMask, index),
                            onClick = { daysMask = Days.toggle(daysMask, index) },
                            label = { Text(name) }
                        )
                    }
                }
                Text(Days.describe(daysMask), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            OutlinedTextField(
                value = label,
                onValueChange = { label = it.take(40) },
                label = { Text("תווית (לא חובה)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Section("מנגנון הכיבוי וההגנה") {
                Text(
                    "כל המשימות מחייבות לקום מהמיטה. המשימה תידרש כדי לכבות את הצלצול — וגם כדי לכבות, לערוך או למחוק את השעון כל עוד הוא דרוך.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TaskTypeGrid(selected = taskType, onSelect = { type ->
                    taskType = type
                    if (type != TaskType.BARCODE && Build.VERSION.SDK_INT >= 29 && !PermissionUtils.hasActivityRecognition(context)) {
                        activityLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                    } else if (type == TaskType.MISSION && !PermissionUtils.hasCamera(context)) {
                        missionCameraLauncher.launch(Manifest.permission.CAMERA)
                    }
                })
                if (taskType == TaskType.BARCODE) {
                    if (!barcodeValue.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CheckCircle, null, tint = Success)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text("ברקוד יעד נשמר", fontWeight = FontWeight.Bold)
                                Text("${barcodeFormat ?: ""}  ${barcodeValue}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            if (PermissionUtils.hasCamera(context)) showScanner = true
                            else cameraLauncher.launch(Manifest.permission.CAMERA)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.QrCodeScanner, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (barcodeValue.isNullOrBlank()) "סרוק ברקוד יעד" else "החלף ברקוד יעד")
                    }
                    Text(
                        "טיפ: בחר מוצר שנמצא רחוק מהמיטה — קופסת חלב במקרר, משחת שיניים באמבטיה. " +
                            "המצלמה משמשת לסריקה בלבד, הכול מעובד על המכשיר.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                when (taskType) {
                    TaskType.BARCODE -> Unit
                    TaskType.STEPS -> {
                        AmountSlider("מספר צעדים נדרש", stepCount, { stepCount = it }, 30f..500f, 46)
                        Text(
                            "הספירה משתמשת בחיישן הצעדים של המכשיר (הרשאת \"פעילות גופנית\"). " +
                                "בלי ההרשאה נעשה שימוש בחיישן התאוצה. הנתונים לא נשמרים.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TaskType.MISSION -> MissionSettings(
                        missionId = missionId,
                        difficulty = missionDifficulty,
                        onRandom = { missionId = null },
                        onDifficulty = { missionDifficulty = it },
                        onOpenLibrary = { showLibrary = true },
                    )
                }
                if (taskType != TaskType.BARCODE || !barcodeValue.isNullOrBlank()) {
                    OutlinedButton(onClick = { showTry = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.PlayArrow, null)
                        Spacer(Modifier.width(8.dp))
                        Text("נסה את המשימה עכשיו")
                    }
                }
            }

            SoundSection(
                sound = sound,
                onSound = { sound = it },
                volume = volume,
                onVolume = { volume = it },
                vibrate = vibrate,
                onVibrate = { vibrate = it }
            )
            Spacer(Modifier.height(8.dp))
        }
    }

    if (showLibrary) {
        MissionLibraryDialog(
            selectedId = missionId,
            onSelect = { missionId = it; showLibrary = false },
            onDismiss = { showLibrary = false }
        )
    }
    if (showTry) {
        TryTaskDialog(alarm = draft(), onDismiss = { showTry = false })
    }
    if (showScanner) {
        ScannerDialog(
            onDismiss = { showScanner = false },
            onScanned = { value, format ->
                barcodeValue = value
                barcodeFormat = format
                showScanner = false
            }
        )
    }
    if (showCameraRationale) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showCameraRationale = false },
            title = { Text("נדרשת הרשאת מצלמה") },
            text = {
                Text(
                    "כדי לשמור ברקוד יעד ולסרוק אותו בבוקר, האפליקציה צריכה גישה למצלמה. " +
                        "התמונה מעובדת על המכשיר בלבד ואינה נשמרת או נשלחת. " +
                        "אפשר גם לבחור במשימת הליכה שלא דורשת מצלמה."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showCameraRationale = false
                    PermissionUtils.open(context, PermissionUtils.appSettingsIntent(context))
                }) { Text("פתח הגדרות") }
            },
            dismissButton = { TextButton(onClick = { showCameraRationale = false }) { Text("סגור") } }
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun SoundSection(
    sound: String,
    onSound: (String) -> Unit,
    volume: Float,
    onVolume: (Float) -> Unit,
    vibrate: Boolean,
    onVibrate: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val player = remember { AlarmSoundPlayer(context) }
    var previewing by remember { mutableStateOf(false) }
    DisposableEffect(Unit) { onDispose { player.stop() } }

    val pickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { IntentCompat.getParcelableExtra(it, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java) }
                ?.let { onSound(it.toString()) }
        }
    }

    fun restartPreview(id: String) {
        if (previewing) player.start(id, volume / 100f)
    }

    Section("צליל ועוצמה") {
        val options = buildList {
            addAll(SoundLibrary.builtIns)
            if (SoundLibrary.isCustom(sound)) add(SoundLibrary.Sound(sound, SoundLibrary.title(context, sound)))
        }
        options.forEach { option ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .selectable(selected = sound == option.id, onClick = { onSound(option.id); restartPreview(option.id) }),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = sound == option.id, onClick = { onSound(option.id); restartPreview(option.id) })
                Text(option.title)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                    .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM or RingtoneManager.TYPE_RINGTONE)
                    .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                    .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false)
                    .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "בחר צליל מעורר")
                if (SoundLibrary.isCustom(sound)) {
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(sound))
                }
                runCatching { pickerLauncher.launch(intent) }
            }) {
                Icon(Icons.Filled.LibraryMusic, null)
                Spacer(Modifier.width(6.dp))
                Text("צלילי המכשיר…")
            }
            OutlinedButton(onClick = {
                if (previewing) {
                    player.stop(); previewing = false
                } else {
                    player.start(sound, volume / 100f); previewing = true
                }
            }) {
                Icon(if (previewing) Icons.Filled.Stop else Icons.Filled.PlayArrow, null)
                Spacer(Modifier.width(6.dp))
                Text(if (previewing) "עצור" else "השמע")
            }
        }
        Text(
            "עוצמה בזמן צלצול: ${volume.roundToInt()}%" + if (volume >= 100f) " (מקסימום)" else "",
            fontWeight = FontWeight.Bold
        )
        Slider(value = volume, onValueChange = onVolume, valueRange = 30f..100f)
        Text(
            "בזמן צלצול העוצמה ננעלת: הורדת עוצמה בכפתורי הצד תוחזר אוטומטית.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("רטט", modifier = Modifier.weight(1f))
            Switch(checked = vibrate, onCheckedChange = onVibrate)
        }
    }
}

@Composable
private fun ScannerDialog(onDismiss: () -> Unit, onScanned: (String, String) -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize()) {
            var done by remember { mutableStateOf(false) }
            BarcodeScannerView(Modifier.fillMaxSize()) { code ->
                if (!done) {
                    done = true
                    onScanned(code.value, code.format)
                }
            }
            Column(
                Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "כוון את המצלמה לברקוד של המוצר שתרצה לסרוק בבוקר",
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(24.dp)
                    .size(64.dp),
                colors = androidx.compose.material3.IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.6f))
            ) {
                Icon(Icons.Filled.Close, "סגור", tint = Color.White, modifier = Modifier.size(32.dp))
            }
        }
    }
}

@Composable
private fun AmountSlider(
    title: String,
    value: Float,
    onChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
) {
    Text("$title: ${value.roundToInt()}", fontWeight = FontWeight.Bold)
    Slider(value = value, onValueChange = onChange, valueRange = range, steps = steps)
}

/** Selectable task cards. */
@Composable
private fun TaskTypeGrid(selected: TaskType, onSelect: (TaskType) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TaskType.entries.forEach { type ->
            val isSelected = type == selected
            Card(
                onClick = { onSelect(type) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainer
                ),
                border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        type.icon(), null,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(type.title, fontWeight = FontWeight.Bold)
                        Text(
                            type.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    RadioButton(selected = isSelected, onClick = { onSelect(type) })
                }
            }
        }
    }
}

/** Lets the user rehearse the task before arming the alarm. */
@Composable
private fun TryTaskDialog(alarm: Alarm, onDismiss: () -> Unit) {
    var success by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        androidx.compose.material3.Surface(
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth(0.94f)
        ) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("ניסיון: ${alarm.taskSummary}", style = MaterialTheme.typography.titleLarge)
                if (success) {
                    Icon(Icons.Filled.CheckCircle, null, tint = Success, modifier = Modifier.size(72.dp))
                    Text("הצלחת! ככה תכבה את השעון בבוקר.", textAlign = TextAlign.Center)
                } else {
                    TaskChallenge(alarm = alarm, autoStartCamera = false, onSolved = { success = true })
                }
                TextButton(onClick = onDismiss) { Text(if (success) "סגור" else "ביטול") }
            }
        }
    }
}
