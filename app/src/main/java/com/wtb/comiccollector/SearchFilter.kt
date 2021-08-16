package com.wtb.comiccollector

import android.annotation.SuppressLint
import android.util.Log
import com.wtb.comiccollector.ComicCollectorApplication.Companion.context
import com.wtb.comiccollector.database.models.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.io.Serializable
import java.time.LocalDate
import kotlin.reflect.KClass

const val ARG_FILTER = "Filter"

private const val TAG = APP + "Filter_SortChipGroup"

@ExperimentalCoroutinesApi
class SearchFilter(
    creators: Set<Creator>? = null,
    series: FullSeries? = null,
    publishers: Set<Publisher>? = null,
    startDate: LocalDate? = null,
    endDate: LocalDate? = null,
    myCollection: Boolean = true,
    sortType: SortType? = null,
    textFilter: TextFilter? = null,
    showVariants: Boolean = false,
    character: Character? = null,
    viewOptionsIndex: Int = 0,
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
        filter.mShowVariants,
        filter.mCharacter,
        filter.mViewOptionsIndex
    )

    override fun equals(other: Any?): Boolean {
        return other is SearchFilter && hashCode() == other.hashCode()
    }

    val needsStoryTable: Boolean
    get() = hasCreator() || hasCharacter()
    var mShowIssues: Boolean = false
    var mCreators: Set<Creator> = creators ?: setOf()
    var mSeries: FullSeries? = series
        set(value) {
            val oldOptions = getSortOptions()
            field = value
            val newOptions = getSortOptions()
            if (oldOptions != newOptions) {
                Log.d(TAG, "SETTING mSortType to ${newOptions[0]}")
                mSortType = newOptions[0]
            } else {
                Log.d(TAG, "NOT!!!! SETTING mSortType to ${newOptions[0]}")
            }
        }
    var mPublishers: Set<Publisher> = publishers ?: setOf()
    var mStartDate: LocalDate = startDate ?: LocalDate.MIN
    var mEndDate: LocalDate = endDate ?: LocalDate.MAX
    var mMyCollection: Boolean = myCollection
    var mSortType: SortType? = sortType ?: getSortOptions()[0]
    var mTextFilter: TextFilter? = textFilter
    var mShowVariants: Boolean = showVariants
    var mCharacter: Character? = character

    var mViewOptionsIndex = viewOptionsIndex
        get() = field % mViewOptions.size

    val mViewOptions: List<KClass<out ListItem>>
        get() = if (mSeries != null) {
            listOf(FullIssue::class, Character::class, NameDetailAndCreator::class)
        } else {
            listOf(FullSeries::class, Character::class, NameDetailAndCreator::class)
        }

    val mViewOption: KClass<out ListItem>
        get() = mViewOptions[mViewOptionsIndex]

    fun nextViewOption() {
        mViewOptionsIndex++
    }

    fun isEmpty() = mCreators.isEmpty() && mSeries == null && mCharacter == null &&
            !hasDateFilter() && mPublishers.isEmpty() && !mMyCollection

    fun isNotEmpty() = !isEmpty()

    fun hasCreator() = mCreators.isNotEmpty()

    fun hasPublisher() = mPublishers.isNotEmpty()

    fun hasDateFilter() = mStartDate != LocalDate.MIN || mEndDate != LocalDate.MAX
    fun hasCharacter() = mCharacter != null

    fun hasSeries(): Boolean = mSeries != null

    fun addFilter(vararg items: FilterItem) {
        items.forEach { item ->
            when (item) {
                is FullSeries -> addSeries(item)
                is Creator    -> addCreator(item)
                is Publisher  -> addPublisher(item)
                is TextFilter -> addTextFilter(item)
                is NameDetail -> {
                    // maybe should add namedetail as option. if someone wanted to find uses of
                    // alias specifically. How to differentiate though? creator and namedetail don't
                    // describe what sets them apart
                }
                is Character  -> addCharacter(item)
            }
        }
    }

    private fun addSeries(series: FullSeries) {
        this.mSeries = series
    }

    private fun addCreator(vararg creator: Creator) {
        val newCreators = mCreators + creator.toSet()
        mCreators = newCreators
    }

    private fun addPublisher(vararg publisher: Publisher) {
        val newPublishers = mPublishers + publisher.toSet()
        mPublishers = newPublishers
    }

    private fun addTextFilter(item: TextFilter) {
        mTextFilter = item
    }

    private fun addCharacter(character: Character) {
        mCharacter = character
    }

    // TODO: CvND
    fun removeFilter(vararg items: FilterItem) {
        items.forEach { item ->
            when (item) {
                is FullSeries -> removeSeries()
                is Creator    -> removeCreator(item)
                is Publisher  -> removePublisher(item)
                is TextFilter -> removeTextFilter(item)
                is NameDetail -> {
                }
                is Character  -> removeCharacter()
            }
        }
    }

    private fun removeSeries() {
        this.mSeries = null
    }

    private fun removeCreator(vararg creator: Creator) {
        val newCreators = mCreators - creator.toSet()
        mCreators = newCreators
    }

    private fun removePublisher(vararg publisher: Publisher) {
        val newPublishers = mPublishers - publisher.toSet()
        mPublishers = newPublishers
    }

    private fun removeTextFilter(item: TextFilter) {
        if (item == mTextFilter)
            mTextFilter = null
    }

    private fun removeCharacter() {
        this.mCharacter = null
    }

    fun getSortOptions(): List<SortType> {
        return when (mViewOption) {
            Character::class            -> SortType.Companion.SortTypeOptions.CHARACTER.options
            FullIssue::class            -> SortType.Companion.SortTypeOptions.ISSUE.options
            FullSeries::class           -> SortType.Companion.SortTypeOptions.SERIES.options
            NameDetailAndCreator::class -> SortType.Companion.SortTypeOptions.CREATOR.options
            else                        -> throw IllegalStateException("illegal view type: ${mViewOption.simpleName}")
        }
    }

    fun getAll(): Set<FilterItem> {
        val series = mSeries?.let { setOf(it) } ?: emptySet()
        val textFilter = mTextFilter?.let { setOf(it) } ?: emptySet()
        val character = mCharacter?.let { setOf(it) } ?: emptySet()
        return mCreators + mPublishers + series + textFilter + character
    }

    override fun toString(): String =
        "Series: $mSeries Creators: ${mCreators.size} Pubs: " +
                "${mPublishers.size} MyCol: $mMyCollection T: ${mTextFilter?.text} ${mCharacter?.name}"

    override fun hashCode(): Int {
        var result = mShowIssues.hashCode()
        result = 31 * result + mCreators.hashCode()
        result = 31 * result + (mSeries?.hashCode() ?: 0)
        result = 31 * result + mPublishers.hashCode()
        result = 31 * result + mStartDate.hashCode()
        result = 31 * result + mEndDate.hashCode()
        result = 31 * result + mMyCollection.hashCode()
        result = 31 * result + (mSortType?.hashCode() ?: 0)
        result = 31 * result + (mTextFilter?.hashCode() ?: 0)
        result = 31 * result + mShowVariants.hashCode()
        result = 31 * result + (mCharacter?.hashCode() ?: 0)
        result = 31 * result + mViewOptionsIndex
        result = 31 * result + mViewOptions.hashCode()
        result = 31 * result + mViewOption.hashCode()
        return result
    }
}

