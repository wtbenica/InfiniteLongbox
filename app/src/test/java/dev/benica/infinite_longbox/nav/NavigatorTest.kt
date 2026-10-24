package dev.benica.infinite_longbox.nav

import androidx.lifecycle.ViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigatorTest {
    private val scheduled = mutableListOf<() -> Unit>()
    private val navigator = Navigator { _, work -> scheduled += work }

    class Probe : ViewModel() {
        var cleared = false

        override fun onCleared() {
            cleared = true
        }
    }

    @Test
    fun itStartsEmpty() {
        assertNull(navigator.top)
    }

    @Test
    fun theLastPushedIsOnTopWithItsOwnKey() {
        val first = navigator.push(Route.List(ListKind.SERIES))
        val second = navigator.push(Route.Issue(7))

        assertSame(second, navigator.top)
        assertTrue(first.key != second.key)
        assertFalse(first.isDetail)
        assertTrue(second.isDetail)
    }

    @Test
    fun backNeverLeavesTheStackEmpty() {
        navigator.push(Route.List(ListKind.SERIES))

        assertFalse(navigator.pop())
        assertEquals(1, navigator.stack.size)
    }

    @Test
    fun aScreenLeftBehindKeepsItsViewModelsUntilItHasFadedOut() {
        navigator.push(Route.List(ListKind.SERIES))
        val page = navigator.push(Route.Issue(7))
        val probe = page.viewModel<Probe>()

        assertTrue(navigator.pop())
        assertFalse(probe.cleared)

        scheduled.forEach { it() }
        assertTrue(probe.cleared)
    }

    @Test
    fun clearingLetsGoOfEveryScreen() {
        val probes = listOf(Route.Issue(1), Route.Character(2), Route.Creator(3))
            .map { navigator.push(it).viewModel<Probe>() }

        navigator.clear()
        scheduled.forEach { it() }

        assertNull(navigator.top)
        assertTrue(probes.all { it.cleared })
    }
}
