package com.wtb.comiccollector

import android.annotation.SuppressLint
import androidx.fragment.app.Fragment
import com.wtb.comiccollector.database.models.Creator
import com.wtb.comiccollector.database.models.FilterOption
import com.wtb.comiccollector.database.models.Publisher
import com.wtb.comiccollector.database.models.Series
import com.wtb.comiccollector.fragments.IssueListFragment
import com.wtb.comiccollector.fragments.SeriesListFragment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.io.Serializable
import java.time.LocalDate

const val ARG_FILTER = "Filter"

private const val TAG = APP + "Filter_SortChipGroup"

@ExperimentalCoroutinesApi
class SearchFilter(
    creators: Set<Creator>? = null,
    series: Series? = null,
    publishers: Set<Publisher>? = null,
    startDate: LocalDate? = null,
    endDate: LocalDate? = null,
    myCollection: Boolean = true,
    sortType: SortType? = null,
    textFilter: TextFilter? = null,
    showVariants: Boolean = false
) : Serializable {

    constructor(filter: SearchFilter) : this(
        filter.mCreators,
        filter.mSeries,
        filter.mPublishers,
        filter.mStartDate,
        filter.mEndDate,
        filter.mMyCollection,
        filter.mSortType,
        filter.mTextFilter,
        filter.mShowVariants
    )

    override fun equals(other: Any?): Boolean {
        return other is SearchFilter && hashCode() == other.hashCode()
    }

    var mShowIssues: Boolean = false
    var mCreators: Set<Creator> = creators ?: setOf()
    var mSeries: Series? = series
        set(value) {
            // sets selected sort option to default if it's not of the same type
            if (getSortOptions(value) != getSortOptions()) {
                mSortType = getSortOptions(value)[0]
            }
            field = value
        }
    var mPublishers: Set<Publisher> = publishers ?: setOf()
    var mStartDate: LocalDate = startDate ?: LocalDate.MIN
    var mEndDate: LocalDate = endDate ?: LocalDate.MAX
    var mMyCollection: Boolean = myCollection
    var mSortType: SortType = sortType ?: getSortOptions()[0]
    var mTextFilter: TextFilter? = textFilter
    var mShowVariants: Boolean = showVariants

    fun hasCreator() = mCreators.isNotEmpty()
    fun returnsIssueList() = mSeries != null || mShowIssues
    fun hasPublisher() = mPublishers.isNotEmpty()
    fun hasDateFilter() = mStartDate != LocalDate.MIN || mEndDate != LocalDate.MAX
    fun isEmpty(): Boolean {
        return mCreators.isEmpty() && mSeries == null && mPublishers.isEmpty() && mStartDate ==
                LocalDate.MIN && mEndDate == LocalDate.MAX && !mMyCollection
    }
    fun isNotEmpty(): Boolean = !isEmpty()


    fun addFilter(vararg items: FilterOption) {
        items.forEach { item ->
            when (item) {
                is Series     -> addSeries(item)
                is Creator    -> addCreator(item)
                is Publisher  -> addPublisher(item)
                is TextFilter -> addTextFilter(item)
            }
        }
    }

    private fun addCreator(vararg creator: Creator) {
        val newCreators = mCreators + creator.toSet()
        mCreators = newCreators
    }

    private fun addSeries(series: Series) {
        this.mSeries = series
    }

    private fun addPublisher(vararg publisher: Publisher) {
        val newPublishers = mPublishers + publisher.toSet()
        mPublishers = newPublishers
    }

    private fun addTextFilter(item: TextFilter) {
        mTextFilter = item
    }

    fun removeFilter(vararg items: FilterOption) {
        items.forEach { item ->
            when (item) {
                is Series     -> removeSeries()
                is Creator    -> removeCreator(item)
                is Publisher  -> removePublisher(item)
                is TextFilter -> removeTextFilter(item)
            }
        }
    }

    private fun removeCreator(vararg creator: Creator) {
        val newCreators = mCreators - creator.toSet()
        mCreators = newCreators
    }

    private fun removeSeries() {
        this.mSeries = null
    }

    private fun removePublisher(vararg publisher: Publisher) {
        val newPublishers = mPublishers - publisher.toSet()
        mPublishers = newPublishers
    }

    private fun removeTextFilter(item: TextFilter) {
        if (item == mTextFilter)
            mTextFilter = null
    }

    fun getFragment(): Fragment =
        when (mSeries) {
            null -> SeriesListFragment.newInstance()
            else -> IssueListFragment.newInstance()
        }

    fun getSortOptions(series: Series? = mSeries): List<SortType> =
        when (series) {
            null -> SortType.Companion.ItemSort.SERIES.options
            else -> SortType.Companion.ItemSort.ISSUE.options
        }

    fun getReturnTypes(): List<ReturnType> {
        val result: MutableList<ReturnType> = mutableListOf()

        if (mSeries == null) {
            result.add(ReturnType.SERIES)
        }

        if (mCreators.isNotEmpty() || mPublishers.isNotEmpty() || mSeries != null) {
            result.add(ReturnType.ISSUE)
        }

        return result
    }

    fun getAll(): Set<FilterOption> {
        val series = mSeries?.let { setOf(it) } ?: emptySet()
        val textFilter = mTextFilter?.let { setOf(it) } ?: emptySet()
        return mCreators + mPublishers + series + textFilter
    }

    override fun hashCode(): Int {
        var result = mCreators.hashCode()
        result = 31 * result + (mSeries?.hashCode() ?: 0)
        result = 31 * result + mPublishers.hashCode()
        result = 31 * result + mStartDate.hashCode()
        result = 31 * result + mEndDate.hashCode()
        result = 31 * result + mMyCollection.hashCode()
        result = 31 * result + mSortType.hashCode()
        result = 31 * result + mTextFilter.hashCode()
        result = 31 * result + mShowVariants.hashCode()
        return result
    }

    override fun toString(): String =
        "Series: $mSeries Creators: ${mCreators.size} Pubs: " +
                "${mPublishers.size} MyCol: $mMyCollection T: ${mTextFilter?.text}"
}

