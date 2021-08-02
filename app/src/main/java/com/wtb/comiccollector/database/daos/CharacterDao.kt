package com.wtb.comiccollector.database.daos

import android.util.Log
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.ComicCollectorApplication
import com.wtb.comiccollector.R
import com.wtb.comiccollector.SearchFilter
import com.wtb.comiccollector.database.models.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import java.util.*

@ExperimentalCoroutinesApi
@Dao
abstract class CharacterDao : BaseDao<Character>("character") {

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
        Log.d(TAG, "creatorQuery filterOptions: ${query.sql}")
        return getCharacterByQuery(query)
    }

    // PAGING SOURCE FUNCTIONS
    @RawQuery(observedEntities = [Character::class])
    abstract fun getCharactersByQueryPagingSource(query: SupportSQLiteQuery): PagingSource<Int, FullCharacter>

    fun getCharactersByFilterPagingSource(filter: SearchFilter): PagingSource<Int, FullCharacter> {
        val query = getCharacterQuery(filter)
        Log.d(TAG, "creatorQuery paged: ${query.sql}")
        return getCharactersByQueryPagingSource(query)
    }

    private fun getCharacterQuery(filter: SearchFilter): SimpleSQLiteQuery {
        val tableJoinString = StringBuilder()
        val conditionsString = StringBuilder()
        val args: ArrayList<Any> = arrayListOf()

        fun connectword(): String = if (conditionsString.isEmpty()) "WHERE" else "AND"

        tableJoinString.append("""SELECT DISTINCT ch.* 
                FROM character ch 
                """)

        if (filter.isNotEmpty()) {
            tableJoinString.append("""JOIN appearance ap ON ap.character = ch.characterId 
                JOIN story sy ON ap.story = sy.storyId 
                JOIN issue ie ON sy.issueId = ie.issueId 
                """)
        }
        if (filter.mPublishers.isNotEmpty()) {
            val publisherList = modelsToSqlIdString(filter.mPublishers)

            conditionsString.append("""${connectword()} ch.publisher IN $publisherList  
            """)
        }

        if (filter.mCreators.isNotEmpty()) {
            val creatorsList = modelsToSqlIdString(filter.mCreators)

            conditionsString.append("""${connectword()} sy.storyId IN (
                SELECT storyId
                FROM credit ct
                JOIN namedetail nl on nl.nameDetailId = ct.nameDetailId
                WHERE nl.creatorId IN $creatorsList)
                OR sy.storyId IN (
                SELECT storyId
                FROM excredit ect
                JOIN namedetail nl on nl.nameDetailId = ect.nameDetailId
                WHERE nl.creatorId IN $creatorsList) 
            """)
        }

        filter.mSeries?.let {
            conditionsString.append("""${connectword()} ie.seriesId = ? 
                """)

            args.add(it.seriesId)
        }

        if (filter.mMyCollection) {
            conditionsString.append("""${connectword()} ie.issueId IN (
                SELECT issueId
                FROM mycollection) 
                """)
        }

        filter.mTextFilter?.let { textFilter ->
            val lookup: Map<FilterTypeSpinnerOption, List<String?>> =
                mapOf(Pair(Series,
                           listOf(ComicCollectorApplication.context?.getString(R.string.table_col_series_name))),
                      Pair(Publisher,
                           listOf(ComicCollectorApplication.context?.getString(R.string.table_col_publisher))),
                      Pair(NameDetail,
                           listOf(ComicCollectorApplication.context?.getString(R.string.table_col_namedetail),
                                  ComicCollectorApplication.context?.getString(R.string.table_col_namedetail2))),
                      Pair(Character,
                           listOf(ComicCollectorApplication.context?.getString(R.string.table_col_character_name),
                                  ComicCollectorApplication.context?.getString(R.string.table_col_character_alterego))))

            conditionsString.append("${connectword()} (")

            when (textFilter.type) {
                All.Companion::class -> {
                    lookup.forEach {
                        conditionsString.append(addTypeFilterElse(it.value, textFilter))
                    }
                }
                else                 -> {
                    conditionsString.append(addTypeFilterElse(lookup[textFilter.type], textFilter))
                }
            }

            conditionsString.append(""") """)
        }


        return SimpleSQLiteQuery(
            "$tableJoinString$conditionsString ORDER BY ch.name", args.toArray()
        )
    }

    companion object {
        private const val TAG = APP + "CharacterDao"

        private fun addTypeFilterElse(
            lookup: List<String?>?,
            textFilter: TextFilter,
        ): StringBuilder {
            val res = StringBuilder()
            var first = true
            lookup?.forEach {
                res.append(
                    addTextFilterCondition(it,
                                           first,
                                           textFilter))
                    .also {
                        first = false
                    }
            }

            return res
        }

        private fun addTextFilterCondition(
            column: String?,
            first: Boolean,
            textFilter: TextFilter,
        ): StringBuilder {
            val res = StringBuilder()
            if (!first) {
                res.append("""OR """)
            }

            res.append("""$column like '%${textFilter.text}%' """)

            return res
        }
    }
}
