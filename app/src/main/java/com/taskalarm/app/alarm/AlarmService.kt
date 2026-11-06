package com.taskalarm.app.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import com.taskalarm.app.data.Alarm
import com.taskalarm.app.data.AlarmRepository
import com.taskalarm.app.ui.ringing.AlarmActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Foreground service that keeps the alarm ringing (sound + vibration + full-screen notification)
 * until the physical task is completed in [AlarmActivity]. There is intentionally no
 * "dismiss" or "snooze" action on the notification.
 */
class AlarmService : Service() {

    companion object {
        private const val ACTION_START = "com.taskalarm.app.action.START"
        private const val ACTION_TASK_COMPLETED = "com.taskalarm.app.action.TASK_COMPLETED"
        private const val ACTION_REPOST = "com.taskalarm.app.action.REPOST"
        private const val EXTRA_ID = "alarm_id"
        private const val VOLUME_GUARD_MS = 1500L

        private val _ringingAlarmId = MutableStateFlow<Long?>(null)
        /** Id of the alarm currently ringing, or null. Observed by the UI. */
        val ringingAlarmId: StateFlow<Long?> = _ringingAlarmId.asStateFlow()

        fun startIntent(context: Context, id: Long) =
            Intent(context, AlarmService::class.java).setAction(ACTION_START).putExtra(EXTRA_ID, id)

        fun repostIntent(context: Context) =
            Intent(context, AlarmService::class.java).setAction(ACTION_REPOST)

        /** Only called from AlarmActivity after the task was verified. */
        fun completeTask(context: Context) {
            context.startService(Intent(context, AlarmService::class.java).setAction(ACTION_TASK_COMPLETED))
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var repository: AlarmRepository
    private lateinit var audio: AudioManager
    private lateinit var player: AlarmSoundPlayer
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private var currentId: Long? = null
    private var currentLabel: String? = null
    private var originalVolume: Int? = null
    private var targetVolume = 0

    private val volumeGuard = object : Runnable {
        override fun run() {
            // Anti-cheat: if the user lowers the alarm volume, force it back up.
            if (audio.getStreamVolume(AudioManager.STREAM_ALARM) < targetVolume) {
                runCatching { audio.setStreamVolume(AudioManager.STREAM_ALARM, targetVolume, 0) }
            }
            handler.postDelayed(this, VOLUME_GUARD_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        repository = AlarmRepository(this)
        audio = getSystemService(AudioManager::class.java)
        player = AlarmSoundPlayer(this)
        vibrator = if (Build.VERSION.SDK_INT >= 31) {
            getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Vibrator::class.java)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val id = intent.getLongExtra(EXTRA_ID, -1L)
                goForeground(currentId ?: id)
                if (currentId == null) {
                    startRinging(id)
                } else if (currentId != id) {
                    // Another alarm fired while one is already ringing: the user is about to be awake anyway.
                    scope.launch(Dispatchers.IO) { repository.onFired(id); repository.onDismissed(id) }
                }
            }
            ACTION_REPOST -> currentId?.let { goForeground(it) }
            ACTION_TASK_COMPLETED -> finishRinging()
            else -> if (currentId == null) stopSelf()
        }
        // Redeliver so that if the process is killed while ringing, the alarm resumes ringing.
        return START_REDELIVER_INTENT
    }

    private fun goForeground(id: Long) {
        val notification = NotificationHelper.buildRinging(this, id, currentLabel)
        val type = if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0
        ServiceCompat.startForeground(this, NotificationHelper.RINGING_NOTIFICATION_ID, notification, type)
    }

    private fun startRinging(id: Long) {
        currentId = id
        _ringingAlarmId.value = id
        acquireWakeLock()

        scope.launch {
            val alarm = withContext(Dispatchers.IO) {
                repository.onFired(id)
                repository.get(id)
            }
            currentLabel = alarm?.label
            goForeground(id) // refresh with label
            startSound(alarm)
            if (alarm?.vibrate != false) startVibration()
            tryLaunchActivity(id)
        }
    }

    private fun startSound(alarm: Alarm?) {
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        val percent = (alarm?.volumePercent ?: 100).coerceIn(10, 100)
        targetVolume = ((max * percent) / 100).coerceAtLeast(1)
        if (originalVolume == null) originalVolume = audio.getStreamVolume(AudioManager.STREAM_ALARM)
        runCatching { audio.setStreamVolume(AudioManager.STREAM_ALARM, targetVolume, 0) }
        player.start(alarm?.sound ?: com.taskalarm.app.data.SoundIds.SIREN)
        handler.removeCallbacks(volumeGuard)
        handler.postDelayed(volumeGuard, VOLUME_GUARD_MS)
    }

    @Suppress("DEPRECATION")
    private fun startVibration() {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        val effect = VibrationEffect.createWaveform(longArrayOf(0, 900, 500), 0)
        val attrs = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build()
        v.vibrate(effect, attrs)
    }

    /** Works when the screen is on and the app may start activities; otherwise the full-screen intent does it. */
    private fun tryLaunchActivity(id: Long) {
        runCatching {
            startActivity(AlarmActivity.intent(this, id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    private fun finishRinging() {
        val id = currentId
        stopEffects()
        if (id != null) {
            // Use a non-cancelled scope tied to the process; the service scope is cancelled in onDestroy.
            CoroutineScope(Dispatchers.IO).launch { repository.onDismissed(id) }
        }
        currentId = null
        _ringingAlarmId.value = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        NotificationManagerCompat.from(this).cancel(NotificationHelper.RINGING_NOTIFICATION_ID)
        stopSelf()
    }

    private fun stopEffects() {
        handler.removeCallbacks(volumeGuard)
        player.stop()
        vibrator?.cancel()
        originalVolume?.let { runCatching { audio.setStreamVolume(AudioManager.STREAM_ALARM, it, 0) } }
        originalVolume = null
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TaskAlarm:ringing")
            .apply { acquire(60 * 60 * 1000L) }
    }

    override fun onDestroy() {
        stopEffects()
        _ringingAlarmId.value = null
        scope.cancel()
        super.onDestroy()
    }
}
