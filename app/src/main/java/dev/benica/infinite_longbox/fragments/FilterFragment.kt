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

package dev.benica.infinite_longbox.fragments

import android.content.Context
import android.os.Build
import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dev.benica.infinite_longbox.*
import dev.benica.infinite_longbox.database.models.*
import dev.benica.infinite_longbox.fragments_view_models.FilterViewModel
import dev.benica.infinite_longbox.repository.Repository
import dev.benica.infinite_longbox.ui.filter.FilterSheet
import dev.benica.infinite_longbox.ui.filter.FilterSheetActions
import dev.benica.infinite_longbox.ui.theme.InfiniteLongboxTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.reflect.KClass

@Suppress("unused")
private const val TAG = APP + "FilterFragment"

/** The bottom sheet: what the list is filtered by when closed, and the controls for it when open. */
@ExperimentalCoroutinesApi
class FilterFragment : Fragment() {

    private val viewModel: FilterViewModel by viewModels({ requireActivity() })
    private var callback: FilterFragmentCallback? = null

    private var currFilter by mutableStateOf(SearchFilter())
    private var summary by mutableStateOf("")
    private var slide by mutableFloatStateOf(0f)
    private var expanded by mutableStateOf(false)

    /** How many issues are in the collection the filter is showing (see [collectionLabel]). */
    private var collectionCount: Int? = null

    internal var visibleState: Int = BottomSheetBehavior.STATE_EXPANDED
        set(value) {
            field = value
            expanded = value == BottomSheetBehavior.STATE_EXPANDED
            // The slide callback doesn't always come before the state settles (a restored sheet)
            when (value) {
                BottomSheetBehavior.STATE_EXPANDED -> slide = 1f
                BottomSheetBehavior.STATE_COLLAPSED, BottomSheetBehavior.STATE_HIDDEN -> slide = 0f
            }
        }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = context as FilterFragmentCallback?
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.fragment_filter, container, false)
        view.clipToOutline = true

        val filterView = view.findViewById<ViewGroup>(R.id.layout_filter_fragment)
        ViewCompat.setOnApplyWindowInsetsListener(filterView) { v, insets ->
            val posBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            v.updatePadding(bottom = posBottom)

            val imeInsetBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> { bottomMargin = imeInsetBottom }

            insets
        }

        view.findViewById<ComposeView>(R.id.sheet_compose).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                InfiniteLongboxTheme {
                    FilterSheet(currFilter, summary, slide, expanded, actions)
                }
            }
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // The count follows whichever collection the filter shows, and changes as issues are added
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.filter.asFlow()
                .map { it.mCollectionId }
                .distinctUntilChanged()
                .flatMapLatest { Repository.get().collectionSize(it) }
                .collect {
                    collectionCount = it
                    showSummary(currFilter)
                }
        }

        viewModel.filter.observe(viewLifecycleOwner) { filter ->
            currFilter = filter
            showSummary(filter)
        }

        viewModel.updateComplete.observe(viewLifecycleOwner) {
            callback?.setProgressBar(it)
        }
    }

    /** "My Collection (286)": the collection the list is limited to, and how many issues it holds. */
    private fun collectionLabel(filter: SearchFilter): String {
        val name = getString(
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
            missingCoverLabel = getString(R.string.filter_no_cover),
            missingStorageLabel = getString(R.string.filter_not_stored),
            series = filter.mSeries?.series?.seriesName,
            character = filter.mCharacter?.name,
            creators = filter.mCreators.map { it.name },
            publishers = filter.mPublishers.map { it.publisher },
            startYear = filter.mStartDate.takeIf { it != SearchFilter.MIN_DATE }?.year,
            endYear = filter.mEndDate.takeIf { it != SearchFilter.MAX_DATE }?.year,
            text = filter.mTextFilter?.text,
            creatorsLabel = { resources.getQuantityString(R.plurals.filter_summary_creators, it, it) },
            publishersLabel = { resources.getQuantityString(R.plurals.filter_summary_publishers, it, it) },
        )
    }

    internal fun onSlide(slideOffset: Float) {
        slide = slideOffset.coerceIn(0f, 1f)
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
        override fun pickDate(isStart: Boolean) = this@FilterFragment.pickDate(isStart)
        override fun setDate(date: LocalDate, isStart: Boolean) = viewModel.addFilterItem(DateFilter(date, isStart))
        override fun openHeader() { callback?.onHandleClick() }
        override suspend fun options(text: String) = viewModel.searchOptions(text)
    }

    interface FilterFragmentCallback {
        fun onHandleClick()
        fun setProgressBar(isHidden: Boolean)
    }

    companion object {
        fun newInstance() = FilterFragment()
    }

    private fun pickDate(isStart: Boolean) {
        val reqKey = if (isStart) RESULT_DATE_PICKER_START else RESULT_DATE_PICKER_END
        parentFragmentManager.setFragmentResultListener(
            reqKey,
            viewLifecycleOwner
        ) { _, result ->
            val resultDate: LocalDate? = if (Build.VERSION.SDK_INT >= TIRAMISU) {
                result.getSerializable(ARG_DATE, LocalDate::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.getSerializable(ARG_DATE) as LocalDate?
            }
            resultDate?.let { actions.setDate(it, isStart) }
        }

        val minDate = if (isStart) LocalDate.of(1900, 1, 1) else currFilter.mStartDate
        val maxDate = if (isStart) currFilter.mEndDate else LocalDate.now()
        val current = if (isStart) currFilter.mStartDate else currFilter.mEndDate

        DatePickerFragment.newInstance(current, minDate, maxDate, reqKey).apply {
            show(this@FilterFragment.parentFragmentManager, DIALOG_DATE)
        }
    }
}
