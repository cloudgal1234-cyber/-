package com.taskalarm.app.ui.task

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.taskalarm.app.data.Alarm
import com.taskalarm.app.data.MissionDifficulty
import com.taskalarm.app.data.PhotoCatalog
import com.taskalarm.app.data.Place
import com.taskalarm.app.data.PlaceRepository

/** Mission swaps allowed per challenge (e.g. the place looks different today). */
private const val MAX_SWAPS = 3

/**
 * The get-out-of-bed task: photograph places in the home. Used both to stop a ringing alarm
 * and to authorise disabling / editing / deleting an armed alarm.
 * If no place is registered, or all swaps are used up, an emergency task (light + walking)
 * guarantees the alarm can always be stopped — still by getting up.
 */
@Composable
fun TaskChallenge(alarm: Alarm?, onSolved: () -> Unit) {
    val context = LocalContext.current
    val places by produceState<Map<String, Place>?>(null) {
        value = PlaceRepository(context).getAll().associateBy { it.objectKey }
    }
    val loaded = places ?: return

    var solved by remember { mutableStateOf(false) }
    val solveOnce = {
        if (!solved) {
            solved = true
            onSolved()
        }
    }
    val registered = loaded.keys
    var missionId by rememberSaveable {
        mutableStateOf(
            PhotoCatalog.mission(alarm?.missionId)?.takeIf { m -> m.objects.all { it.key in registered } }?.id
                ?: PhotoCatalog.random(registered, alarm?.missionDifficulty ?: MissionDifficulty.ANY)?.id
        )
    }
    var swapsLeft by rememberSaveable { mutableIntStateOf(MAX_SWAPS) }
    var emergency by rememberSaveable { mutableStateOf(false) }
    val mission = PhotoCatalog.mission(missionId)

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (emergency || mission == null) {
            Text(
                if (mission == null) "עוד לא צילמת מקומות בבית — משימת חירום:" else "משימת חירום:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            EmergencyTask(onComplete = solveOnce)
            if (mission != null) {
                TextButton(onClick = { emergency = false }) { Text("חזרה למשימת הצילום") }
            }
        } else {
            PhotoMissionTask(mission = mission, places = loaded, onComplete = solveOnce)
            Spacer(Modifier.height(8.dp))
            if (swapsLeft > 0) {
                TextButton(onClick = {
                    val next = PhotoCatalog.random(registered, mission.difficulty, exclude = mission.id)
                    if (next == null) {
                        emergency = true // only one place registered: nothing to swap to
                    } else {
                        swapsLeft--
                        missionId = next.id
                    }
                }) { Text("לא מצליח? החלף משימה (נותרו $swapsLeft)") }
            } else {
                OutlinedButton(onClick = { emergency = true }) { Text("עדיין לא מזהה? משימת חירום (אור והליכה)") }
            }
        }
    }
}
