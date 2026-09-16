package dev.aten.rssh.exec

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RunStateStoreTest {
    private val holdMs = 4_000L

    @Test
    fun `idle by default`() = runTest {
        val store = RunStateStore(backgroundScope, holdMs)

        assertEquals(RunState.Idle, store.observe(1).first())
    }

    @Test
    fun `running is reported per command`() = runTest {
        val store = RunStateStore(backgroundScope, holdMs)

        store.running(1)

        assertEquals(RunState.Running, store.observe(1).first())
        assertEquals(RunState.Idle, store.observe(2).first())
    }

    @Test
    fun `result is held then drops back to idle`() = runTest {
        val store = RunStateStore(backgroundScope, holdMs)

        store.running(1)
        store.finished(1, ok = true, summary = "✓ done")
        assertEquals(RunState.Finished(true, "✓ done"), store.observe(1).first())

        advanceTimeBy(holdMs - 1)
        runCurrent()
        assertEquals(RunState.Finished(true, "✓ done"), store.observe(1).first())

        advanceTimeBy(1)
        runCurrent()
        assertEquals(RunState.Idle, store.observe(1).first())
    }

    @Test
    fun `a new run during the hold is not cleared by the previous timer`() = runTest {
        val store = RunStateStore(backgroundScope, holdMs)

        store.finished(1, ok = true, summary = "✓ done")
        advanceTimeBy(holdMs / 2)
        runCurrent()
        store.running(1)

        advanceTimeBy(holdMs)
        runCurrent()
        assertEquals(RunState.Running, store.observe(1).first())

        store.finished(1, ok = true, summary = "✓ done")
        advanceTimeBy(holdMs / 2)
        runCurrent()
        assertEquals(RunState.Finished(true, "✓ done"), store.observe(1).first())

        advanceTimeBy(holdMs / 2)
        runCurrent()
        assertEquals(RunState.Idle, store.observe(1).first())
    }
}
