package dev.benica.infinite_longbox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CollectionBackupTest {
    private val collections = listOf(
        BackupCollection(
            1, "My Collection", listOf(
                BackupEntry(212404, 7533, "Love and Rockets", "18"),
                BackupEntry(50, 9, "Doom Patrol", "9"),
            )
        ),
        BackupCollection(2, "Wish List", listOf(BackupEntry(77, 3, "Saga", "1"))),
    )

    @Test
    fun aBackupReadsBackAsItWasWritten() {
        val json = CollectionBackup.toJson(collections, "2026-10-19T12:00:00Z")
        assertEquals(CollectionBackup.Parsed.Ok(collections), CollectionBackup.parse(json))
    }

    @Test
    fun aBackupOfNothingIsStillABackup() {
        val json = CollectionBackup.toJson(
            listOf(BackupCollection(1, "My Collection", emptyList())), "now"
        )
        assertEquals(
            CollectionBackup.Parsed.Ok(listOf(BackupCollection(1, "My Collection", emptyList()))),
            CollectionBackup.parse(json)
        )
    }

    @Test
    fun theFileSaysWhatItIsAndCanBeRead() {
        val json = CollectionBackup.toJson(collections, "2026-10-19T12:00:00Z")
        assertTrue(json.contains("\"app\": \"InfiniteLongbox\""))
        assertTrue(json.contains("\"format\": 1"))
        assertTrue(json.contains("Love and Rockets"))
    }

    @Test
    fun somethingThatIsNotJsonIsRefused() {
        assertTrue(CollectionBackup.parse("hello") is CollectionBackup.Parsed.Unreadable)
        assertTrue(CollectionBackup.parse("") is CollectionBackup.Parsed.Unreadable)
    }

    @Test
    fun jsonFromSomethingElseIsRefused() {
        val other = """{"app": "Other", "format": 1, "collections": []}"""
        assertTrue(CollectionBackup.parse(other) is CollectionBackup.Parsed.Unreadable)
        assertTrue(CollectionBackup.parse("{}") is CollectionBackup.Parsed.Unreadable)
        assertTrue(CollectionBackup.parse("[]") is CollectionBackup.Parsed.Unreadable)
    }

    @Test
    fun aFileFromANewerVersionIsRefusedRatherThanHalfRead() {
        val newer = """{"app": "InfiniteLongbox", "format": 2, "collections": []}"""
        val parsed = CollectionBackup.parse(newer) as CollectionBackup.Parsed.Unreadable
        assertTrue(parsed.reason.contains("newer"))
    }

    @Test
    fun entriesWithoutAnIssueAndCollectionsThatDoNotExistAreSkipped() {
        val json = """{"app": "InfiniteLongbox", "format": 1, "collections": [
            {"id": 1, "name": "My Collection", "issues": [
                {"issueId": 0, "seriesId": 1, "series": "x", "number": "1"},
                {"issueId": 5, "seriesId": 1, "series": "y", "number": "2"}]},
            {"id": 9, "name": "Made up", "issues": [{"issueId": 6, "seriesId": 1, "series": "z", "number": "3"}]},
            {"id": 2, "name": "Wish List"}]}"""
        assertEquals(
            CollectionBackup.Parsed.Ok(
                listOf(
                    BackupCollection(1, "My Collection", listOf(BackupEntry(5, 1, "y", "2"))),
                    BackupCollection(2, "Wish List", emptyList()),
                )
            ),
            CollectionBackup.parse(json)
        )
    }

    @Test
    fun theSummaryTellsWhatHappened() {
        assertEquals("Added 12", ImportResult(12, 0, emptyList()).summary())
        assertEquals("Added 3, 9 already there", ImportResult(3, 9, emptyList()).summary())
        val missing = listOf(
            BackupEntry(1, 1, "Doom Patrol", "9"), BackupEntry(2, 1, "Saga", "1"),
            BackupEntry(3, 1, "Hawkeye", "2"), BackupEntry(4, 1, "Flex Mentallo", "3"),
        )
        assertEquals(
            "Added 0. 4 could not be found on the server (Doom Patrol #9, Saga #1, Hawkeye #2, …)",
            ImportResult(0, 0, missing).summary()
        )
    }
}
