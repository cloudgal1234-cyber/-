package com.taskalarm.app.ui.edit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.taskalarm.app.data.Mission
import com.taskalarm.app.data.MissionDifficulty
import com.taskalarm.app.data.MissionLibrary

private val levels = listOf(MissionDifficulty.ANY, MissionDifficulty.EASY, MissionDifficulty.MEDIUM, MissionDifficulty.HARD)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MissionSettings(
    missionId: String?,
    difficulty: Int,
    onRandom: () -> Unit,
    onDifficulty: (Int) -> Unit,
    onOpenLibrary: () -> Unit,
) {
    val fixed = MissionLibrary.get(missionId)
    Row(
        Modifier.fillMaxWidth().selectable(selected = fixed == null, onClick = onRandom),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = fixed == null, onClick = onRandom)
        Column {
            Text("משימה אקראית בכל בוקר", fontWeight = FontWeight.Bold)
            Text(
                "אי אפשר להתכונן מראש — לא יודעים מה מחכה עד שהשעון מצלצל.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    if (fixed == null) {
        Text("רמת קושי", fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            levels.forEach { level ->
                val count = MissionLibrary.all.count { level == MissionDifficulty.ANY || it.difficulty == level }
                FilterChip(
                    selected = difficulty == level,
                    onClick = { onDifficulty(level) },
                    label = { Text("${MissionDifficulty.name(level)} ($count)") }
                )
            }
        }
    }
    Row(
        Modifier.fillMaxWidth().selectable(selected = fixed != null, onClick = onOpenLibrary),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = fixed != null, onClick = onOpenLibrary)
        Column(Modifier.weight(1f)) {
            Text("משימה קבועה מהספרייה", fontWeight = FontWeight.Bold)
            Text(
                fixed?.let { "${it.title} · ${MissionDifficulty.name(it.difficulty)}" } ?: "בחר אחת מתוך ${MissionLibrary.all.size} משימות",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    if (fixed != null) {
        fixed.steps.forEachIndexed { i, step ->
            Text("${i + 1}. ${step.description}", style = MaterialTheme.typography.bodyMedium)
        }
        OutlinedButton(onClick = onOpenLibrary, modifier = Modifier.fillMaxWidth()) { Text("החלף משימה") }
    }
}

/** Browse / search all missions in the library. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MissionLibraryDialog(selectedId: String?, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var level by remember { mutableIntStateOf(MissionDifficulty.ANY) }
    val missions = remember(query, level) {
        MissionLibrary.all.filter { m ->
            (level == MissionDifficulty.ANY || m.difficulty == level) &&
                (query.isBlank() || m.title.contains(query.trim()) || m.steps.any { it.description.contains(query.trim()) })
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.9f)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("ספריית המשימות", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    placeholder = { Text("חיפוש: אור, צעדים, סיומת 7…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    levels.forEach {
                        FilterChip(selected = level == it, onClick = { level = it }, label = { Text(MissionDifficulty.name(it)) })
                    }
                }
                Text("${missions.size} משימות", color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyColumn(Modifier.weight(1f)) {
                    items(missions, key = { it.id }) { mission ->
                        MissionRow(mission, selected = mission.id == selectedId) { onSelect(mission.id) }
                        HorizontalDivider()
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("סגור") }
            }
        }
    }
}

@Composable
private fun MissionRow(mission: Mission, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column(Modifier.weight(1f)) {
            Text(mission.title, fontWeight = FontWeight.Bold)
            Text(
                mission.steps.joinToString(" ← ") { it.description },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(MissionDifficulty.name(mission.difficulty), style = MaterialTheme.typography.labelMedium)
            if (mission.needsCamera) Icon(Icons.Filled.CameraAlt, contentDescription = "דורש מצלמה")
        }
    }
}
