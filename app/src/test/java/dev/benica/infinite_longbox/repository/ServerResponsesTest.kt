package dev.benica.infinite_longbox.repository

import com.google.gson.Gson
import dev.benica.infinite_longbox.Manifest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

/**
 * Gson ignores Kotlin's null rules: a field the server leaves out comes back null from a class
 * with a required parameter, even where the type says it can't be. A class with a default for
 * every parameter has a no-arg constructor, which Gson uses, so what is left out gets its default.
 * Reading an empty object into each response class has to give the same thing as making one.
 */
class ServerResponsesTest {
    private val gson = Gson()

    private val responseClasses: List<Class<*>> = listOf(
        CharacterInfo::class.java,
        IssueRef::class.java,
        TeamRef::class.java,
        RelatedCharacter::class.java,
        CreatorInfo::class.java,
        LifeEvent::class.java,
        OtherName::class.java,
        RoleCount::class.java,
        SeriesCount::class.java,
        Manifest::class.java,
    )

    @Test
    fun anEmptyObjectReadsAsTheClassWithItsDefaults() {
        responseClasses.forEach { type ->
            val made = try {
                type.getDeclaredConstructor().newInstance()
            } catch (e: NoSuchMethodException) {
                fail("${type.simpleName} needs a default for every parameter, so Gson can apply them")
                return
            }
            assertEquals(type.simpleName, made, gson.fromJson("{}", type))
        }
    }

    @Test
    fun aPageOfTheServersAnswerWithAListLeftOutHasAnEmptyList() {
        val info = gson.fromJson("""{"characterId": 5, "name": "Oracle"}""", CharacterInfo::class.java)
        assertEquals(emptyList<String>(), info.alternateNames)
        assertEquals(emptyList<TeamRef>(), info.teams)
        assertEquals(emptyList<RelatedCharacter>(), info.related)
    }
}
