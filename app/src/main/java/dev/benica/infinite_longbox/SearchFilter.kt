/*
 * Copyright (c) 2023. Wesley T. Benica
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.benica.infinite_longbox

import android.annotation.SuppressLint
import dev.benica.infinite_longbox.InfiniteLongboxApplication.Companion.context
import dev.benica.infinite_longbox.database.models.*
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
        creators = filter.mCreators,
        series = filter.mSeries,
        publishers = filter.mPublishers,
        startDate = filter.mStartDate,
        endDate = filter.mEndDate,
        myCollection = filter.mMyCollection,
        sortType = filter.mSortType,
        textFilter = filter.mTextFilter,
        showVariants = filter.mShowVariants,
        character = filter.mCharacter,
        viewOptionsIndex = filter.mViewOptionsIndex
    )

    override fun equals(other: Any?): Boolean {
        return other is SearchFilter && hashCode() == other.hashCode()
    }

    val needsStoryTable: Boolean
        get() = hasCreator() || hasCharacter()
    private var mShowIssues: Boolean = false
    var mCreators: Set<Creator> = creators ?: setOf()
    var mSeries: FullSeries? = series
        set(value) {
            val oldOptions = getSortOptions()
            field = value
            val newOptions = getSortOptions()
            if (oldOptions != newOptions) {
                mSortType = newOptions[0]
            }
        }
    var mPublishers: Set<Publisher> = publishers ?: setOf()
    var mStartDate: LocalDate = startDate ?: LocalDate.MIN
        get() = when (field) {
            LocalDate.MIN -> MIN_DATE
            LocalDate.MAX -> MAX_DATE
            else -> field
        }
    var mEndDate: LocalDate = endDate ?: LocalDate.MAX
        get() = when (field) {
            LocalDate.MIN -> MIN_DATE
            LocalDate.MAX -> MAX_DATE
            else -> field
        }
    var mMyCollection: Boolean = myCollection
    var mSortType: SortType? = sortType ?: getSortOptions()[0]
    var mTextFilter: TextFilter? = textFilter
    var mShowVariants: Boolean = showVariants
    var mCharacter: Character? = character

    private var mViewOptionsIndex = viewOptionsIndex
        get() = field % mViewOptions.size
        set(value) {
            field = value
            if (mSortType !in getSortOptions()) {
                mSortType = getSortOptions()[0]
            }
        }

    private val mViewOptions: List<KClass<out ListItem>>
        get() = if (mSeries != null) {
            listOf(FullIssue::class, Character::class, FullCreator::class)
        } else {
            listOf(FullSeries::class, Character::class, FullCreator::class)
        }

    val mViewOption: KClass<out ListItem>
        get() = mViewOptions[mViewOptionsIndex]

    fun nextViewOption() {
        mViewOptionsIndex++
    }

    private fun isEmpty() = mCreators.isEmpty() && mSeries == null && mCharacter == null &&
            !hasDateFilter() && mPublishers.isEmpty() && !mMyCollection && mTextFilter == null

    fun isNotEmpty() = !isEmpty()

    fun hasCreator() = mCreators.isNotEmpty()

    fun hasPublisher() = mPublishers.isNotEmpty()

    fun hasDateFilter() = mStartDate != MIN_DATE || mEndDate != MAX_DATE
    fun hasCharacter() = mCharacter != null

    fun hasSeries(): Boolean = mSeries != null

    fun addFilter(vararg items: FilterItem) {
        items.forEach { item: FilterItem ->
            if (item is FilterModel) {
                mTextFilter = null
            }
            when (item) {
                is FullSeries -> addSeries(item)
                is Creator -> addCreator(item)
                is Publisher -> addPublisher(item)
                is TextFilter -> addTextFilter(item)
                is NameDetail -> {
                    // maybe should add namedetail as option. if someone wanted to find uses of
                    // alias specifically. How to differentiate though? creator and namedetail don't
                    // describe what sets them apart
                }
                is Character -> addCharacter(item)
                is DateFilter -> addDateFilter(item)
            }
        }
    }

    private fun addSeries(series: FullSeries) {
        this.mSeries = series
    }

    private fun addCreator(vararg creator: Creator) {
        val oldOptions = getSortOptions()
        val newCreators = mCreators + creator.toSet()
        val newOptions = getSortOptions()
        if (oldOptions != newOptions) {
            mSortType = newOptions[0]
        }
        if (this.mViewOption == NameDetailAndCreator::class) {
            this.mViewOptionsIndex = 0
        }

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
        val oldOptions = getSortOptions()
        mCharacter = character
        val newOptions = getSortOptions()
        if (oldOptions != newOptions) {
            mSortType = newOptions[0]
        }
        if (this.mViewOption == Character::class) {
            this.mViewOptionsIndex = 0
        }
    }

    private fun addDateFilter(dateFilter: DateFilter) {
        when (dateFilter.isStart) {
            true -> mStartDate = dateFilter.date
            false -> mEndDate = dateFilter.date
        }
    }

    // TODO: CvND
    fun removeFilter(vararg items: FilterItem) {
        items.forEach { item ->
            when (item) {
                is FullSeries -> removeSeries()
                is Creator -> removeCreator(item)
                is Publisher -> removePublisher(item)
                is TextFilter -> removeTextFilter(item)
                is NameDetail -> {
                }
                is Character -> removeCharacter()
                is DateFilter -> Unit
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

    val isComplex = hasCreator() || hasCharacter() || mMyCollection

    fun getSortOptions(): List<SortType> {
        return when (mViewOption) {
            Character::class -> SortType.Companion.SortTypeOptions.CHARACTER.options
            FullIssue::class -> SortType.Companion.SortTypeOptions.ISSUE.options
            FullSeries::class -> when (isComplex) {
                true -> SortType.Companion.SortTypeOptions.SERIES_COMPLEX.options
                false -> SortType.Companion.SortTypeOptions.SERIES.options
            }
            FullCreator::class -> SortType.Companion.SortTypeOptions.CREATOR.options
            else -> throw IllegalStateException("illegal view type: ${mViewOption.simpleName}")
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
                "${mPublishers.size} MyCol: $mMyCollection T: ${mTextFilter?.text} ${mCharacter?.name} $mStartDate $mEndDate"

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

    companion object {
        val MIN_DATE: LocalDate = LocalDate.of(1900, 1, 1)
        val MAX_DATE: LocalDate = LocalDate.now()
    }
}

class SortType(
    val tag: String,
    val sortColumn: String,
    private val table: String?,
    var order: SortOrder,
    private val sortColumn2: String? = null,
    private val table2: String? = null,
    private var order2: SortOrder? = null,
) : Serializable {


    constructor(other: SortType) : this(
        other.tag,
        other.sortColumn,
        other.table,
        other.order,
        other.sortColumn2,
        other.table2,
        other.order2
    )

    val sortString: String
        get() {
            val primarySort =
                """${if (table != null) "$table." else ""}$sortColumn ${order.option}"""

            val o2 = order2
            val secondarySort: String =
                if (sortColumn2 != null && table2 != null && o2 != null)
                    ", $table2.$sortColumn2 ${o2.option}"
                else
                    ""

            return """$primarySort$secondarySort
            """
        }

    override fun toString(): String = tag

    fun toggle(): SortType {
        order = flipSortOrder(order)
        order2 = order2?.let { flipSortOrder(it) }

        return this
    }

    private fun flipSortOrder(sortOrder: SortOrder) = when (sortOrder) {
        SortOrder.ASC -> SortOrder.DESC
        SortOrder.DESC -> SortOrder.ASC
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
        result = 31 * result + table.hashCode()
        result = 31 * result + order.hashCode()
        result = 31 * result + (table2?.hashCode() ?: 0)
        result = 31 * result + (order2?.hashCode() ?: 0)
        return result
    }

    enum class SortOrder(val option: String, val icon: Int, val contentDescription: String) :
        Serializable {
        ASC("ASC", R.drawable.icon_arrow_up, "Sort Ascending"),
        DESC("DESC", R.drawable.icon_arrow_down, "Sort Descending")
    }

    @ExperimentalCoroutinesApi
    companion object {
        @SuppressLint("StaticFieldLeak")

        fun List<SortType>.containsSortType(elem: SortType): Boolean {
            return this.contains(elem) or this.contains(SortType(elem).toggle())
        }

        val sortTypeSeriesName = SortType(
            context!!.getString(R.string.sort_type_series_name),
            context!!.getString(R.string.column_sort_name),
            "ss",
            SortOrder.ASC
        )

        // TODO: getting an option gets the same object. so if the sort order is changed in the
        //  filter, trying to get a default sortorder doesn't work. Need to figure out when I
        //  want it to be the same object and when I don't
        enum class SortTypeOptions(val options: List<SortType>) {
            SERIES(
                listOf(
                    sortTypeSeriesName,
                    SortType(
                        context!!.getString(R.string.sort_type_start_date),
                        context!!.getString(R.string.column_start_date),
                        "ss",
                        SortOrder.DESC
                    )
                )
            ),
            SERIES_COMPLEX(
                listOf(
                    sortTypeSeriesName,
                    SortType(
                        "Date",
                        "coverDate",
                        "ie",
                        SortOrder.DESC,
                        "startDate",
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