package com.taskalarm.app.ui.task

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.ui.graphics.vector.ImageVector
import com.taskalarm.app.data.TaskType

fun TaskType.icon(): ImageVector = when (this) {
    TaskType.BARCODE -> Icons.Filled.QrCode2
    TaskType.SHAKE -> Icons.Filled.Vibration
    TaskType.STEPS -> Icons.AutoMirrored.Filled.DirectionsWalk
    TaskType.MATH -> Icons.Filled.Calculate
    TaskType.MEMORY -> Icons.Filled.GridView
    TaskType.TYPING -> Icons.Filled.Keyboard
}
