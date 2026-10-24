package dev.benica.infinite_longbox.nav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class FilterNavigationTest {
    private val series = Route.List(ListKind.SERIES)

    @Test
    fun theFirstFilterStartsTheFirstList() {
        assertEquals(ListMove.START, listMove(null, ListKind.SERIES) { false })
    }

    @Test
    fun aChangeOfFilterIsTakenByTheListOfTheSameKind() {
        assertEquals(ListMove.APPLY, listMove(series, ListKind.SERIES) { false })
    }

    @Test
    fun theSameItemsSeenAnotherWayReplaceTheList() {
        assertEquals(ListMove.REPLACE, listMove(series, ListKind.CHARACTERS) { true })
    }

    @Test
    fun otherItemsInAnotherViewOpenANewList() {
        assertEquals(ListMove.PUSH, listMove(series, ListKind.ISSUES) { false })
    }

    @Test
    fun aFilterAskedFromAPageOpensAListOverIt() {
        assertEquals(ListMove.PUSH, listMove(Route.Issue(7), ListKind.SERIES) { true })
        assertEquals(ListMove.PUSH, listMove(Route.Character(3), ListKind.CHARACTERS) { true })
    }

    @Test
    fun theItemsAreOnlyComparedWhenTheKindDiffers() {
        var asked = false
        listMove(series, ListKind.SERIES) { asked = true; true }
        assertFalse(asked)
    }
}
