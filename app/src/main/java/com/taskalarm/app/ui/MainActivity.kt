package com.taskalarm.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.taskalarm.app.alarm.AlarmService
import com.taskalarm.app.ui.challenge.ChallengeScreen
import com.taskalarm.app.ui.challenge.ProtectedAction
import com.taskalarm.app.ui.dashboard.DashboardScreen
import com.taskalarm.app.ui.edit.EditAlarmRoute
import com.taskalarm.app.ui.permissions.PermissionsScreen
import com.taskalarm.app.ui.ringing.AlarmActivity
import com.taskalarm.app.ui.theme.TaskAlarmTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TaskAlarmTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavHost()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // While an alarm rings, the app can only show the ringing screen.
        AlarmService.ringingAlarmId.value?.let { startActivity(AlarmActivity.intent(this, it)) }
    }
}

private object Routes {
    const val PERMISSIONS = "permissions"
    const val DASHBOARD = "dashboard"
    const val EDIT = "edit/{id}"
    const val CHALLENGE = "challenge/{id}/{action}"
    fun edit(id: Long?) = "edit/${id ?: -1}"
    fun challenge(id: Long, action: ProtectedAction) = "challenge/$id/${action.name}"
}

@Composable
private fun AppNavHost(vm: AlarmViewModel = viewModel()) {
    val context = LocalContext.current
    val nav = rememberNavController()
    val alarms by vm.alarms.collectAsStateWithLifecycle()

    NavHost(
        navController = nav,
        startDestination = if (AppPrefs.isOnboarded(context)) Routes.DASHBOARD else Routes.PERMISSIONS
    ) {
        composable(Routes.PERMISSIONS) {
            PermissionsScreen(onContinue = {
                AppPrefs.setOnboarded(context)
                if (!nav.popBackStack(Routes.DASHBOARD, inclusive = false)) {
                    nav.navigate(Routes.DASHBOARD) { popUpTo(Routes.PERMISSIONS) { inclusive = true } }
                }
            })
        }
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                alarms = alarms,
                onAdd = { nav.navigate(Routes.edit(null)) },
                onEdit = { alarm ->
                    if (alarm.isProtected) nav.navigate(Routes.challenge(alarm.id, ProtectedAction.EDIT))
                    else nav.navigate(Routes.edit(alarm.id))
                },
                onToggle = { alarm, enabled ->
                    if (enabled) vm.setEnabled(alarm, true)
                    else nav.navigate(Routes.challenge(alarm.id, ProtectedAction.DISABLE))
                },
                onDelete = { alarm ->
                    if (alarm.isProtected) nav.navigate(Routes.challenge(alarm.id, ProtectedAction.DELETE))
                    else vm.delete(alarm)
                },
                onFixPermissions = { nav.navigate(Routes.PERMISSIONS) },
            )
        }
        composable(Routes.EDIT, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
            val id = entry.arguments?.getLong("id")?.takeIf { it >= 0 }
            EditAlarmRoute(
                alarmId = id,
                load = vm::load,
                onSave = { alarm -> vm.save(alarm) { nav.popBackStack(Routes.DASHBOARD, inclusive = false) } },
                onBack = { nav.popBackStack() },
            )
        }
        composable(
            Routes.CHALLENGE,
            arguments = listOf(
                navArgument("id") { type = NavType.LongType },
                navArgument("action") { type = NavType.StringType },
            )
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: return@composable
            val action = ProtectedAction.valueOf(entry.arguments?.getString("action") ?: return@composable)
            ChallengeScreen(
                alarmId = id,
                action = action,
                load = vm::load,
                onAuthorized = { alarm ->
                    when (action) {
                        ProtectedAction.DISABLE -> vm.setEnabled(alarm, false) { nav.popBackStack() }
                        ProtectedAction.DELETE -> vm.delete(alarm) { nav.popBackStack() }
                        ProtectedAction.EDIT -> nav.navigate(Routes.edit(alarm.id)) {
                            popUpTo(Routes.DASHBOARD) { inclusive = false }
                        }
                    }
                },
                onCancel = { nav.popBackStack() },
            )
        }
    }
}
