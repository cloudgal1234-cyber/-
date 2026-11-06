package com.taskalarm.app.ui.ringing

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.taskalarm.app.data.Alarm
import com.taskalarm.app.data.AlarmRepository
import com.taskalarm.app.ui.formatDate
import com.taskalarm.app.ui.formatTime
import com.taskalarm.app.ui.task.TaskChallenge
import com.taskalarm.app.ui.theme.ClockStyle
import com.taskalarm.app.ui.theme.Success
import kotlinx.coroutines.delay
import java.time.ZonedDateTime

private sealed interface LoadState {
    data object Loading : LoadState
    data class Ready(val alarm: Alarm?) : LoadState
}

@Composable
fun RingingScreen(alarmId: Long, onSolved: () -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    var solved by rememberSaveable { mutableStateOf(false) }
    val state by produceState<LoadState>(LoadState.Loading, alarmId) {
        value = LoadState.Ready(if (alarmId >= 0) AlarmRepository(context).get(alarmId) else null)
    }
    var now by remember { mutableStateOf(ZonedDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) { now = ZonedDateTime.now(); delay(1000) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (solved) {
            GoodMorning(now, onClose)
            return@Column
        }
        val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
            initialValue = 0.9f, targetValue = 1.15f,
            animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "scale"
        )
        Spacer(Modifier.height(12.dp))
        Icon(
            Icons.Filled.AlarmOn, null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp).scale(pulse)
        )
        Text(formatTime(now.hour, now.minute), style = ClockStyle)
        val ready = state as? LoadState.Ready
        val label = ready?.alarm?.label
        Text(
            if (!label.isNullOrBlank()) label else "זמן לקום!",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Text(
            "השעון לא ייכבה עד שתשלים את המשימה",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        if (ready != null) {
            Column(Modifier.fillMaxWidth()) {
                TaskChallenge(
                    alarm = ready.alarm,
                    onSolved = {
                        solved = true
                        onSolved()
                    }
                )
            }
        }
    }
}

@Composable
private fun GoodMorning(now: ZonedDateTime, onClose: () -> Unit) {
    Spacer(Modifier.height(48.dp))
    Icon(Icons.Filled.WbSunny, null, tint = Success, modifier = Modifier.size(120.dp))
    Text("בוקר טוב!", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
    Text(formatDate(now), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(
        "כל הכבוד — קמת ועשית את המשימה. שיהיה יום מעולה ☀️",
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyLarge
    )
    Spacer(Modifier.height(24.dp))
    Button(
        onClick = onClose,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(16.dp)
    ) { Text("סגור", style = MaterialTheme.typography.titleMedium) }
}
