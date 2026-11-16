package dev.benica.infinite_longbox.database.daos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccentFoldingTest {
    private fun matches(typed: String, name: String): Boolean {
        val regex = AccentFolding.globContaining(typed)
            .replace("[*]", "\u0001").replace("*", ".*").replace("\u0001", "\\*")
        return Regex(regex).matches(name)
    }

    @Test fun plainFindsAccented() = assertTrue(matches("ramon k. perez", "Ramón K. Pérez"))

    @Test fun accentedFindsPlain() = assertTrue(matches("Ramón", "ramon villalobos"))

    @Test fun strokedLetters() = assertTrue(matches("bjorn", "Bjørn"))

    @Test fun nonMatch() = assertEquals(false, matches("ramon", "Ramona"[0] + "xyz"))

    @Test fun globSpecialsEscaped() = assertEquals("*[?]*", AccentFolding.globContaining("?"))
}
