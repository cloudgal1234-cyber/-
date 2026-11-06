package com.taskalarm.app.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AlarmAdd
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.taskalarm.app.data.Alarm
import com.taskalarm.app.data.Days
import com.taskalarm.app.data.NextAlarmCalculator
import com.taskalarm.app.data.TaskType
import com.taskalarm.app.ui.formatCountdown
import com.taskalarm.app.ui.formatDate
import com.taskalarm.app.ui.formatTime
import com.taskalarm.app.ui.permissions.PermissionUtils
import com.taskalarm.app.ui.theme.ClockStyle
import kotlinx.coroutines.delay
import java.time.ZonedDateTime

@Composable
fun DashboardScreen(
    alarms: List<Alarm>?,
    onAdd: () -> Unit,
    onEdit: (Alarm) -> Unit,
    onToggle: (Alarm, Boolean) -> Unit,
    onDelete: (Alarm) -> Unit,
    onFixPermissions: () -> Unit,
) {
    val context = LocalContext.current
    var now by remember { mutableStateOf(ZonedDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = ZonedDateTime.now()
            delay(1000L - now.nano / 1_000_000)
        }
    }
    var permissionsMissing by remember { mutableStateOf(PermissionUtils.criticalMissing(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        permissionsMissing = PermissionUtils.criticalMissing(context)
    }
    var confirmDelete by remember { mutableStateOf<Alarm?>(null) }
    var showAbout by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("שעון מעורר חדש", fontWeight = FontWeight.Bold) },
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Box(Modifier.fillMaxWidth()) {
                    IconButton(onClick = { showAbout = true }, modifier = Modifier.align(Alignment.TopStart)) {
                        Icon(Icons.Filled.Info, contentDescription = "אודות ופרטיות")
                    }
                    ClockHeader(now, alarms)
                }
            }
            if (permissionsMissing) {
                item { PermissionWarning(onFixPermissions) }
            }
            when {
                alarms == null -> Unit
                alarms.isEmpty() -> item { EmptyState() }
                else -> items(alarms, key = { it.id }) { alarm ->
                    AlarmCard(
                        alarm = alarm,
                        onClick = { onEdit(alarm) },
                        onToggle = { onToggle(alarm, it) },
                        onDelete = { if (alarm.isProtected) onDelete(alarm) else confirmDelete = alarm },
                    )
                }
            }
        }
    }

    confirmDelete?.let { alarm ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("למחוק את השעון?") },
            text = { Text("השעון ${formatTime(alarm.hour, alarm.minute)} יימחק לצמיתות.") },
            confirmButton = {
                TextButton(onClick = { onDelete(alarm); confirmDelete = null }) { Text("מחק") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("ביטול") } }
        )
    }
    if (showAbout) AboutDialog { showAbout = false }
}

@Composable
private fun ClockHeader(now: ZonedDateTime, alarms: List<Alarm>?) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(formatTime(now.hour, now.minute), style = ClockStyle)
            Text(
                String.format(java.util.Locale.US, ":%02d", now.second),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp, start = 4.dp)
            )
        }
        Text(formatDate(now), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val next = alarms?.filter { it.enabled }
            ?.map { NextAlarmCalculator.next(it, now) }
            ?.minByOrNull { it.toInstant() }
        Spacer(Modifier.height(12.dp))
        Text(
            if (next != null) "השעון הבא יצלצל ${formatCountdown(now, next)}" else "אין שעונים דרוכים",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun PermissionWarning(onFix: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onFix)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.WarningAmber, null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("חסרות הרשאות קריטיות", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                Text(
                    "בלי ההרשאות השעון עלול לא לצלצל או לא להופיע על מסך הנעילה.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            TextButton(onClick = onFix) { Text("תקן") }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.AlarmAdd, null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Text("עדיין אין שעונים מעוררים", style = MaterialTheme.typography.titleLarge)
        Text(
            "צור שעון ראשון ובחר משימה פיזית — למשל סריקת הברקוד של קופסת החלב במקרר.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun AlarmCard(
    alarm: Alarm,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    val contentAlpha = if (alarm.enabled) 1f else 0.55f
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        formatTime(alarm.hour, alarm.minute),
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
                    )
                    if (alarm.isProtected) {
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.Filled.Lock, contentDescription = "מוגן",
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)
                        )
                    }
                }
                val subtitle = listOfNotNull(alarm.label.takeIf { it.isNotBlank() }, Days.describe(alarm.daysMask)).joinToString(" · ")
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha))
                Spacer(Modifier.height(6.dp))
                AssistChip(
                    onClick = onClick,
                    label = {
                        Text(
                            if (alarm.taskType == TaskType.BARCODE) "סריקת ברקוד" else "${alarm.shakeCount} ניעורים"
                        )
                    },
                    leadingIcon = {
                        Icon(
                            if (alarm.taskType == TaskType.BARCODE) Icons.Filled.QrCode2 else Icons.Filled.Vibration,
                            null, Modifier.size(AssistChipDefaults.IconSize)
                        )
                    }
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Switch(checked = alarm.enabled, onCheckedChange = onToggle)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = "מחק")
                }
            }
        }
    }
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("אודות ופרטיות") },
        text = {
            Text(
                "מעורר משימה הוא שעון מעורר שנכבה רק אחרי משימה פיזית.\n\n" +
                    "• שעון דרוך מוגן: כיבוי, עריכה או מחיקה שלו דורשים לבצע את המשימה.\n" +
                    "• המצלמה משמשת רק לסריקת ברקוד; העיבוד מתבצע על המכשיר ושום תמונה לא נשמרת או נשלחת.\n" +
                    "• כל הנתונים נשמרים מקומית בלבד. לאפליקציה אין הרשאת אינטרנט.\n" +
                    "• איבדת את המוצר עם הברקוד? תמיד קיימת משימת חירום של $EMERGENCY_TEXT ניעורים."
            )
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("הבנתי") } }
    )
}

private const val EMERGENCY_TEXT = com.taskalarm.app.ui.task.EMERGENCY_SHAKES
