package com.wtb.comiccollector.database.daos

import android.util.Log
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.SearchFilter
import com.wtb.comiccollector.SortType
import com.wtb.comiccollector.SortType.Companion.containsSortType
import com.wtb.comiccollector.database.models.Character
import com.wtb.comiccollector.database.models.Creator
import com.wtb.comiccollector.database.models.FullCreator
import com.wtb.comiccollector.database.models.ids
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow

@ExperimentalCoroutinesApi
@Dao
abstract class CreatorDao : BaseDao<Creator>("creator") {

    @Query("SELECT * FROM creator ORDER BY sortName ASC")
    abstract fun getAll(): Flow<List<Creator>>

    // FLOW FUNCTIONS
    @RawQuery(observedEntities = [Creator::class])
    abstract fun getCreatorsByQuery(query: SupportSQLiteQuery): Flow<List<Creator>>

    fun getCreatorsByFilter(filter: SearchFilter): Flow<List<Creator>> {

        val query = getCreatorQuery(filter)

        return getCreatorsByQuery(query)
    }

    // PAGING SOURCE FUNCTIONS
    @RawQuery(observedEntities = [Character::class])
    abstract fun getCreatorsByQueryPagingSource(query: SupportSQLiteQuery): PagingSource<Int, FullCreator>

    fun getCreatorsByFilterPagingSource(filter: SearchFilter): PagingSource<Int, FullCreator> {
        val query = getCreatorQuery(filter)

        return getCreatorsByQueryPagingSource(query)
    }

    companion object {
        private const val TAG = APP + "CreatorDao"

        private fun getCreatorQuery(filter: SearchFilter): SimpleSQLiteQuery {

            val tableJoinString = StringBuilder()
            val conditionsString = StringBuilder()
            val args: ArrayList<Any> = arrayListOf()
            fun connectWord(): String = if (conditionsString.isEmpty()) "WHERE" else "AND"

            //language=RoomSql
            tableJoinString.append(
                """SELECT DISTINCT cr.* 
                FROM creator cr 
                """
            )

            if (filter.isNotEmpty()) {
                tableJoinString.append(
                    """JOIN nameDetail nd ON cr.creatorId = nd.creator 
                    JOIN credit ct ON ct.nameDetail = nd.nameDetailId 
                    """
                )
            }

            //language=RoomSql
            filter.mSeries?.let {
                conditionsString.append(
                    """${connectWord()} ct.series = ${it.series.seriesId}
                    """
                )
            }

            if (filter.hasPublisher()) {
                val publisherList = modelsToSqlIdString(filter.mPublishers)
                tableJoinString.append(
                    """JOIN series ss on ct.series = ss.seriesId 
                    """
                )

                conditionsString.append(
                    """${connectWord()} ss.publisher IN $publisherList  
                    """
                )
            }

            if (filter.hasCreator()) {
                tableJoinString.append(
                    """JOIN story sy ON sy.storyId = ct.story
                    """
                )

                for (creatorId in filter.mCreators.ids) {
                    conditionsString.append(
                        """${connectWord()} sy.storyId IN (
                            SELECT ct.story
                            FROM credit ct
                            WHERE ct.nameDetail IN (
                                SELECT nl.nameDetailId
                                FROM namedetail nl
                                WHERE nl.creator = $creatorId)
                        )
                        """
                    )
                }
            }

            if (filter.hasDateFilter() || filter.mMyCollection) {
                tableJoinString.append("""JOIN issue ie on ct.issue = ie.issueId 
                """)

                if (filter.hasDateFilter()) {
                    //language=RoomSql
                    conditionsString.append(
                        """${connectWord()} ie.releaseDate <= '${filter.mEndDate}'
                        AND ie.releaseDate > '${filter.mStartDate}'
                        """
                    )
                }

                if (filter.mMyCollection) {
                    conditionsString.append(
                        """${connectWord()} ie.issueId IN (
                            SELECT issue
                            FROM mycollection
                        ) 
                        """
                    )
                }
            }

            filter.mTextFilter?.let { textFilter ->
                val text = textFilterToString(textFilter.text)
                conditionsString.append(
                    """${connectWord()} nd.name LIKE ?
                    OR cr.name LIKE ?
                    """
                )
                args.addAll(listOf(text, text))
            }

            if (filter.hasCreator()) {
                conditionsString.append(
                    """${connectWord()} cr.creatorId NOT IN ${modelsToSqlIdString(filter.mCreators)}
                    """
                )
            }


            val allArgs: ArrayList<Any> = arrayListOf()
            args.forEach { allArgs.add(it) }
            args.forEach { allArgs.add(it) }

            val sortClause: String = filter.mSortType?.let {
                val isValid =
                    SortType.Companion.SortTypeOptions.CREATOR.options.containsSortType(it)
                val sortString: String =
                    if (isValid) {
                        it.sortString
                    } else {
                        SortType.Companion.SortTypeOptions.CREATOR.options[0].sortString
                    }
                "ORDER BY $sortString"
            } ?: ""

            val tableJoinString2 = sqlCreditToExCredit(tableJoinString)
            val conditionsString2 = sqlCreditToExCredit(conditionsString)
            val simpleSQLiteQuery = SimpleSQLiteQuery(
                """
                    SELECT *
                    FROM ( 
                        $tableJoinString$conditionsString 
                        UNION 
                        $tableJoinString2$conditionsString2
                    )
                    $sortClause
                """,
                allArgs.toArray()
            )
            Log.d(TAG, "XYZ ${simpleSQLiteQuery.sql}")
            return simpleSQLiteQuery
        }

        private fun sqlCreditToExCredit(tableJoinString: StringBuilder) =
            tableJoinString.replace(Regex("credit"), "excredit")
                .replace(Regex(" ct"), " ect")
    }
}