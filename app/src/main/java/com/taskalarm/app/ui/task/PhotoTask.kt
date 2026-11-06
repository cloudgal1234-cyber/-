package com.taskalarm.app.ui.task

import android.Manifest
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.taskalarm.app.data.ImageFeatures
import com.taskalarm.app.data.PhotoMission
import com.taskalarm.app.data.Place
import com.taskalarm.app.data.PlaceRepository
import com.taskalarm.app.ui.permissions.PermissionUtils

/** Photograph the mission's places one after the other. */
@Composable
fun PhotoMissionTask(mission: PhotoMission, places: Map<String, Place>, onComplete: () -> Unit) {
    var index by rememberSaveable(mission.id) { mutableIntStateOf(0) }
    val done by rememberUpdatedState(onComplete)
    val total = mission.objects.size

    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (total > 1) {
            mission.objects.forEachIndexed { i, obj ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Icon(
                        if (i < index) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked, null,
                        tint = if (i <= index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("צלם את ${obj.name}", fontWeight = if (i == index) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
        val obj = mission.objects[index.coerceAtMost(total - 1)]
        val place = places[obj.key]
        key(mission.id, index) {
            if (place != null) {
                PhotoStep(name = obj.name, hint = obj.hint, place = place, onMatched = {
                    if (index + 1 >= total) done() else index += 1
                })
            } else {
                Text("${obj.name} עוד לא צולם בהגדרות", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

/**
 * Live camera that keeps comparing frames with the registered reference of [place].
 * Accepted once two analysed frames in a row reach the place's threshold.
 */
@Composable
private fun PhotoStep(name: String, hint: String, place: Place, onMatched: () -> Unit) {
    val context = LocalContext.current
    val matched by rememberUpdatedState(onMatched)
    var hasPermission by remember { mutableStateOf(PermissionUtils.hasCamera(context)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasPermission = it }
    val references = remember(place) { place.references }
    val thumbnail = remember(place) {
        runCatching { BitmapFactory.decodeFile(PlaceRepository(context).photoFile(place).path)?.asImageBitmap() }.getOrNull()
    }
    var score by remember { mutableFloatStateOf(0f) }
    var streak by remember { mutableIntStateOf(0) }
    var finished by remember { mutableStateOf(false) }

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            thumbnail?.let {
                Image(
                    it, contentDescription = name, contentScale = ContentScale.Crop,
                    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp))
                )
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text("קום וצלם את $name", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(hint, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (hasPermission) {
            PhotoCameraView(
                Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .clip(RoundedCornerShape(24.dp))
            ) { frame ->
                if (finished) return@PhotoCameraView
                val s = ImageFeatures.bestMatch(frame.features, references).toFloat()
                score = s
                streak = if (s >= place.threshold) streak + 1 else 0
                if (streak >= 2) {
                    finished = true
                    matched()
                }
            }
            val progress = (score / place.threshold).coerceIn(0f, 1f)
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            Text(
                when {
                    progress >= 1f -> "נמצא! מחזיק…"
                    progress > 0.75f -> "כמעט! כוון בדיוק כמו בתמונה"
                    else -> "כוון את המצלמה ל$name, כמו בתמונה הקטנה"
                },
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }, modifier = Modifier.fillMaxWidth().height(64.dp)) {
                Icon(Icons.Filled.CameraAlt, null)
                Spacer(Modifier.width(8.dp))
                Text("פתח מצלמה", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