class SortType(
    val tag: String,
    val sortColumn: String,
    var order: SortOrder
) : Serializable {

    constructor(other: SortType) : this(
        other.tag,
        other.sortColumn,
        other.order
    )

    val sortString: String
        get() = "$sortColumn ${order.option}"

    override fun toString(): String = tag

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SortType

        if (tag != other.tag) return false
        if (sortColumn != other.sortColumn) return false
        if (order != other.order) return false
        if (sortString != other.sortString) return false

        return true
    }

    override fun hashCode(): Int {
        var result = tag.hashCode()
        result = 31 * result + sortColumn.hashCode()
        result = 31 * result + when (order) {
            SortOrder.ASC  -> true.hashCode()
            SortOrder.DESC -> false.hashCode()
        }
        result = 31 * result + sortString.hashCode()
        return result
    }

    enum class SortOrder(val option: String, val icon: Int) : Serializable {
        ASC("ASC", R.drawable.arrow_up_24),
        DESC("DESC", R.drawable.arrow_down_24)
    }

    @ExperimentalCoroutinesApi
    companion object {
        @SuppressLint("StaticFieldLeak")
        val context = ComicCollectorApplication.context

        enum class ItemSort(val options: List<SortType>) {
            SERIES(
                listOf(
                    SortType(
                        context!!.getString(R.string.sort_type_series_name),
                        context.getString(R.string.column_sort_name),
                        SortOrder.ASC
                    ),
                    SortType(
                        context.getString(R.string.sort_type_start_date),
                        context.getString(R.string.column_start_date),
                        SortOrder.DESC
                    )
                )
            ),

            ISSUE(
                listOf(
                    SortType(
                        context!!.getString(R.string.sort_type_issue_number),
                        context.getString(R.string.column_issue_num),
                        SortOrder.ASC
                    ),
                    SortType(
                        context.getString(R.string.sort_type_release_date),
                        context.getString(R.string.column_release_date),
                        SortOrder.DESC
                    )
                )
            )
        }
    }
}

//val SERIES_SORT_TYPES: List<SortType> = listOf(
//    SortType("Series Name", "sortName", SortType.SortOrder.ASC),
//    SortType("Start Date", "startDate", SortType.SortOrder.DESC)
//)
//
//val ISSUE_SORT_TYPES: List<SortType> = listOf(
//    SortType("Issue Number", "issueNum", SortType.SortOrder.ASC),
//    SortType("Release Date", "releaseDate", SortType.SortOrder.DESC)
//)
//
data class TextFilter(val text: String) : FilterOption {
    override val compareValue: String
        get() = text

    override fun toString(): String = "\"$text\""
}

enum class ReturnType {
    SERIES, ISSUE, CREATOR, PUBLISHER, CHARACTER
}

