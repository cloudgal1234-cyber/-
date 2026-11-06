package com.taskalarm.app.ui.ringing

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.taskalarm.app.alarm.AlarmService
import com.taskalarm.app.ui.theme.TaskAlarmTheme
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * Full-screen ringing screen shown over the lock screen.
 * Back is blocked, volume keys are swallowed, and leaving the screen does not stop the alarm:
 * only completing the physical task does.
 */
class AlarmActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_ID = "alarm_id"
        private const val EXTRA_FALLBACK = "from_fallback"

        fun intent(context: Context, alarmId: Long, fromFallback: Boolean = false): Intent =
            Intent(context, AlarmActivity::class.java)
                .putExtra(EXTRA_ID, alarmId)
                .putExtra(EXTRA_FALLBACK, fromFallback)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
    }

    private val alarmId = mutableLongStateOf(-1L)
    private var solved = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        showOverLockScreen()
        handleIntent(intent)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Blocked on purpose: the only way out is the task.
                if (solved) finish()
            }
        })

        setContent {
            TaskAlarmTheme(dark = true) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    RingingScreen(
                        alarmId = alarmId.longValue,
                        onSolved = {
                            solved = true
                            AlarmService.completeTask(this)
                        },
                        onClose = { finish() }
                    )
                }
            }
        }

        // If the alarm is stopped elsewhere (e.g. service killed and not restarted), close this screen.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                AlarmService.ringingAlarmId.drop(1).collect { if (it == null && !solved) finish() }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val id = intent.getLongExtra(EXTRA_ID, -1L)
        if (id >= 0) alarmId.longValue = id
        // Fallback path: the receiver could not start the service from the background,
        // so start it now that we are in the foreground.
        if (intent.getBooleanExtra(EXTRA_FALLBACK, false) && AlarmService.ringingAlarmId.value == null && id >= 0) {
            runCatching { ContextCompat.startForegroundService(this, AlarmService.startIntent(this, id)) }
        }
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    /** Swallow volume keys so the alarm can't be silenced from the ringing screen. */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (!solved && (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE ||
                keyCode == KeyEvent.KEYCODE_VOLUME_UP)
        ) return true
        return super.onKeyDown(keyCode, event)
    }
}
