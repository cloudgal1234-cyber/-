package com.taskalarm.app.ui.places

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.taskalarm.app.data.HouseObject
import com.taskalarm.app.data.ImageFeatures
import com.taskalarm.app.data.PhotoCatalog
import com.taskalarm.app.data.Place
import com.taskalarm.app.data.PlaceRepository
import com.taskalarm.app.data.Rooms
import com.taskalarm.app.ui.permissions.PermissionUtils
import com.taskalarm.app.ui.task.PhotoCameraView
import com.taskalarm.app.ui.theme.Success
import kotlinx.coroutines.launch

/** Register the places in the home: one reference photo (taken twice) per object. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlacesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { PlaceRepository(context) }
    val places by repository.places.collectAsState(initial = emptyList())
    val byKey = places.associateBy { it.objectKey }
    val scope = rememberCoroutineScope()
    var capturing by remember { mutableStateOf<HouseObject?>(null) }
    var confirmDelete by remember { mutableStateOf<HouseObject?>(null) }
    var needCamera by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<HouseObject?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) capturing = pending else needCamera = true
    }
    val startCapture = { obj: HouseObject ->
        if (PermissionUtils.hasCamera(context)) capturing = obj
        else {
            pending = obj
            cameraLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("המקומות שלי בבית") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "חזרה") } }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 4.dp, bottom = padding.calculateBottomPadding() + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("צולמו ${places.size} מתוך ${PhotoCatalog.objects.size} מקומות", fontWeight = FontWeight.Bold)
                        Text(
                            "צלם פעם אחת כל מקום שיש לך בבית. בבוקר השעון ייכבה רק כשתצלם שוב את אותו מקום. " +
                                "כדאי לצלם בתאורה של הבוקר (אור דולק), ולבחור מקומות רחוקים מהמיטה. " +
                                "ככל שתצלם יותר מקומות, יהיו יותר משימות.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            Rooms.order.forEach { room ->
                val inRoom = PhotoCatalog.objects.filter { it.room == room }
                item(key = "room-$room") {
                    Text(room, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                }
                items(inRoom, key = { it.key }) { obj ->
                    PlaceRow(
                        obj = obj,
                        place = byKey[obj.key],
                        repository = repository,
                        onCapture = { startCapture(obj) },
                        onDelete = { confirmDelete = obj }
                    )
                }
            }
        }
    }

    capturing?.let { obj ->
        CaptureDialog(
            obj = obj,
            onDismiss = { capturing = null },
            onSaved = { refs, threshold, photo ->
                scope.launch { repository.save(obj.key, refs, threshold, photo) }
                capturing = null
            }
        )
    }
    confirmDelete?.let { obj ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("למחוק את ${obj.name}?") },
            text = { Text("משימות שכוללות את המקום הזה לא יוגרלו עד שתצלם אותו שוב.") },
            confirmButton = {
                TextButton(onClick = { scope.launch { repository.delete(obj.key) }; confirmDelete = null }) { Text("מחק") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("ביטול") } }
        )
    }
    if (needCamera) {
        AlertDialog(
            onDismissRequest = { needCamera = false },
            title = { Text("נדרשת הרשאת מצלמה") },
            text = { Text("המשימות הן צילום מקומות בבית, ולכן צריך מצלמה. התמונות נשמרות רק במכשיר.") },
            confirmButton = {
                TextButton(onClick = {
                    needCamera = false
                    PermissionUtils.open(context, PermissionUtils.appSettingsIntent(context))
                }) { Text("פתח הגדרות") }
            },
            dismissButton = { TextButton(onClick = { needCamera = false }) { Text("סגור") } }
        )
    }
}

@Composable
private fun PlaceRow(
    obj: HouseObject,
    place: Place?,
    repository: PlaceRepository,
    onCapture: () -> Unit,
    onDelete: () -> Unit,
) {
    val thumb = remember(place) {
        place?.let { runCatching { BitmapFactory.decodeFile(repository.photoFile(it).path)?.asImageBitmap() }.getOrNull() }
    }
    Card(
        onClick = onCapture,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (thumb != null) {
                    Image(thumb, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(Icons.Filled.AddAPhoto, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(obj.name, fontWeight = FontWeight.Bold)
                Text(
                    if (place != null) "צולם ✓ — הקש לצילום מחדש" else obj.hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (place != null) Success else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (place != null) {
                IconButton(onClick = onDelete) { Icon(Icons.Filled.DeleteOutline, "מחק") }
            }
        }
    }
}

/**
 * Two shots of the same place: the first is the reference, the second (from a slightly
 * different spot) measures how stable the match is and sets this place's threshold.
 */
@Composable
private fun CaptureDialog(
    obj: HouseObject,
    onDismiss: () -> Unit,
    onSaved: (List<ImageFeatures>, Float, Bitmap) -> Unit,
) {
    var latest by remember { mutableStateOf<com.taskalarm.app.ui.task.Frame?>(null) }
    var first by remember { mutableStateOf<com.taskalarm.app.ui.task.Frame?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var shots by remember { mutableIntStateOf(0) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            PhotoCameraView(Modifier.fillMaxSize(), keepThumbnails = true) { latest = it }
            Column(
                Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    if (shots == 0) "צלם את ${obj.name}" else "עוד תמונה אחת של ${obj.name}",
                    color = Color.White, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center
                )
                Text(
                    if (shots == 0) obj.hint else "זוז צעד קטן הצידה וצלם שוב את אותו דבר",
                    color = Color.White, textAlign = TextAlign.Center
                )
                LinearProgressIndicator(progress = { shots / 2f }, modifier = Modifier.fillMaxWidth())
                message?.let { Text(it, color = Color(0xFFFFB4AB), textAlign = TextAlign.Center) }
            }
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.6f)),
                    modifier = Modifier.size(56.dp)
                ) { Icon(Icons.Filled.Close, "ביטול", tint = Color.White) }
                FilledIconButton(
                    onClick = {
                        val frame = latest ?: return@FilledIconButton
                        if (shots == 0) {
                            first = frame
                            shots = 1
                            message = null
                        } else {
                            val ref = first ?: return@FilledIconButton
                            val self = ImageFeatures.similarity(ref.features, frame.features)
                            if (self < 0.35) {
                                shots = 0
                                first = null
                                message = "התמונות שונות מדי. צלם שוב, ושים את ${obj.name} באמצע התמונה"
                            } else {
                                val photo = ref.thumbnail ?: frame.thumbnail ?: return@FilledIconButton
                                onSaved(listOf(ref.features, frame.features), ImageFeatures.threshold(self), photo)
                            }
                        }
                    },
                    enabled = latest != null,
                    shape = CircleShape,
                    modifier = Modifier.size(84.dp)
                ) { Icon(Icons.Filled.CameraAlt, "צלם", modifier = Modifier.size(40.dp)) }
                Spacer(Modifier.size(56.dp))
            }
        }
    }
}
