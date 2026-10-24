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
package dev.benica.infinite_longbox.ui.filter

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import dev.benica.infinite_longbox.FilterSummary
import dev.benica.infinite_longbox.R
import dev.benica.infinite_longbox.SearchFilter
import dev.benica.infinite_longbox.SortType
import dev.benica.infinite_longbox.database.models.BaseCollection
import dev.benica.infinite_longbox.database.models.DateFilter
import dev.benica.infinite_longbox.database.models.FilterItem
import dev.benica.infinite_longbox.database.models.FilterType
import dev.benica.infinite_longbox.fragments_view_models.FilterViewModel
import dev.benica.infinite_longbox.repository.Repository
import dev.benica.infinite_longbox.ui.dialogs.DateDialog
import dev.benica.infinite_longbox.ui.dialogs.showDialog
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.reflect.KClass

/**
 * What the filter sheet shows and does: the filter it follows, the line it summarises it in, and
 * what its controls ask for (turned into changes of the filter). The sheet's dragging and look are
 * [FilterSheetLayer]'s.
 *
 * @param onUpdating the filter's items are (false) or aren't (true) being updated
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FilterSheetHost(
    private val activity: ComponentActivity,
    private val viewModel: FilterViewModel,
    onUpdating: (isHidden: Boolean) -> Unit,
) {
    private var currFilter by mutableStateOf(SearchFilter())
    private var summary by mutableStateOf("")

    /** Opens the sheet: set by the layer that holds it. */
    var onOpen: () -> Unit = {}

    /** How many issues are in the collection the filter is showing (see [collectionLabel]). */
    private var collectionCount: Int? = null

    init {
        // The count follows whichever collection the filter shows, and changes as issues are added
        activity.lifecycleScope.launch {
            viewModel.filter.asFlow()
                .map { it.mCollectionId }
                .distinctUntilChanged()
                .flatMapLatest { Repository.get().collectionSize(it) }
                .collect {
                    collectionCount = it
                    showSummary(currFilter)
                }
        }

        viewModel.filter.observe(activity) { filter ->
            currFilter = filter
            showSummary(filter)
        }

        viewModel.updateComplete.observe(activity) { onUpdating(it) }
    }

    @Composable
    fun Sheet(slide: Float, expanded: Boolean) {
        FilterSheet(currFilter, summary, slide, expanded, actions)
    }

    /** "My Collection (286)": the collection the list is limited to, and how many issues it holds. */
    private fun collectionLabel(filter: SearchFilter): String {
        val name = activity.getString(
            if (filter.mCollectionId == BaseCollection.WISH_LIST.id) R.string.filter_summary_wish_list
            else R.string.filter_summary_my_collection
        )
        return collectionCount?.let { "$name ($it)" } ?: name
    }

    /** The collapsed sheet's line saying what the list is filtered by. */
    private fun showSummary(filter: SearchFilter) {
        summary = FilterSummary.of(
            myCollection = filter.mMyCollection,
            myCollectionLabel = collectionLabel(filter),
            missingCover = filter.mMissingCover,
            missingStorage = filter.mMissingStorage,
            missingCoverLabel = activity.getString(R.string.filter_no_cover),
            missingStorageLabel = activity.getString(R.string.filter_not_stored),
            series = filter.mSeries?.series?.seriesName,
            character = filter.mCharacter?.name,
            creators = filter.mCreators.map { it.name },
            publishers = filter.mPublishers.map { it.publisher },
            startYear = filter.mStartDate.takeIf { it != SearchFilter.MIN_DATE }?.year,
            endYear = filter.mEndDate.takeIf { it != SearchFilter.MAX_DATE }?.year,
            text = filter.mTextFilter?.text,
            creatorsLabel = { activity.resources.getQuantityString(R.plurals.filter_summary_creators, it, it) },
            publishersLabel = { activity.resources.getQuantityString(R.plurals.filter_summary_publishers, it, it) },
        )
    }

    private val actions = object : FilterSheetActions {
        override fun myCollection(on: Boolean) = viewModel.myCollection(on)
        override fun wishList(on: Boolean) = viewModel.wishList(on)
        override fun missingCover(on: Boolean) = viewModel.missingCover(on)
        override fun missingStorage(on: Boolean) = viewModel.missingStorage(on)
        override fun showVariants(on: Boolean) = viewModel.showVariants(on)
        override fun nextView() = viewModel.nextViewOption()
        override fun sortBy(sort: SortType) = viewModel.setSortOption(sort)
        override fun add(item: FilterItem) = viewModel.addFilterItem(item)
        override fun remove(item: FilterItem) = viewModel.removeFilterItem(item)
        override fun setFilterType(type: KClass<out FilterType>) = viewModel.setFilterType(type)
        override fun pickDate(isStart: Boolean) = this@FilterSheetHost.pickDate(isStart)
        override fun setDate(date: LocalDate, isStart: Boolean) = viewModel.addFilterItem(DateFilter(date, isStart))
        override fun openHeader() = onOpen()
        override suspend fun options(text: String) = viewModel.searchOptions(text)
    }

    private fun pickDate(isStart: Boolean) {
        val minDate = if (isStart) LocalDate.of(1900, 1, 1) else currFilter.mStartDate
        val maxDate = if (isStart) currFilter.mEndDate else LocalDate.now()
        val current = if (isStart) currFilter.mStartDate else currFilter.mEndDate

        activity.showDialog { dismiss ->
            DateDialog(current, minDate, maxDate, { actions.setDate(it, isStart) }, dismiss)
        }
    }
}
