package dev.benica.infinite_longbox.ui.issue

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppearanceNameTest {
    @Test
    fun anAlterEgoGoesInBrackets() {
        assertEquals("Steel (John Henry Irons)", appearanceName("Steel", "John Henry Irons", null))
    }

    @Test
    fun theNamesACharacterGoesByStandInForAnAbsentAlterEgo() {
        assertEquals("Superman (Clark Kent; Kal-El)", appearanceName("Superman", null, "Clark Kent; Kal-El"))
        assertNull(roster("Clark Kent; Kal-El"))
    }

    @Test
    fun aTeamsMembersAreALineOfTheirOwn() {
        val members = "Mr. Fantastic [Reed Richards]; The Thing [Ben Grimm]"
        assertEquals("Fantastic Four", appearanceName("Fantastic Four", null, members))
        assertEquals(members, roster(members))
    }

    @Test
    fun anAlterEgoWinsOverTheNamesGivenInTheStory() {
        assertEquals("Superboy (Conner Kent)", appearanceName("Superboy", "Conner Kent", "Kon-El; Conner Kent"))
    }

    @Test
    fun nothingKnownLeavesTheBareName() {
        assertEquals("Lois Lane", appearanceName("Lois Lane", " ", ""))
    }
}
