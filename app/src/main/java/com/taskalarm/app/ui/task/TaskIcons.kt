package com.taskalarm.app.ui.task

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector
import com.taskalarm.app.data.TaskType

fun TaskType.icon(): ImageVector = when (this) {
    TaskType.MISSION -> Icons.Filled.WbSunny
    TaskType.BARCODE -> Icons.Filled.QrCode2
    TaskType.STEPS -> Icons.AutoMirrored.Filled.DirectionsWalk
}
