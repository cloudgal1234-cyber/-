package com.taskalarm.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.taskalarm.app.data.Alarm
import com.taskalarm.app.data.AlarmRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlarmViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = AlarmRepository(app)

    val alarms: StateFlow<List<Alarm>?> =
        repository.alarms.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    suspend fun load(id: Long): Alarm? = repository.get(id)

    fun save(alarm: Alarm, onDone: () -> Unit = {}) = viewModelScope.launch {
        repository.save(alarm); onDone()
    }

    /** Arming never needs a task — only disarming an armed alarm does (handled by the caller). */
    fun setEnabled(alarm: Alarm, enabled: Boolean, onDone: () -> Unit = {}) = viewModelScope.launch {
        repository.setEnabled(alarm, enabled); onDone()
    }

    fun delete(alarm: Alarm, onDone: () -> Unit = {}) = viewModelScope.launch {
        repository.delete(alarm); onDone()
    }
}
