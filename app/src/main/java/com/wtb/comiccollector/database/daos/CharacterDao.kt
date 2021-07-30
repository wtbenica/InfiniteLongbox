package com.wtb.comiccollector.database.daos

import android.util.Log
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.wtb.comiccollector.SearchFilter
import com.wtb.comiccollector.database.models.Character
import com.wtb.comiccollector.database.models.FullCharacter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import java.util.*

@ExperimentalCoroutinesApi
@Dao
abstract class CharacterDao : BaseDao<Character>() {

    @Query("SELECT * FROM character WHERE characterId = :id")
    abstract suspend fun get(id: Int): Character?

    @Query("SELECT * FROM character ORDER BY name ASC")
    abstract fun getAll(): Flow<List<Character>>

    @Transaction
    @Query("SELECT * FROM character WHERE characterId=:characterId")
    abstract fun getCharacter(characterId: Int): Flow<List<FullCharacter>?>

    // FLOW FUNCTIONS
    @RawQuery(observedEntities = [Character::class])
    abstract fun getCharacterByQuery(query: SupportSQLiteQuery): Flow<List<Character>>

    fun getCharacterFilterOptions(filter: SearchFilter): Flow<List<Character>> {
        val query = getCharacterQuery(filter)
        Log.d(TAG, "creatorQuery: ${query.sql}")
        return getCharacterByQuery(query)
    }

    // PAGING SOURCE FUNCTIONS
    @RawQuery(observedEntities = [Character::class])
    abstract fun getCharactersByQueryPagingSource(query: SupportSQLiteQuery): PagingSource<Int, FullCharacter>

    fun getCharactersByFilterPagingSource(filter: SearchFilter): PagingSource<Int, FullCharacter> {
        val query = getCharacterQuery(filter)
        Log.d(TAG, "creatorQuery: ${query.sql}")
        return getCharactersByQueryPagingSource(query)
    }

    private fun getCharacterQuery(filter: SearchFilter): SimpleSQLiteQuery {
        var tableJoinString = String()
        var conditionsString = String()
        val args: ArrayList<Any> = arrayListOf()

        fun connectword(): String = if (conditionsString.isEmpty()) "WHERE" else "AND"

        tableJoinString += """SELECT DISTINCT ch.* 
                FROM character ch 
                LEFT JOIN appearance ap ON ap.character = ch.characterId 
                LEFT JOIN story sy ON ap.story = sy.storyId 
                LEFT JOIN issue ie ON sy.issueId = ie.issueId """

        if (filter.hasPublisher()) {
            val publisherList = modelsToSqlIdString(filter.mPublishers)

            conditionsString += "${connectword()} ch.publisher IN $publisherList "
        }

        if (filter.hasCreator()) {
            tableJoinString += """LEFT JOIN credit ct on ct.storyId = sy.storyId
                        LEFT JOIN namedetail nl on nl.nameDetailId = ct.nameDetailId
                        LEFT JOIN excredit ect on ect.storyId = sy.storyId
                        LEFT JOIN namedetail nl2 on nl2.nameDetailId = ect.nameDetailId """

            val creatorsList = modelsToSqlIdString(filter.mCreators)

            conditionsString += "${connectword()} (nl.creatorId IN $creatorsList " +
                    "or nl2.creatorId IN $creatorsList) "
        }

        filter.mSeries?.let {
            tableJoinString += """JOIN series ss ON ss.seriesId = ie.seriesId """

            conditionsString +=
                """${connectword()} ss.seriesId = ? """

            args.add(it.seriesId)
        }

        if (filter.mMyCollection) {
            tableJoinString += """JOIN mycollection mc ON mc.issueId = ie.issueId """
        }

        return SimpleSQLiteQuery(
            tableJoinString + conditionsString + "ORDER BY ch.name",
            args.toArray()
        )
    }

//    @Query(
//        """SELECT cr.*
//            FROM character cr
//            WHERE cr.name = :name
//            AND cr.alterEgo = :aka
//        """
//    )
//    abstract suspend fun getCharacterByNameAndAka(name: String, aka: String): List<Character>?
//
//    @Query(
//        """SELECT cr.*
//            FROM character cr
//            WHERE cr.name = :name
//        """
//    )
//    abstract suspend fun getCharacterByName(name: String): List<Character>?
//
//    suspend fun getCharacterByInfo(name: String, aka: String? = null): List<Character>? {
//        return if (aka == null) {
//            getCharacterByName(name)
//        } else {
//            getCharacterByNameAndAka(name, aka)
//        }
//    }
//
    companion object {
        private const val TAG = "CharacterDao"
    }
}