class SortType(
    val tag: String,
    val sortColumn: String,
    val table: String?,
    var order: SortOrder,
) : Serializable {

    constructor(other: SortType) : this(
        other.tag,
        other.sortColumn,
        other.table,
        other.order
    )

    val sortString: String
        get() = "${table?.let { "${it}." } ?: ""}$sortColumn ${order.option}"

    override fun toString(): String = tag

    fun toggle(): SortType {
        order = when (order) {
            SortOrder.ASC  -> SortOrder.DESC
            SortOrder.DESC -> SortOrder.ASC
        }

        return this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SortType

        return hashCode() == other.hashCode()
    }

    override fun hashCode(): Int {
        var result = tag.hashCode()
        result = 31 * result + sortColumn.hashCode()
        result = 31 * result + (table?.hashCode() ?: 0)
        result = 31 * result + (order == SortOrder.ASC).hashCode()
        result = 31 * result + sortString.hashCode()
        return result
    }

    enum class SortOrder(val option: String, val icon: Int, val contentDescription: String) :
        Serializable {
        ASC("ASC", R.drawable.arrow_up_24, "Sort Ascending"),
        DESC("DESC", R.drawable.arrow_down_24, "Sort Descending")
    }

    @ExperimentalCoroutinesApi
    companion object {
        @SuppressLint("StaticFieldLeak")

        fun List<SortType>.containsSortType(elem: SortType): Boolean {
            return this.contains(elem) or this.contains(SortType(elem).toggle())
        }

        enum class SortTypeOptions(val options: List<SortType>) {
            SERIES(
                listOf(
                    SortType(
                        context!!.getString(R.string.sort_type_series_name),
                        context!!.getString(R.string.column_sort_name),
                        "ss",
                        SortOrder.ASC
                    ),
                    SortType(
                        context!!.getString(R.string.sort_type_start_date),
                        context!!.getString(R.string.column_start_date),
                        "ss",
                        SortOrder.DESC
                    )
                )
            ),

            ISSUE(
                listOf(
                    SortType(
                        context!!.getString(R.string.sort_type_issue_number),
                        context!!.getString(R.string.column_issue_num),
                        "ie",
                        SortOrder.ASC
                    ),
                    SortType(
                        context!!.getString(R.string.sort_type_release_date),
                        context!!.getString(R.string.column_release_date),
                        "ie",
                        SortOrder.DESC
                    )
                )
            ),
            CHARACTER(
                listOf(
                    SortType(
                        "Name",
                        "name",
                        "ch",
                        SortOrder.ASC
                    )
                )
            ),
            CREATOR(
                listOf(
                    SortType(
                        "Name",
                        "name",
                        null,
                        SortOrder.ASC
                    )
                )
            )
        }
    }
}
