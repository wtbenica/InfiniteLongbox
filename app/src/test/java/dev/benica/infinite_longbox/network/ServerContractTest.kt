package dev.benica.infinite_longbox.network

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import dev.benica.infinite_longbox.Webservice
import java.io.File
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.Response
import retrofit2.http.GET

/**
 * What the app makes of what the server sends.
 *
 * server_samples/<endpoint>.json is a real answer from the server for each [Webservice] call
 * (long lists cut to a few items), and <endpoint>.expected.json is what the app read from it: the
 * sample read into the call's declared type and written back out. The two differ where the app
 * leaves something out. Whatever reads the server's answers has to keep matching the expected
 * files; -Dsnapshot.update=true rewrites them from the current reader.
 */
class ServerContractTest {
    private val writer = GsonBuilder().setPrettyPrinting().serializeNulls().create()

    /** A suspend function's declared type is the type argument of its last, Continuation, parameter. */
    private fun returnType(method: Method): Type {
        val continuation = method.genericParameterTypes.last() as ParameterizedType
        val argument = continuation.actualTypeArguments[0]
        return if (argument is WildcardType) argument.lowerBounds[0] else argument
    }

    private val calls: List<Method> = Webservice::class.java.methods
        .filter { it.isAnnotationPresent(GET::class.java) }
        .filter { returnType(it) != ResponseBody::class.java && !returnType(it).toString().contains(Response::class.java.name) }

    private fun resource(name: String) = javaClass.getResource("/server_samples/$name")

    @Test
    fun everyCallHasASampleOrSaysWhyNot() {
        // not served: the app asks for these but the server has no route for them
        val withoutSample = setOf("getPublishersByIds")
        val missing = calls.map { it.name }.filter { resource("$it.json") == null } - withoutSample
        assertTrue("no sample in server_samples for: $missing", missing.isEmpty())
    }

    @Test
    fun everySampleReadsAsItsExpectedFileSays() {
        val update = System.getProperty("snapshot.update").orEmpty() == "true"
        val dir = File(System.getProperty("snapshot.dir").orEmpty())
        var checked = 0

        calls.forEach { method ->
            val sample = resource("${method.name}.json") ?: return@forEach
            // read the way the app reads it: kotlinx.serialization for its own classes, Gson for the rest
            val read = ServerJson.decode<Any>(sample.readText(), returnType(method))
            val actual = writer.toJson(read)
            val expectedFile = File(dir, "${method.name}.expected.json")

            if (update) {
                expectedFile.writeText(actual + "\n")
            } else {
                val expected = resource("${method.name}.expected.json")?.readText()
                    ?: fail("no expected file for ${method.name}: run with -Dsnapshot.update=true").let { "" }
                assertEquals(
                    method.name,
                    JsonParser.parseString(expected),
                    JsonParser.parseString(actual),
                )
            }
            checked++
        }
        assertTrue("nothing was checked", checked > 0)
    }
}
