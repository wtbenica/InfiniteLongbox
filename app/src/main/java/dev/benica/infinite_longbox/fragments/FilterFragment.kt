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

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.chip.ChipGroup
import dev.benica.infinite_longbox.*
import dev.benica.infinite_longbox.database.models.*
import dev.benica.infinite_longbox.fragments_view_models.FilterViewModel
import dev.benica.infinite_longbox.views.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.LocalDate

private const val TAG = APP + "FilterFragment"

@ExperimentalCoroutinesApi
class FilterFragment : Fragment(),
    SearchBar.SearchTextViewCallback,
    FilterChip.FilterChipCallback, OptionChipGroup.OptionChipGroupCallback,
    SortChipGroup.SortChipGroupCallback, DateChipGroup.DateChipGroupCallback {

    private lateinit var sections: LinearLayout
    private val viewModel: FilterViewModel by viewModels({ requireActivity() })
    private var callback: FilterFragmentCallback? = null
    private val undoQueue = ArrayDeque<Undo<*>>()
    private val disabledFilterChips = mutableSetOf<FilterChip>()

    internal var visibleState: Int = BottomSheetBehavior.STATE_EXPANDED
        set(value) {
            field = onVisibleStateUpdated(value)
        }

    private var prevFilter: SearchFilter = SearchFilter()
    private var currFilter: SearchFilter = SearchFilter()
        set(value) {
            field = onFilterUpdate(value)
        }

    // Views
    private lateinit var filterView: FrameLayout
    private lateinit var handleBox: FrameLayout
    private lateinit var handleImage: ImageView

    private lateinit var chipSections: LinearLayout

    private lateinit var dateFilterSection: LinearLayout
    private lateinit var dateChipGroup: DateChipGroup

    private lateinit var optionsSection: LinearLayout
    private lateinit var optionsChipGroup: OptionChipGroup

    private lateinit var sortSection: LinearLayout
    private lateinit var sortChipGroup: SortChipGroup

    private lateinit var filterSection: LinearLayout
    private lateinit var filterOptionsLabel: ImageView

    private lateinit var filterChipGroup: ChipGroup
    private lateinit var filterAddButton: ImageButton

    private lateinit var searchSection: ConstraintLayout
    private lateinit var searchBar: SearchBar

    private lateinit var filterTypeScrollView: HorizontalScrollView
    private lateinit var filterTypeChipGroup: ChipGroup
    private lateinit var filterChipAll: FilterTypeChip<All.Companion>
    private lateinit var filterChipSeries: FilterTypeChip<Series.Companion>
    private lateinit var filterChipCreator: FilterTypeChip<NameDetail.Companion>
    private lateinit var filterChipCharacter: FilterTypeChip<Character.Companion>
    private lateinit var filterChipPublisher: FilterTypeChip<Publisher.Companion>

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = context as FilterFragmentCallback?
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.fragment_filter, container, false)
        onCreateViewFindViews(view)
        onCreateViewInitViews()

        view.clipToOutline = true

        viewModel.filterOptions.observe(viewLifecycleOwner) { filterOptions ->
            searchBar.setAdapter(
                FilterOptionsAdapter(
                    context = requireContext(),
                    filterOptions = filterOptions
                )
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                searchBar.refreshAutoCompleteResults()
            }
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.filter.observe(
            viewLifecycleOwner
        ) { filter ->
            currFilter = filter
            sortChipGroup.update(filter)
            optionsChipGroup.update(filter)
            dateChipGroup.update(filter)
        }

        viewModel.updateComplete.observe(viewLifecycleOwner) {
            callback?.setProgressBar(it)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun onCreateViewInitViews() {
        ViewCompat.setOnApplyWindowInsetsListener(filterView) { view, insets ->
            val posBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            view.updatePadding(bottom = posBottom)


            val imeInsetBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> { bottomMargin = imeInsetBottom }

            insets
        }

        handleImage.setOnClickListener {
            callback?.onHandleClick()
        }

        sortChipGroup.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId >= 0) {
                view?.findViewById<SortChip>(checkedId)?.sortType?.let {
                    if (it != currFilter.mSortType) {
                        viewModel.setSortOption(it)
                    }
                }
            }
        }

        sortChipGroup.callback = this@FilterFragment

        filterChipGroup.removeAllViews()

        filterAddButton.setOnClickListener {
            searchSection.visibility = VISIBLE
            filterAddButton.visibility = GONE
        }

        searchBar.callbacks = this

        filterTypeChipGroup.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId >= 0) {
                view?.findViewById<FilterTypeChip<*>>(checkedId)?.let { filterChip ->
                    filterChip.type?.let { type -> viewModel.setFilterType(type) }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    searchBar.refreshAutoCompleteResults()
                }
            }
        }
    }

    private fun onCreateViewFindViews(view: View) {
        filterView = view.findViewById(R.id.layout_filter_fragment)
        handleBox = view.findViewById(R.id.layout_filter_fragment_handle)
        handleImage = view.findViewById(R.id.handle_handle)

        sections = view.findViewById(R.id.sections)
        chipSections = view.findViewById(R.id.chip_sections)
        dateFilterSection = view.findViewById(R.id.section_date_filters)
        dateChipGroup = view.findViewById(R.id.chip_group_dates)
        dateChipGroup.callback = this

        optionsSection = view.findViewById(R.id.section_options)
        optionsChipGroup = view.findViewById(R.id.chip_group_option)
        optionsChipGroup.callback = this

        sortSection = view.findViewById(R.id.section_sort)
        sortChipGroup = view.findViewById(R.id.chip_group_sort)

        filterSection = view.findViewById(R.id.section_filter)

        filterOptionsLabel = view.findViewById(R.id.image_label_filter_items)
        filterChipGroup = view.findViewById(R.id.chip_group_filter)
        filterAddButton = view.findViewById(R.id.add_filter_button)

        searchSection = view.findViewById(R.id.section_search)
        searchBar = view.findViewById(R.id.search_bar)
        searchBar.dropDownAnchor = R.id.filter_type_chip_group

        initFilterTypeChipGroup(view)
    }

    private fun initFilterTypeChipGroup(view: View) {
        filterTypeScrollView = view.findViewById(R.id.chip_group_scrollview)
        filterTypeChipGroup = view.findViewById(R.id.filter_type_chip_group)
        filterChipAll =
            view.findViewById<FilterTypeChip<All.Companion>>(R.id.filter_chip_all).apply {
                type = All.Companion::class
            }
        filterChipSeries =
            view.findViewById<FilterTypeChip<Series.Companion>>(R.id.filter_chip_series)
                .apply {
                    type = Series.Companion::class
                }
        filterChipCreator = view.findViewById<FilterTypeChip<NameDetail.Companion>>(
            R.id.filter_chip_creator
        )
            .apply {
                type = NameDetail.Companion::class
            }
        filterChipCharacter = view.findViewById<FilterTypeChip<Character.Companion>>(
            R.id.filter_chip_character
        )
            .apply {
                type = Character.Companion::class
            }
        filterChipPublisher = view.findViewById<FilterTypeChip<Publisher.Companion>>(
            R.id.filter_chip_publisher
        )
            .apply {
                type = Publisher.Companion::class
            }
    }

    internal fun onSlide(slideOffset: Float) {
        val inverseOffset = 1 - slideOffset
        handleBox.alpha = inverseOffset
        sections.alpha = slideOffset
        enableViewGroup(sections, slideOffset != 0F)
    }

    private fun enableViewGroup(vg: ViewGroup, isEnabled: Boolean = true) {
        vg.isEnabled = isEnabled
        for (child in vg.children) {
            if (child is ViewGroup) {
                enableViewGroup(child, isEnabled)
            } else {
                child.isEnabled = isEnabled
            }
        }
    }

    private fun onFilterUpdate(value: SearchFilter): SearchFilter {
        if (prevFilter != value) {
            undoQueue.add(
                Undo(function = { viewModel.setFilter(it) }, item = SearchFilter(prevFilter))
            )
            prevFilter = currFilter
        } else {
            undoQueue.removeLastOrNull()
        }

        updateFilterCard(value)

        return value
    }

    private fun onVisibleStateUpdated(value: Int): Int {
        if (value == BottomSheetBehavior.STATE_EXPANDED) {
            handleBox.visibility = GONE
            optionsChipGroup.isEnabled = true
            sortChipGroup.isEnabled = true
            filterChipGroup.isEnabled = true
        } else {
            handleBox.visibility = VISIBLE
            optionsChipGroup.isEnabled = false
            sortChipGroup.isEnabled = false
            filterChipGroup.isEnabled = false
        }

        return value
    }

    private fun updateFilterCard(value: SearchFilter) {
        val newFilters: Set<FilterItem> = value.getAll()

        filterChipGroup.removeAllViews()
        newFilters.forEach { addChip(it) }
        disabledFilterChips.forEach { filterChipGroup.addView(it) }

        if (filterChipGroup.isEmpty()) {
            collapseFilterCard()
            searchSection.visibility = VISIBLE
        } else {
            showChipsHideBox()
            expandFilterCard()
        }
    }

    private fun showChipsHideBox() {
        searchSection.visibility = GONE
        filterSection.visibility = VISIBLE
        filterAddButton.visibility = VISIBLE
    }

    private fun addChip(item: FilterItem) {
        val chip = FilterChip(context, item, this)
        filterChipGroup.addView(chip)
    }

    private fun expandFilterCard() {
        filterSection.visibility = VISIBLE
    }

    private fun collapseFilterCard() {
        filterSection.visibility = GONE
    }

    fun onBackPressed() {
        val undo = undoQueue.removeLastOrNull()
        undo?.evaluate()
    }

    // SearchTextViewCallback
    override fun addFilterItem(option: FilterItem) = viewModel.addFilterItem(option)

    override fun hideKeyboard() {
        callback?.hideKeyboard()
    }

    override fun setFilterTypesVisibility(isVisible: Boolean) {
        filterTypeScrollView.visibility = if (isVisible) VISIBLE else GONE
        chipSections.visibility = if (isVisible) GONE else VISIBLE
    }

    // ChippyCallback
    override fun filterChipClosed(chip: FilterChip) {
        viewModel.removeFilterItem(chip.item)
        disabledFilterChips.remove(chip)
    }

    override fun filterChipCheckChanged(buttonView: FilterChip, checked: Boolean) {
        if (checked) {
            disabledFilterChips.remove(buttonView)
            viewModel.addFilterItem(buttonView.item)
        } else {
            disabledFilterChips.add(buttonView)
            viewModel.removeFilterItem(buttonView.item)
        }
    }

    // OptionChipGroupCallback
    override fun checkChanged(action: (FilterViewModel, Boolean) -> Unit, isChecked: Boolean) {
        action(viewModel, isChecked)
    }

    override fun onClickViewChip() {
        viewModel.nextViewOption()
    }

    // SortChipGroupCallback
    override fun sortOrderChanged(sortType: SortType) {
        viewModel.setSortOption(sortType)
    }

    interface FilterFragmentCallback {
        fun onHandleClick()
        fun hideKeyboard()
        fun showKeyboard(focus: EditText)
        fun setProgressBar(isHidden: Boolean)
    }

    companion object {
        fun newInstance() = FilterFragment()

        /*
                This is a better way to do the Filter-Type Chips, rather than hard-coding them, it was
                just faster in the short run.

                val filterTypeOptions: List<KClass<*>> = FilterType::class.sealedSubclasses
                    .sortedBy { it.objectInstance.toString() }
         */
    }

    override fun getDate(currentSelection: LocalDate, isStart: Boolean) {
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
            when (reqKey) {
                RESULT_DATE_PICKER_START -> resultDate?.let { dateChipGroup.setStartDate(it) }
                RESULT_DATE_PICKER_END -> resultDate?.let { dateChipGroup.setEndDate(it) }
                else -> Unit
            }
        }

        val minDate = when (isStart) {
            true -> LocalDate.of(1900, 1, 1)
            false -> this@FilterFragment.currFilter.mStartDate
        }

        val maxDate = when (isStart) {
            true -> this@FilterFragment.currFilter.mEndDate
            false -> LocalDate.now()
        }

        DatePickerFragment.newInstance(currentSelection, minDate, maxDate, reqKey).apply {
            show(this@FilterFragment.parentFragmentManager, DIALOG_DATE)
        }
    }

    override fun setDate(date: LocalDate, isStart: Boolean) {
        viewModel.addFilterItem(DateFilter(date, isStart))
    }
}


data class Undo<T>(private val function: (T) -> Unit, private val item: T) {
    fun evaluate() = function(item)
}
