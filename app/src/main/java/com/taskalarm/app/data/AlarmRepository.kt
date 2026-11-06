package com.taskalarm.app.data

import android.content.Context
import com.taskalarm.app.alarm.AlarmScheduler
import kotlinx.coroutines.flow.Flow

class AlarmRepository(context: Context) {
    private val app = context.applicationContext
    private val dao = AppDatabase.get(app).alarmDao()

    val alarms: Flow<List<Alarm>> = dao.observeAll()

    suspend fun get(id: Long): Alarm? = dao.getById(id)

    suspend fun save(alarm: Alarm): Alarm {
        val saved = if (alarm.id == 0L) {
            alarm.copy(id = dao.insert(alarm))
        } else {
            dao.update(alarm); alarm
        }
        applySchedule(saved)
        return saved
    }

    suspend fun setEnabled(alarm: Alarm, enabled: Boolean) = save(alarm.copy(enabled = enabled))

    suspend fun delete(alarm: Alarm) {
        AlarmScheduler.cancel(app, alarm.id)
        dao.delete(alarm)
    }

    suspend fun rescheduleAll() {
        dao.getAll().forEach { applySchedule(it) }
    }

    /** Called the moment an alarm starts ringing: queue the next occurrence of a repeating alarm. */
    suspend fun onFired(id: Long) {
        val alarm = dao.getById(id) ?: return
        if (alarm.enabled && alarm.isRepeating) AlarmScheduler.schedule(app, alarm)
    }

    /** Called once the task was completed: a one-time alarm disarms itself. */
    suspend fun onDismissed(id: Long) {
        val alarm = dao.getById(id) ?: return
        if (!alarm.isRepeating && alarm.enabled) {
            dao.update(alarm.copy(enabled = false))
            AlarmScheduler.cancel(app, id)
        }
    }

    private fun applySchedule(alarm: Alarm) {
        if (alarm.enabled) AlarmScheduler.schedule(app, alarm) else AlarmScheduler.cancel(app, alarm.id)
    }
}
