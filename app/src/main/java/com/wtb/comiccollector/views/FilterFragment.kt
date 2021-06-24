package com.wtb.comiccollector.views

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.asLiveData
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.chip.ChipGroup
import com.wtb.comiccollector.*
import com.wtb.comiccollector.database.models.Creator
import com.wtb.comiccollector.database.models.FilterOption
import com.wtb.comiccollector.database.models.Publisher
import com.wtb.comiccollector.database.models.Series
import kotlinx.coroutines.ExperimentalCoroutinesApi

private const val TAG = APP + "FilterFragment"

internal const val P_H = 48

data class Figueroa<T>(private val function: (T) -> Unit, private val item: T) {
    fun evaluate() = function(item)
}

@ExperimentalCoroutinesApi
class FilterFragment(var callback: FilterFragmentCallback? = null) : Fragment(),
    SearchAutoCompleteTextView.SearchTextViewCallback,
    Chippy.ChipCallbacks {

    companion object {
        fun newInstance(callback: FilterFragmentCallback) = FilterFragment(callback)
    }

    interface FilterFragmentCallback {
        fun hideKeyboard()
        fun showKeyboard(focus: EditText)
    }

    private var prevFilter: SearchFilter? = null

    private var filter: SearchFilter = SearchFilter()
        set(value) {
            if (prevFilter != value) {
                prevFilter = field
                filterOptionsQueue.add(
                    Figueroa(
                        { viewModel.setFilter(it) },
                        prevFilter ?: SearchFilter()
                    )
                )
            }
            field = value
            updateViews()
        }

    internal var visibleState: Int = BottomSheetBehavior.STATE_COLLAPSED
        set(value) {
            field = value
            if (field == BottomSheetBehavior.STATE_COLLAPSED) {
                handle.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    updateMargins(
                        top = dpToPx(requireContext(), 23).toInt(),
                        bottom = dpToPx(requireContext(), 24).toInt()
                    )
                }
            } else if (field == BottomSheetBehavior.STATE_EXPANDED) {
                handle.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    updateMargins(
                        top = dpToPx(requireContext(), 0).toInt(),
                        bottom = dpToPx(requireContext(), 0).toInt()
                    )
                }
            }
        }

    private val viewModel: FilterViewModel by viewModels({ requireActivity() })

    private lateinit var handle: View

    private lateinit var switchCardView: CardView
    private lateinit var myCollectionSwitch: SwitchCompat

    private lateinit var sortCardView: CardView
    private lateinit var sortChipGroup: SortChipGroup

    private lateinit var filterCardView: CardView
    private lateinit var filterConstraintLayout: ConstraintLayout
    private lateinit var filterChipGroup: ChipGroup
    private lateinit var filterOptionsLabel: ImageView
    private lateinit var filterTextView: SearchAutoCompleteTextView

    private val filterOptionsQueue = ArrayDeque<Figueroa<*>>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_filter, container, false)

        view.background = ResourcesCompat.getDrawable(resources, R.drawable.bottom_sheet, null)

        handle = view.findViewById(R.id.handle)

        switchCardView = view.findViewById(R.id.switch_card_view)
        myCollectionSwitch = view.findViewById(R.id.my_collection_switch) as SwitchCompat

        sortCardView = view.findViewById(R.id.sort_card_view) as CardView
        sortChipGroup = view.findViewById(R.id.sort_chip_group) as SortChipGroup

        filterCardView = view.findViewById(R.id.filter_card_view) as CardView
        filterConstraintLayout = view.findViewById(R.id.filter_contraint_layout)
        filterChipGroup = view.findViewById(R.id.filter_chip_group) as ChipGroup
        filterChipGroup.removeAllViews()
        filterOptionsLabel = view.findViewById(R.id.label_filter_items) as ImageView
        filterTextView = view.findViewById(R.id.filter_text_view) as SearchAutoCompleteTextView
        filterTextView.callbacks = this

        viewModel.filterOptions.asLiveData()
            .observe(context as LifecycleOwner) { filterObjects: List<FilterOption> ->
                filterTextView.setAdapter(
                    FilterOptionsAdapter(
                        requireContext(),
                        filterObjects
                    )
                )
            }

        viewModel.filter.asLiveData().observe(context as LifecycleOwner) { filter ->
            Log.d(TAG, "UPDATING FILTER")
            this.filter = filter
            sortChipGroup.filter = filter
        }

        myCollectionSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.myCollection(isChecked)
        }

        sortChipGroup.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId >= 0) {
                view?.findViewById<SortChipGroup.SortChip>(checkedId)?.sortOption?.let {
                    if (it != filter.mSortOption) {
                        viewModel.setSortOption(it)
                    }
                }
            }
        }

        return view
    }

    internal fun onSlide(slideOffset: Float) {
        val inverseOffset = 1 - slideOffset
        handle.alpha = inverseOffset
        switchCardView.alpha = slideOffset
        sortCardView.alpha = slideOffset
        filterCardView.alpha = slideOffset

        handle.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            updateMargins(
                top = dpToPx(requireContext(), 23 * inverseOffset).toInt(),
                bottom = dpToPx(requireContext(), 24 * inverseOffset).toInt()
            )
        }
    }

    private fun updateViews() {
        Log.d(TAG, "updateViews")

        updateFilterCard()

        myCollectionSwitch.isChecked = filter.mMyCollection
    }

    private fun updateFilterCard() {
        val newFilters: Set<FilterOption> = filter.getAll()

        filterChipGroup.removeAllViews()
        newFilters.forEach { addChip(it) }

        if (newFilters.isEmpty()) {
            val constraints = ConstraintSet()
            constraints.clone(filterConstraintLayout)
            constraints.connect(
                R.id.label_filter_items,
                ConstraintSet.TOP, R.id.filter_text_view,
                ConstraintSet.TOP
            )
            constraints.connect(
                R.id.label_filter_items,
                ConstraintSet.BOTTOM, R.id.filter_text_view,
                ConstraintSet.BOTTOM
            )
            constraints.connect(
                R.id.filter_text_view,
                ConstraintSet.START, R.id.label_filter_items,
                ConstraintSet.END
            )
            constraints.applyTo(filterConstraintLayout)
        } else {
            val constraints = ConstraintSet()
            constraints.clone(filterConstraintLayout)
            constraints.connect(
                R.id.label_filter_items,
                ConstraintSet.TOP, R.id.filter_chip_scrollview,
                ConstraintSet.TOP
            )
            constraints.connect(
                R.id.label_filter_items,
                ConstraintSet.BOTTOM,
                R.id.filter_chip_scrollview,
                ConstraintSet.BOTTOM
            )
            constraints.connect(
                R.id.filter_text_view,
                ConstraintSet.START,
                ConstraintSet.PARENT_ID,
                ConstraintSet.START
            )
            constraints.applyTo(filterConstraintLayout)
            filterCardView.visibility = CardView.VISIBLE
        }
    }

    private fun addChip(item: FilterOption) {
        val chip = Chippy(context, item, this)
        filterChipGroup.addView(chip)
    }

    fun onBackPressed() {
        val undo = filterOptionsQueue.removeLastOrNull()
        undo?.evaluate()
    }

    // SearchTextViewCallback
    override fun addFilterItem(option: FilterOption) = viewModel.addFilterItem(option)

    override fun hideKeyboard() {
        callback?.hideKeyboard()
    }

    // ChippyCallback
    override fun chipClosed(view: View, item: FilterOption) {
        viewModel.removeFilterItem(item)
//        filterOptionsQueue.add(
//            Figueroa(
//                { addChip(it, true) },
//                item
//            )
//        )
    }

    class FilterOptionsAdapter(ctx: Context, filterOptions: List<FilterOption>) :
        ArrayAdapter<FilterOption>(ctx, LAYOUT, filterOptions) {

        companion object {
            private const val LAYOUT = R.layout.filter_option_auto_complete
        }

        private var allOptions: List<FilterOption> = filterOptions
        private var mOptions: List<FilterOption> = filterOptions

        override fun getCount(): Int = mOptions.size

        override fun getItem(position: Int): FilterOption = mOptions[position]

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

            val view =
                convertView ?: View.inflate(context, R.layout.filter_option_auto_complete, null)

            val itemText: TextView = view.findViewById(R.id.item_text)
            val optionTypeText: TextView = view.findViewById(R.id.filter_option_type)

            val filterOption: FilterOption = getItem(position)

            itemText.text = filterOption.toString()

            optionTypeText.text = when (filterOption) {
                is Series    -> "Series"
                is Creator   -> "Creator"
                is Publisher -> "Publisher"
                else         -> ""
            }

            optionTypeText.setTextColor(
                when (filterOption) {
                    is Series    -> 0xFF0000FF.toInt()
                    is Creator   -> 0xFF00FF00.toInt()
                    is Publisher -> 0xFFFF0000.toInt()
                    else         -> 0xFFFFFFFF.toInt()
                }
            )

            return view
        }

        override fun getFilter(): Filter {
            return object : Filter() {
                override fun performFiltering(constraint: CharSequence?): FilterResults {
                    val query = constraint?.toString()?.lowercase()

                    val results = FilterResults()
                    results.values = if (query == null || query.isEmpty()) {
                        allOptions
                    } else {
                        allOptions.filter {
                            it.compareValue.lowercase().contains(query)
                        }
                    }

                    return results
                }

                override fun publishResults(
                    constraint: CharSequence?,
                    results: FilterResults?
                ) {
                    val optionsList: MutableList<FilterOption> = mutableListOf()

                    for (item in (results?.values as List<*>)) {
                        if (item is FilterOption) {
                            optionsList.add(item)
                        }
                    }

                    mOptions = optionsList
                    notifyDataSetChanged()
                }
            }
        }
    }
}