package com.wtb.comiccollector.fragments

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.chip.ChipGroup
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.R
import com.wtb.comiccollector.SearchFilter
import com.wtb.comiccollector.SortType
import com.wtb.comiccollector.database.models.FilterOptionAutoCompletePopupItem
import com.wtb.comiccollector.database.models.FilterTypeSpinnerOption
import com.wtb.comiccollector.fragments_view_models.FilterViewModel
import com.wtb.comiccollector.views.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

private const val TAG = APP + "FilterFragment"

@ExperimentalCoroutinesApi
class FilterFragment : Fragment(),
    SearchAutoComplete.SearchTextViewCallback,
    FilterChip.FilterChipCallbacks, OptionChipGroup.OptionChipGroupCallback,
    SortChipGroup.SortChipGroupCallback {

    private lateinit var sections: LinearLayout
    private val viewModel: FilterViewModel by viewModels({ requireActivity() })
    private var callback: FilterFragmentCallback? = null
    private val undoQueue = ArrayDeque<Undo<*>>()
    private val disabledFilterChips = mutableSetOf<FilterChip>()

    internal var visibleState: Int = BottomSheetBehavior.STATE_EXPANDED
        set(value) {
            field = onVisibleStateUpdated(value)
        }

    private var prevFilter: SearchFilter? = null
    private var currFilter: SearchFilter = SearchFilter()
        set(value) {
            field = onFilterUpdate(value)
        }

    // Views
    private lateinit var filterView: FrameLayout
    private lateinit var handleBox: FrameLayout

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
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_filter, container, false)
        onCreateViewFindViews(view)
        onCreateViewInitViews()

        lifecycleScope.launch {
            viewModel.filter.asLiveData().observe(context as LifecycleOwner) { filter ->
                this@FilterFragment.currFilter = filter
                sortChipGroup.update(filter)
                optionsChipGroup.update(filter)
            }

            viewModel.filterOptions.asLiveData()
                .observe(context as LifecycleOwner) { filterObjects: List<FilterOptionAutoCompletePopupItem> ->
                    searchAutoComplete.setAdapter(
                        FilterOptionsAdapter(
                            requireContext(),
                            filterObjects
                        )
                    )

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        searchAutoComplete.refreshAutoCompleteResults()
                    }
                }
        }

        return view
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


        val objects = FilterTypeSpinnerOption::class.sealedSubclasses.map { it.objectInstance }
            .sortedBy { it.toString() }
        searchBoxSpinner.adapter = ArrayAdapter(
            requireContext(),
            R.layout.list_item_search_type,
            R.id.text_sort_type,
            objects
        )
        searchBoxSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                parent?.let {
                    val selectedFilterOption =
                        (it.getItemAtPosition(position) as FilterTypeSpinnerOption)
                    viewModel.setFilterOptionType(selectedFilterOption)
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
    }

    private fun onFilterUpdate(value: SearchFilter): SearchFilter {
        prevFilter = currFilter

        // TODO: Don't want to add to back stack if it's just a sort order change
        if (prevFilter != value) {
            undoQueue.add(
                Undo(function = { viewModel.setFilter(it) }, item = prevFilter ?: SearchFilter())
            )
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
        val newFilters: Set<FilterOptionAutoCompletePopupItem> = value.getAll()

        filterChipGroup.removeAllViews()
        newFilters.forEach { addChip(it) }
        disabledFilterChips.forEach { filterChipGroup.addView(it) }

        if (filterChipGroup.isEmpty()) {
            Log.d(TAG, "filterChipGroup is empty ${filterChipGroup.childCount}")
            collapseFilterCard()
            searchSection.visibility = VISIBLE
        } else {
            Log.d(TAG, "filterChipGroup is NOT empty ${filterChipGroup.childCount}")
            showChipsHideBox()
            expandFilterCard()
        }
    }

    private fun showChipsHideBox() {
        searchSection.visibility = GONE
        filterSection.visibility = VISIBLE
        filterAddButton.visibility = VISIBLE
    }

    private fun addChip(item: FilterOptionAutoCompletePopupItem) {
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
    override fun addFilterItem(option: FilterOptionAutoCompletePopupItem) =
        viewModel.addFilterItem(option)

    override fun hideKeyboard() {
        callback?.hideKeyboard()
    }

    // ChippyCallback
    override fun filterChipClosed(chip: FilterChip) {
        viewModel.removeFilterItem(chip.item)
        disabledFilterChips.remove(chip)
    }

    override fun filterChipCheckChanged(buttonView: FilterChip, checked: Boolean) {
        Log.d(TAG, "BV?: ${buttonView.isChecked} CK? $checked")
        if (checked) {
            Log.d(
                TAG,
                "adding check ${buttonView.item} removing from disabled: ${disabledFilterChips.size}"
            )
            disabledFilterChips.remove(buttonView)
            viewModel.addFilterItem(buttonView.item)
            Log.d(TAG, "checked DFC: ${disabledFilterChips.size}")
        } else {
            Log.d(
                TAG, "removing check ${buttonView.item} adding to disabled: ${
                    disabledFilterChips
                        .size
                }"
            )
            disabledFilterChips.add(buttonView)
            viewModel.removeFilterItem(buttonView.item)
            Log.d(TAG, "not checked DFC: ${disabledFilterChips.size}")
            Log.d(TAG, "Adding $buttonView to disabledFilterChips ${disabledFilterChips.size}")
        }
    }

    // OptionChipGroupCallback
    override fun checkChanged(action: (FilterViewModel, Boolean) -> Unit, isChecked: Boolean) {
        Log.d(TAG, "checkChanged: $isChecked")
        action(viewModel, isChecked)
    }

    // SortChipGroupCallback
    override fun sortOrderChanged(sortType: SortType) {
        Log.d(TAG, "Telling the viewModel to set the sort option: ${sortType.sortString}")
        viewModel.setSortOption(sortType)
    }

    interface FilterFragmentCallback {
        fun onHandleClick()
        fun hideKeyboard()
        fun showKeyboard(focus: EditText)
    }

    companion object {
        fun newInstance() = FilterFragment()
    }
}

data class Undo<T>(private val function: (T) -> Unit, private val item: T) {
    fun evaluate() = function(item)
}
