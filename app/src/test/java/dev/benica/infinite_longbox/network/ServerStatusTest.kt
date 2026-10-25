package dev.benica.infinite_longbox.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.io.IOException

class ServerStatusTest {
    private var time = 0L
    private var told = 0
    private val status = ServerStatus(now = { time }, coolOffMs = 1000, onDown = { told++ })

    @Test
    fun requestsAreAllowedUntilTheServerFails() {
        assertTrue(status.allowed())
        status.failed()
        assertFalse(status.allowed())
    }

    @Test
    fun theyAreAllowedAgainAfterTheCoolOff() {
        status.failed()
        time = 1000
        assertTrue(status.allowed())
    }

    @Test
    fun anOutageIsToldOnceWhateverTheNumberOfFailures() {
        status.failed()
        time = 1000
        status.failed()
        assertEquals(1, told)
    }

    @Test
    fun anAnswerEndsTheOutageSoTheNextIsToldAgain() {
        status.failed()
        status.answered()
        assertTrue(status.allowed())
        status.failed()
        assertEquals(2, told)
    }

    @Test
    fun onlyAServerThatCantBeReachedCountsAsDown() {
        assertTrue(status.isUnreachable(ConnectException()))
        assertTrue(status.isUnreachable(SocketTimeoutException()))
        assertFalse(status.isUnreachable(IOException("closed mid-response")))
    }
}
