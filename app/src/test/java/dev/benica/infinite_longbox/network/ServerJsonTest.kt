package dev.benica.infinite_longbox.network

import dev.benica.infinite_longbox.CollectionBackup
import dev.benica.infinite_longbox.Manifest
import dev.benica.infinite_longbox.database.daos.Count
import dev.benica.infinite_longbox.database.models.GcdSeries
import dev.benica.infinite_longbox.repository.CharacterInfo
import dev.benica.infinite_longbox.repository.CreatorInfo
import dev.benica.infinite_longbox.repository.RelatedCharacter
import dev.benica.infinite_longbox.repository.SeedMeta
import dev.benica.infinite_longbox.repository.TeamRef
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ServerJsonTest {
    private inline fun <reified T> decode(text: String): T = ServerJson.decode(text, T::class.java)

    private fun assertRejected(text: String, type: Class<*>) {
        try {
            ServerJson.decode<Any>(text, type)
            fail("${type.simpleName} should not have been read from $text")
        } catch (e: SerializationException) {
            // the message says which field
            assertTrue(e.message.orEmpty().isNotEmpty())
        }
    }

    @Test
    fun everyResponseClassIsReadWithKotlinx() {
        listOf(
            CharacterInfo::class.java, CreatorInfo::class.java, Manifest::class.java,
            SeedMeta::class.java, GcdSeries::class.java, Count::class.java,
        ).forEach { assertTrue(it.simpleName, ServerJson.usesKotlinx(it)) }
        // not a class the server sends
        assertFalse(ServerJson.usesKotlinx(CollectionBackup::class.java))
    }

    @Test
    fun whatTheServerLeavesOutThatHasADefaultGetsIt() {
        val info = decode<CharacterInfo>("""{"characterId": 5, "name": "Oracle"}""")
        assertEquals(CharacterInfo(characterId = 5, name = "Oracle"), info)
        assertEquals(emptyList<TeamRef>(), info.teams)
        assertEquals(emptyList<RelatedCharacter>(), info.related)
    }

    @Test
    fun whatTheServerLeavesOutThatHasNoDefaultIsRejectedAtOnce() {
        assertRejected("{}", CharacterInfo::class.java)
        assertRejected("""{"characterId": 5}""", CharacterInfo::class.java)
        assertRejected("""{"creatorId": 2}""", CreatorInfo::class.java)
        assertRejected("""{"pages": 3}""", Manifest::class.java)
    }

    @Test
    fun aNullWhereTheTypeAllowsNoneIsRejected() {
        assertRejected("""{"characterId": 5, "name": null}""", CharacterInfo::class.java)
        assertRejected("""{"characterId": 5, "name": "x", "teams": null}""", CharacterInfo::class.java)
    }

    @Test
    fun aNullWhereTheTypeAllowsOneIsKept() {
        val info = decode<CharacterInfo>("""{"characterId": 5, "name": "x", "gcdId": null, "firstAppearance": null}""")
        assertEquals(null, info.gcdId)
        assertEquals(null, info.firstAppearance)
    }

    @Test
    fun fieldsTheAppDoesNotKnowAreIgnored() {
        val info = decode<CharacterInfo>("""{"characterId": 5, "name": "x", "somethingNew": [1, 2]}""")
        assertEquals(5, info.characterId)
    }
}
