package dev.benica.infinite_longbox.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Collections

class RequestBatcherTest {
    private val handled = Collections.synchronizedList(mutableListOf<Set<Int>>())
    private fun batcher(waitMs: Long = 60, work: suspend () -> Unit = {}) =
        RequestBatcher<Int>(CoroutineScope(Dispatchers.Default), waitMs) { handled.add(it); work() }

    @Test
    fun whatIsAskedForTogetherIsHandledTogether() = runBlocking {
        val b = batcher()
        listOf(1, 2, 3, 4).forEach { b.request(it) }
        delay(250)
        assertEquals(listOf(setOf(1, 2, 3, 4)), handled.toList())
    }

    @Test
    fun theSameItemTwiceIsOneItem() = runBlocking {
        val b = batcher()
        b.request(7); b.request(7); b.request(7)
        delay(250)
        assertEquals(listOf(setOf(7)), handled.toList())
    }

    @Test
    fun anItemAskedForWhileItIsBeingHandledIsNotHandledAgain() = runBlocking {
        val b = batcher(waitMs = 30) { delay(300) }
        b.request(1)
        delay(150)
        b.request(1)
        delay(500)
        assertEquals(listOf(setOf(1)), handled.toList())
    }

    @Test
    fun anItemAskedForAfterItWasHandledIsHandledAgain() = runBlocking {
        val b = batcher(waitMs = 30)
        b.request(1)
        delay(200)
        b.request(1)
        delay(200)
        assertEquals(listOf(setOf(1), setOf(1)), handled.toList())
    }

    @Test
    fun itemsThatArriveLaterGoInALaterBatch() = runBlocking {
        val b = batcher(waitMs = 40)
        b.request(1); b.request(2)
        delay(200)
        b.request(3)
        delay(200)
        assertEquals(listOf(setOf(1, 2), setOf(3)), handled.toList())
    }
}
