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
import com.taskalarm.app.data.MissionDifficulty
import com.taskalarm.app.data.PhotoCatalog
import com.taskalarm.app.data.PhotoMission

private val levels = listOf(MissionDifficulty.ANY, MissionDifficulty.EASY, MissionDifficulty.MEDIUM, MissionDifficulty.HARD)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MissionSettings(
    missionId: String?,
    difficulty: Int,
    registered: Set<String>,
    onRandom: () -> Unit,
    onDifficulty: (Int) -> Unit,
    onOpenLibrary: () -> Unit,
) {
    val fixed = PhotoCatalog.mission(missionId)
    val eligible = remember(registered) { PhotoCatalog.eligible(registered) }
    Row(
        Modifier.fillMaxWidth().selectable(selected = fixed == null, onClick = onRandom),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = fixed == null, onClick = onRandom)
        Column {
            Text("משימה אקראית בכל בוקר", fontWeight = FontWeight.Bold)
            Text(
                "לא יודעים מה מחכה עד שהשעון מצלצל. כרגע ${eligible.size} משימות זמינות מהמקומות שצילמת.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    if (fixed == null) {
        Text("רמת קושי", fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            levels.forEach { level ->
                val count = eligible.count { level == MissionDifficulty.ANY || it.difficulty == level }
                FilterChip(
                    selected = difficulty == level,
                    onClick = { onDifficulty(level) },
                    label = { Text("${MissionDifficulty.name(level)} ($count)") }
                )
            }
        }
        Text(MissionDifficulty.explain(difficulty), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Row(
        Modifier.fillMaxWidth().selectable(selected = fixed != null, onClick = onOpenLibrary),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = fixed != null, onClick = onOpenLibrary)
        Column(Modifier.weight(1f)) {
            Text("משימה קבועה מהספרייה", fontWeight = FontWeight.Bold)
            Text(
                fixed?.let { "${it.title} · ${MissionDifficulty.name(it.difficulty)}" }
                    ?: "בחר מתוך ${PhotoCatalog.missions.size} משימות",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    if (fixed != null) {
        OutlinedButton(onClick = onOpenLibrary, modifier = Modifier.fillMaxWidth()) { Text("החלף משימה") }
    }
}

/** Browse / search all photo missions. Missions with places not yet photographed are greyed out. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MissionLibraryDialog(
    selectedId: String?,
    registered: Set<String>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var level by remember { mutableIntStateOf(MissionDifficulty.ANY) }
    var onlyAvailable by remember { mutableStateOf(true) }
    val missions = remember(query, level, onlyAvailable, registered) {
        PhotoCatalog.missions.filter { m ->
            (level == MissionDifficulty.ANY || m.difficulty == level) &&
                (!onlyAvailable || m.objects.all { it.key in registered }) &&
                (query.isBlank() || query.trim().split(" ").all { word -> m.title.contains(word) })
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.9f)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("ספריית המשימות (${PhotoCatalog.missions.size})", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    placeholder = { Text("חיפוש: מקרר, מתג, כיור…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    levels.forEach {
                        FilterChip(selected = level == it, onClick = { level = it }, label = { Text(MissionDifficulty.name(it)) })
                    }
                    FilterChip(selected = onlyAvailable, onClick = { onlyAvailable = !onlyAvailable }, label = { Text("רק זמינות") })
                }
                Text("${missions.size} משימות", color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyColumn(Modifier.weight(1f)) {
                    items(missions, key = { it.id }) { mission ->
                        val missing = mission.objects.filter { it.key !in registered }
                        MissionRow(
                            mission,
                            selected = mission.id == selectedId,
                            missing = missing.joinToString(", ") { it.name },
                        ) { if (missing.isEmpty()) onSelect(mission.id) }
                        HorizontalDivider()
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("סגור") }
            }
        }
    }
}

@Composable
private fun MissionRow(mission: PhotoMission, selected: Boolean, missing: String, onClick: () -> Unit) {
    val available = missing.isEmpty()
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = available, onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick, enabled = available)
        Column(Modifier.weight(1f)) {
            Text(
                mission.title,
                fontWeight = FontWeight.Bold,
                color = if (available) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                if (available) MissionDifficulty.explain(mission.difficulty) else "צריך לצלם קודם: $missing",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(MissionDifficulty.name(mission.difficulty), style = MaterialTheme.typography.labelMedium)
    }
}
