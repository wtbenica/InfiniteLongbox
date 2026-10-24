package dev.benica.infinite_longbox.filter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class LetterIndexEqualityTest {
    private val a = listOf(LetterBucket("a", 3), LetterBucket("b", 2))

    @Test
    fun sameBucketsAreEqual() = assertEquals(LetterIndex(a, false), LetterIndex(a.reversed(), false))

    @Test
    fun moreRowsMakeADifferentIndex() =
        assertNotEquals(LetterIndex(a, false), LetterIndex(a + LetterBucket("c", 1), false))

    @Test
    fun otherDirectionIsDifferent() = assertNotEquals(LetterIndex(a, false), LetterIndex(a, true))
}
