package com.wtb.comiccollector.database.daos

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.SearchFilter
import com.wtb.comiccollector.SortType
import com.wtb.comiccollector.SortType.Companion.containsSortType
import com.wtb.comiccollector.database.models.Character
import com.wtb.comiccollector.database.models.FullCharacter
import com.wtb.comiccollector.database.models.TextFilter
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

        return getCharacterByQuery(query)
    }

    // PAGING SOURCE FUNCTIONS
    @RawQuery(observedEntities = [Character::class])
    abstract fun getCharactersByQueryPagingSource(query: SupportSQLiteQuery): PagingSource<Int, FullCharacter>

    fun getCharactersByFilterPagingSource(filter: SearchFilter): PagingSource<Int, FullCharacter> {
        val query = getCharacterQuery(filter)

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
                JOIN issue ie ON ap.issue = ie.issueId 
                """)
        }
        if (filter.mPublishers.isNotEmpty()) {
            val publisherList = modelsToSqlIdString(filter.mPublishers)

            conditionsString.append("""${connectword()} ch.publisher IN $publisherList  
            """)
        }

        if (filter.mCreators.isNotEmpty()) {
            val creatorsList = modelsToSqlIdString(filter.mCreators)

            tableJoinString.append("""JOIN story sy ON sy.storyId = ap.story """)

            conditionsString.append("""${connectword()} (sy.storyId IN (
                SELECT story
                FROM credit ct
                JOIN namedetail nl on nl.nameDetailId = ct.nameDetail
                WHERE nl.creator IN $creatorsList)
                OR sy.storyId IN (
                SELECT story
                FROM excredit ect
                JOIN namedetail nl on nl.nameDetailId = ect.nameDetail
                WHERE nl.creator IN $creatorsList))
            """)
        }

        filter.mSeries?.let {
            conditionsString.append("""${connectword()} ie.series = ? 
                """)

            args.add(it.series.seriesId)
        }

        if (filter.mMyCollection) {
            conditionsString.append("""${connectword()} ie.issueId IN (
                SELECT issue
                FROM mycollection) 
                """)
        }

        filter.mTextFilter?.let { textFilter ->
            val text = textFilterToString(textFilter.text)
            conditionsString.append(
                """${connectword()} ch.name LIKE '$text'
                    OR ch.alterEgo LIKE '$text'
                    """)
        }

        val sortClause: String = filter.mSortType?.let {
            val isValid =
                SortType.Companion.SortTypeOptions.CHARACTER.options.containsSortType(it)
            val sortString: String =
                if (isValid) {
                    it.sortString
                } else {
                    SortType.Companion.SortTypeOptions.CHARACTER.options[0].sortString
                }
            "ORDER BY $sortString"
        } ?: ""

        return SimpleSQLiteQuery(
            "$tableJoinString$conditionsString$sortClause", args.toArray()
        )
    }

    @Query(
        """SELECT ch.* 
            FROM character ch
            JOIN appearance ap ON ap.character = ch.characterId
            JOIN story sy ON ap.story = sy.storyId
            WHERE sy.issue = :issueId""")
    abstract fun getCharacterByIssue(issueId: Int): Flow<List<Character>>

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

//    fun getCharacterIdsByFilter(filter: SearchFilter) {
//        val t = listOf(filter.mSeries, filter.mCreators, filter.mCharacter, filter.mTextFilter,
//                       filter.mPublishers, Pair(filter.mStartDate, filter.mEndDate), filter.mMyCollection)
//
//        val issuesInMyCollection = CollectionDao().
//    }
}
