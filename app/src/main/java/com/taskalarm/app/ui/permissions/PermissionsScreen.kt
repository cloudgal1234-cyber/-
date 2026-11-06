package com.taskalarm.app.ui.permissions

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.taskalarm.app.ui.theme.Success

/** Onboarding / repair screen: every permission with a plain-language reason and a one-tap fix. */
@Composable
fun PermissionsScreen(onContinue: () -> Unit) {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh++ }

    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        refresh++
        if (!it) PermissionUtils.open(context, PermissionUtils.notificationSettingsIntent(context))
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++ }
    val activityLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++ }

    // Recomputed whenever we come back from a dialog or the system settings.
    val notifications = remember(refresh) { PermissionUtils.hasNotifications(context) }
    val exact = remember(refresh) { PermissionUtils.hasExactAlarms(context) }
    val fullScreen = remember(refresh) { PermissionUtils.hasFullScreen(context) }
    val camera = remember(refresh) { PermissionUtils.hasCamera(context) }
    val activity = remember(refresh) { PermissionUtils.hasActivityRecognition(context) }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("ברוכים הבאים ל־מעורר משימה", style = MaterialTheme.typography.headlineMedium)
        Text(
            "כדי שהשעון יצלצל בזמן — גם כשהמסך נעול או האפליקציה סגורה — צריך לאשר כמה הרשאות. " +
                "כל הרשאה מוסברת למטה.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        PermissionCard(
            icon = Icons.Filled.NotificationsActive,
            title = "התראות",
            reason = "מציגות את מסך האזעקה ומשאירות את השעון פעיל עד שהמשימה בוצעה.",
            granted = notifications,
            onGrant = {
                if (Build.VERSION.SDK_INT >= 33) notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                else PermissionUtils.open(context, PermissionUtils.notificationSettingsIntent(context))
            }
        )
        PermissionCard(
            icon = Icons.Filled.Schedule,
            title = "שעונים מעוררים מדויקים",
            reason = "מאפשר לאנדרואיד להעיר את המכשיר בדיוק בשעה שבחרת, גם במצב חיסכון בסוללה.",
            granted = exact,
            onGrant = { PermissionUtils.open(context, PermissionUtils.exactAlarmSettingsIntent(context)) }
        )
        PermissionCard(
            icon = Icons.Filled.Fullscreen,
            title = "התראות במסך מלא",
            reason = "מקפיץ את מסך השעון מעל מסך הנעילה ברגע הצלצול.",
            granted = fullScreen,
            onGrant = { PermissionUtils.open(context, PermissionUtils.fullScreenSettingsIntent(context)) }
        )
        PermissionCard(
            icon = Icons.Filled.CameraAlt,
            title = "מצלמה (מומלץ)",
            reason = "רוב משימות הבוקר הן ציד ברקודים של מוצרים בבית. התמונה מעובדת על המכשיר בלבד, לא נשמרת ולא נשלחת. בלי מצלמה יוגרלו רק משימות אור והליכה.",
            granted = camera,
            onGrant = { cameraLauncher.launch(Manifest.permission.CAMERA) }
        )
        if (Build.VERSION.SDK_INT >= 29) {
            PermissionCard(
                icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                title = "פעילות גופנית (לא חובה)",
                reason = "משמשת רק לספירת צעדים במשימות הליכה. בלעדיה הספירה נעשית עם חיישן התאוצה. שום נתון לא נשמר.",
                granted = activity,
                onGrant = { activityLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION) }
            )
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(if (notifications && exact && fullScreen) "המשך" else "המשך בכל זאת", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    reason: String,
    granted: Boolean,
    onGrant: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(reason, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(8.dp))
            if (granted) {
                Icon(Icons.Filled.CheckCircle, contentDescription = "אושר", tint = Success, modifier = Modifier.size(32.dp))
            } else {
                FilledTonalButton(onClick = onGrant) { Text("אפשר") }
            }
        }
    }
}
