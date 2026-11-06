package com.taskalarm.app.ui.challenge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.taskalarm.app.data.Alarm
import com.taskalarm.app.ui.formatTime
import com.taskalarm.app.ui.task.TaskChallenge

/** A change to an armed alarm that must be authorised by completing its physical task. */
enum class ProtectedAction(val verb: String, val title: String) {
    DISABLE("לכבות", "כיבוי שעון דרוך"),
    DELETE("למחוק", "מחיקת שעון דרוך"),
    EDIT("לערוך", "עריכת שעון דרוך"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengeScreen(
    alarmId: Long,
    action: ProtectedAction,
    load: suspend (Long) -> Alarm?,
    onAuthorized: (Alarm) -> Unit,
    onCancel: () -> Unit,
) {
    val alarm by produceState<Alarm?>(null, alarmId) { value = load(alarmId) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(action.title) },
                navigationIcon = { IconButton(onClick = onCancel) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "ביטול") } }
            )
        }
    ) { padding ->
        val a = alarm ?: return@Scaffold
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(Icons.Filled.Lock, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp))
            Text(
                "השעון ${formatTime(a.hour, a.minute)} דרוך ומוגן",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "כדי ${action.verb} אותו עליך להשלים את המשימה הפיזית עכשיו. " +
                    "כך אי אפשר לבטל את השעון מראש בערב ולהמשיך לישון בבוקר.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(Modifier.fillMaxWidth()) {
                TaskChallenge(alarm = a, autoStartCamera = false, onSolved = { onAuthorized(a) })
            }
        }
    }
}
