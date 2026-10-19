package dev.benica.infinite_longbox

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.benica.infinite_longbox.database.IssueDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Everything in the app has to share one Room database: Room tells a list about changes made
 * through the same instance only, so a second instance on the same file means lists that never
 * hear about writes. The app starts many threads at once (the sync, the screens, covers).
 */
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class DatabaseSingletonTest {
    @Test
    fun threadsStartingTogetherAllGetTheSameDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // as it is when the app has just started and nothing has asked for the database yet
        val field = IssueDatabase::class.java.getDeclaredField("INSTANCE").apply { isAccessible = true }
        field.set(null, null)

        val threads = 24
        val pool = Executors.newFixedThreadPool(threads)
        val ready = CountDownLatch(threads)
        val go = CountDownLatch(1)
        val results = (1..threads).map {
            pool.submit<Int> {
                ready.countDown()
                go.await()
                System.identityHashCode(IssueDatabase.getInstance(context))
            }
        }
        ready.await(5, TimeUnit.SECONDS)
        go.countDown()
        val distinct = results.map { it.get(10, TimeUnit.SECONDS) }.toSet()
        pool.shutdown()

        assertEquals("how many different databases the threads were given", 1, distinct.size)
    }
}
