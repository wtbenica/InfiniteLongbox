package com.wtb.comiccollector.fragments

import android.content.Context
import android.graphics.Outline
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.*
import androidx.core.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.chip.ChipGroup
import com.wtb.comiccollector.*
import com.wtb.comiccollector.database.models.*
import com.wtb.comiccollector.fragments_view_models.FilterViewModel
import com.wtb.comiccollector.views.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.LocalDate
import kotlin.reflect.KClass

private const val TAG = APP + "FilterFragment"

@ExperimentalCoroutinesApi
class FilterFragment : Fragment(),
    SearchAutoComplete.SearchTextViewCallback,
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

    private lateinit var searchSection: LinearLayout
    private lateinit var searchAutoComplete: SearchAutoComplete
    private lateinit var searchBoxSpinner: Spinner

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

        view.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View?, outline: Outline?) {
                val rect = Rect()
                view?.background?.copyBounds(rect)
                rect.offset(0, -resources.getDimension(R.dimen.margin_default).toInt())

                outline?.setRect(rect)
            }
        }

        view.clipToOutline = true

        viewModel.filterOptions.observe(viewLifecycleOwner) { filterOptions ->
            searchAutoComplete.setAdapter(
                FilterOptionsAdapter(
                    context = requireContext(),
                    filterOptions = filterOptions
                )
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                searchAutoComplete.refreshAutoCompleteResults()
            }
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.filter.observe(
            viewLifecycleOwner,
            { filter ->
                currFilter = filter
                sortChipGroup.update(filter)
                optionsChipGroup.update(filter)
                dateChipGroup.update(filter)
            }
        )
    }

    private fun onCreateViewInitViews() {
        ViewCompat.setOnApplyWindowInsetsListener(filterView) { view, insets ->
            val posBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            view.updatePadding(bottom = posBottom)


            val imeInsetBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> { bottomMargin = imeInsetBottom }

            insets
        }

        handleBox.setOnClickListener {
            callback?.onHandleClick()
        }

        sortChipGroup.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId >= 0) {
                view?.findViewById<SortChip>(checkedId)?.sortType?.let { it ->
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

        searchAutoComplete.callbacks = this

        searchBoxSpinner.adapter = object : ArrayAdapter<KClass<*>?>(
            requireContext(),
            R.layout.spinner_item_filter_type,
            R.id.text_filter_option,
            filterTypeOptions
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: inflate(
                    context,
                    R.layout.spinner_item_filter_type,
                    null
                )
                val sortText: TextView = view.findViewById(R.id.text_filter_option)
                val item = getItem(position)
                sortText.text = item?.objectInstance.toString()
                return view
            }

            override fun getDropDownView(
                position: Int,
                convertView: View?,
                parent: ViewGroup,
            ): View {
                return getView(position, convertView, parent)
            }
        }

        searchBoxSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long,
            ) {
                parent?.let {
                    val selectedFilterOption = it.getItemAtPosition(position) as KClass<*>
                    viewModel.setFilterType(selectedFilterOption)

                    searchAutoComplete.showDropDown()

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        searchAutoComplete.refreshAutoCompleteResults()
                    }

                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
    }

    private fun onCreateViewFindViews(view: View) {
        filterView = view.findViewById(R.id.layout_filter_fragment)
        handleBox = view.findViewById(R.id.layout_filter_fragment_handle)

        sections = view.findViewById(R.id.sections)
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
        searchAutoComplete = view.findViewById(R.id.search_auto)
        searchBoxSpinner = view.findViewById(R.id.search_bar_spinner)
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
    override fun addFilterItem(option: FilterItem) {
        Log.d(TAG, "setting filter: add item $option")
        viewModel.addFilterItem(option)
    }

    override fun hideKeyboard() {
        callback?.hideKeyboard()
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
    }

    companion object {
        fun newInstance() = FilterFragment()

        val filterTypeOptions: List<KClass<*>> = FilterType::class.sealedSubclasses
            .sortedBy { it.objectInstance.toString() }

        init {
            Log.d(TAG, "FTO: ${filterTypeOptions.size} $filterTypeOptions")
        }
    }

    override fun getDate(currentSelection: LocalDate, isStart: Boolean) {
        val reqKey = if (isStart) RESULT_DATE_PICKER_START else RESULT_DATE_PICKER_END
        parentFragmentManager.setFragmentResultListener(
            reqKey,
            viewLifecycleOwner,
            { requestKey, result ->
                val resultDate: LocalDate? = result.getSerializable(ARG_DATE) as LocalDate?
                when (reqKey) {
                    RESULT_DATE_PICKER_START -> resultDate?.let { dateChipGroup.setStartDate(it) }
                    RESULT_DATE_PICKER_END   -> resultDate?.let { dateChipGroup.setEndDate(it) }
                    else                     -> Unit
                }
            })

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
