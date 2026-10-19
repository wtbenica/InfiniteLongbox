package dev.benica.infinite_longbox.repository

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoadTrackerTest {
    @Test
    fun itCountsWhatIsInFlightAndForgetsWhatFinishes() = runBlocking {
        val before = LoadTracker.activeCount.value
        val hold = CompletableDeferred<Unit>()
        val job = async { LoadTracker.track { hold.await() } }
        withTimeout(2000) { while (LoadTracker.activeCount.value != before + 1) delay(5) }
        hold.complete(Unit)
        job.await()
        assertEquals(before, LoadTracker.activeCount.value)
    }

    @Test
    fun aRequestThatFailsIsNotCountedForever() = runBlocking {
        val before = LoadTracker.activeCount.value
        try {
            LoadTracker.track { throw java.io.IOException("refused") }
        } catch (e: java.io.IOException) {
        }
        assertEquals(before, LoadTracker.activeCount.value)
    }

    @Test
    fun itGivesBackWhatTheRequestGave() = runBlocking {
        assertEquals(42, LoadTracker.track { 42 })
    }

    @Test
    fun aRequestThatFinishesAtOnceShowsNothing() = runBlocking {
        val seen = mutableListOf<Boolean>()
        val watcher = launch { LoadTracker.busy(showAfterMs = 150, hideAfterMs = 150).toList(seen) }
        delay(50)
        LoadTracker.track { delay(20) }
        delay(500)
        watcher.cancel()
        assertTrue("shown for a request that took 20 ms: $seen", true !in seen)
    }

    @Test
    fun aLongRequestShowsThenHides() = runBlocking {
        val seen = mutableListOf<Boolean>()
        val watcher = launch { LoadTracker.busy(showAfterMs = 100, hideAfterMs = 100).toList(seen) }
        delay(50)
        LoadTracker.track { delay(400) }
        delay(500)
        watcher.cancel()
        assertEquals(listOf(true, false), seen.takeLast(2))
        assertTrue(true in seen)
    }
}
