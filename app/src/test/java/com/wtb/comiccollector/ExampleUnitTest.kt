package com.wtb.comiccollector

import com.wtb.comiccollector.database.models.Issue
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun formatDate_isCorrect() {
        val t = Issue.formatDate("2011-02-23")
        assertEquals(Issue.formatDate("2011-02-23"), LocalDate.of(2011, 2, 23))
    }
}