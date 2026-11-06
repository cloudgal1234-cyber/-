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
import androidx.compose.material.icons.filled.AudioFile
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
import com.taskalarm.app.alarm.AlarmSoundPlayer
import com.taskalarm.app.alarm.SoundLibrary
import com.taskalarm.app.data.Alarm
import com.taskalarm.app.data.PlaceRepository
import androidx.compose.runtime.collectAsState
import androidx.compose.material.icons.filled.AddAPhoto
import com.taskalarm.app.data.Days
import com.taskalarm.app.ui.AppPrefs
import com.taskalarm.app.ui.permissions.PermissionUtils
import com.taskalarm.app.ui.theme.Success
import kotlin.math.roundToInt

/** Loads the alarm (or a new default) and shows the editor. */
@Composable
fun EditAlarmRoute(
    alarmId: Long?,
    load: suspend (Long) -> Alarm?,
    onSave: (Alarm) -> Unit,
    onBack: () -> Unit,
    onManagePlaces: () -> Unit,
) {
    val context = LocalContext.current
    val initial by produceState<Alarm?>(initialValue = null, alarmId) {
        value = alarmId?.let { load(it) }
            ?: AppPrefs.lastSound(context)?.let { Alarm(sound = it) }
            ?: Alarm()
    }
    initial?.let {
        EditAlarmScreen(
            it,
            isNew = alarmId == null,
            onSave = { alarm -> AppPrefs.setLastSound(context, alarm.sound); onSave(alarm) },
            onBack = onBack,
            onManagePlaces = onManagePlaces,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditAlarmScreen(
    initial: Alarm,
    isNew: Boolean,
    onSave: (Alarm) -> Unit,
    onBack: () -> Unit,
    onManagePlaces: () -> Unit,
) {
    val context = LocalContext.current
    val timeState = rememberTimePickerState(initial.hour, initial.minute, is24Hour = true)
    var label by rememberSaveable { mutableStateOf(initial.label) }
    var daysMask by rememberSaveable { mutableIntStateOf(initial.daysMask) }
    var missionId by rememberSaveable { mutableStateOf(initial.missionId) }
    var missionDifficulty by rememberSaveable { mutableIntStateOf(initial.missionDifficulty) }
    var showLibrary by remember { mutableStateOf(false) }
    var showTry by remember { mutableStateOf(false) }
    var sound by rememberSaveable { mutableStateOf(SoundLibrary.normalize(initial.sound)) }
    var volume by rememberSaveable { mutableFloatStateOf(initial.volumePercent.toFloat()) }
    var vibrate by rememberSaveable { mutableStateOf(initial.vibrate) }

    val places by remember { PlaceRepository(context).places }.collectAsState(initial = null)
    val registered = places?.map { it.objectKey }?.toSet() ?: emptySet()
    val canSave = registered.isNotEmpty()

    fun draft() = initial.copy(
        hour = timeState.hour,
        minute = timeState.minute,
        daysMask = daysMask,
        label = label.trim(),
        enabled = true,
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
                    Text(
                        if (canSave) "שמור ודרוך" else "צלם קודם לפחות מקום אחד בבית",
                        style = MaterialTheme.typography.titleMedium
                    )
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

            Section("משימת הבוקר: קום וצלם") {
                Text(
                    "כדי לכבות את הצלצול — וגם כדי לכבות, לערוך או למחוק את השעון כל עוד הוא דרוך — " +
                        "צריך לקום ולצלם מקום בבית, למשל את המקרר או את מתג האור.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(onClick = onManagePlaces, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.AddAPhoto, null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (registered.isEmpty()) "צלם מקומות בבית (חובה בפעם הראשונה)"
                        else "המקומות שלי: ${registered.size} צולמו — הוסף עוד"
                    )
                }
                if (registered.isNotEmpty()) {
                    MissionSettings(
                        missionId = missionId,
                        difficulty = missionDifficulty,
                        registered = registered,
                        onRandom = { missionId = null },
                        onDifficulty = { missionDifficulty = it },
                        onOpenLibrary = { showLibrary = true },
                    )
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
            registered = registered,
            onSelect = { missionId = it; showLibrary = false },
            onDismiss = { showLibrary = false }
        )
    }
    if (showTry) {
        TryTaskDialog(alarm = draft(), onDismiss = { showTry = false })
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

    // Any audio file on the phone (mp3, m4a, ogg…). The read permission is persisted so the
    // alarm service can still open the file at ring time, after reboots.
    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            onSound(uri.toString())
            if (previewing) player.start(uri.toString(), volume / 100f)
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
        Button(
            onClick = { runCatching { fileLauncher.launch(arrayOf("audio/*")) } },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.AudioFile, null)
            Spacer(Modifier.width(6.dp))
            Text("בחר קובץ שמע מהטלפון (mp3 וכו')")
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
                    TaskChallenge(alarm = alarm, onSolved = { success = true })
                }
                TextButton(onClick = onDismiss) { Text(if (success) "סגור" else "ביטול") }
            }
        }
    }
}
