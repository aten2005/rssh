package dev.aten.rssh.exec

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface RunState {
    data object Idle : RunState
    data object Running : RunState
    data class Finished(val ok: Boolean, val summary: String) : RunState
}

/**
 * In-process view of which commands are running, so tiles can show progress while the
 * worker does the SSH round trip. A result is held for [holdMs] and then drops back to idle.
 */
class RunStateStore(private val scope: CoroutineScope, private val holdMs: Long = DEFAULT_HOLD_MS) {
    private val states = MutableStateFlow<Map<Long, RunState>>(emptyMap())
    private val holdJobs = HashMap<Long, Job>()

    fun observe(commandId: Long): Flow<RunState> =
        states.map { it[commandId] ?: RunState.Idle }.distinctUntilChanged()

    fun running(commandId: Long) = set(commandId, RunState.Running)

    fun finished(commandId: Long, ok: Boolean, summary: String) {
        set(commandId, RunState.Finished(ok, summary))
        val job = scope.launch {
            delay(holdMs)
            states.update { it - commandId }
        }
        synchronized(holdJobs) { holdJobs[commandId] = job }
    }

    private fun set(commandId: Long, state: RunState) {
        // Cancel first so a previous run's hold timer cannot clear this run's state.
        synchronized(holdJobs) { holdJobs.remove(commandId) }?.cancel()
        states.update { it + (commandId to state) }
    }

    companion object {
        const val DEFAULT_HOLD_MS = 4_000L
    }
}
