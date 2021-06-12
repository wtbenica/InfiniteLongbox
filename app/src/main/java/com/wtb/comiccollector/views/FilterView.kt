package com.wtb.comiccollector.views

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.constraintlayout.widget.ConstraintSet.*
import androidx.core.view.children
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.asLiveData
import com.google.android.material.chip.ChipGroup
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.FilterViewModel
import com.wtb.comiccollector.R
import com.wtb.comiccollector.SearchFilter
import com.wtb.comiccollector.database.models.Creator
import com.wtb.comiccollector.database.models.FilterOption
import com.wtb.comiccollector.database.models.Publisher
import com.wtb.comiccollector.database.models.Series
import com.wtb.comiccollector.item_lists.fragments.SeriesListFragment
import kotlinx.coroutines.ExperimentalCoroutinesApi


private const val TAG = APP + "FilterView"

@ExperimentalCoroutinesApi
class FilterView(ctx: Context, attributeSet: AttributeSet) :
    LinearLayout(ctx, attributeSet), Chippy.ChipCallbacks,
    SearchAutoCompleteTextView.SearchTextViewCallback {

    private var filter: SearchFilter = SearchFilter()
        set(value) {
            field = value
            onFilterChanged()
        }

    var callback: FilterCallback? = null
    private var showFilter: Boolean = false

    private val filterView: FilterView  // the whole thing

    private var switchCardView: CardView
    private val myCollectionSwitch: SwitchCompat

    private val sortCardView: CardView
    private val sortChipGroup: SortChipGroup

    private val filterCardView: CardView
    private val filterConstraintLayout: ConstraintLayout
    private val filterChipGroup: ChipGroup
    private val filterOptionsLabel: ImageView
    private val filterTextView: SearchAutoCompleteTextView

    private val viewModel by lazy {
        ViewModelProvider(ctx as ViewModelStoreOwner).get(FilterViewModel::class.java)
    }

    init {
        val inflater = ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        val view = inflater.inflate(R.layout.filter_view, this)

        filterView = view.findViewById(R.id.filter_view) as FilterView

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

        viewModel.filterOptions.asLiveData().observe(
            ctx as LifecycleOwner,
            { filterObjects: List<FilterOption> ->
                Log.d(TAG, "Updating filter options")
                filterTextView.setAdapter(FilterOptionsAdapter(ctx, filterObjects))
            }
        )

        viewModel.filter.asLiveData().observe(
            ctx as LifecycleOwner,
            { filter ->
                Log.d(TAG, "THIS SHOULD BE CHANGING FILTER _FILTER!!! $filter")
                this.filter = filter
                sortChipGroup.filter = filter
            }
        )

        myCollectionSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            Log.d(TAG, "Collection switch $isChecked")
            viewModel.myCollection(isChecked)
        }

        sortChipGroup.setOnCheckedChangeListener { group, checkedId ->
            if (checkedId >= 0) {
                view?.findViewById<SortChipGroup.SortChip>(checkedId)?.sortOption?.let {
                    if (it != filter.mSortOption) {
                        viewModel.setSortOption(it)
                    }
                }
            }
        }
    }

    private fun onFilterChanged() {
        updateViews()
        Log.d(TAG, "onFilterChanged")
        callback?.onFilterChanged(filter)
    }

    private fun updateViews() {
        val filterItems = updateFilterCard()

        myCollectionSwitch.isChecked = filter.mMyCollection

        if (showFilter) {
            filterView.visibility = VISIBLE
            switchCardView.visibility = VISIBLE
            sortCardView.visibility = VISIBLE
        } else {
            filterView.visibility =
                if (myCollectionSwitch.isChecked || filterItems.isNotEmpty()) VISIBLE else GONE
            switchCardView.visibility = if (myCollectionSwitch.isChecked) VISIBLE else GONE
            sortCardView.visibility = GONE
        }

        if (!showFilter) {
            callback?.hideKeyboard()
        }

        refreshDrawableState()
    }

    private fun updateFilterCard(): Set<FilterOption> {
        val filterItems: Set<FilterOption> = filter.getAll()
        Log.d(TAG, "${filterItems.size} FILTER ITEMS")

        val existingFilterOptions: Set<FilterOption> = filterChipGroup.children.mapNotNull {
            Log.d(TAG, "CHECKING ${(it as? Chippy)?.item?.compareValue}")
            val chippy = it as? Chippy
            if (chippy?.item in filterItems) {
                Log.d(TAG, "EXISTING FILTER ${(it as? Chippy)?.item?.compareValue}")
                chippy?.item
            } else {
                Log.d(TAG, "REMOVING FILTER ${(it as? Chippy)?.item?.compareValue}")
                filterChipGroup.removeView(it)
                null
            }
        }.toSet()

        filterItems.forEach {
            if (it !in existingFilterOptions) {
                Log.d(TAG, "ADDING ${it.compareValue} CHIP")
                addChip(it)
            } else {
                Log.d(TAG, "ALREADY ADDED ${it.compareValue}")
            }
        }

        Log.d(TAG, "KIDS: ${filterChipGroup.childCount} ${filterChipGroup.children.toList()}")
        Log.d(TAG, "FILTERS: ${filter.getAll().size}")

        if (filterItems.isEmpty()) {
            val constraints = ConstraintSet()
            constraints.clone(filterConstraintLayout)
            constraints.connect(R.id.label_filter_items, TOP, R.id.filter_text_view, TOP)
            constraints.connect(R.id.label_filter_items, BOTTOM, R.id.filter_text_view, BOTTOM)
            constraints.connect(R.id.filter_text_view, START, R.id.label_filter_items, END)
            constraints.applyTo(filterConstraintLayout)
            filterCardView.visibility = if (showFilter) VISIBLE else GONE
        } else {
            val constraints = ConstraintSet()
            constraints.clone(filterConstraintLayout)
            constraints.connect(R.id.label_filter_items, TOP, R.id.filter_chip_scrollview, TOP)
            constraints.connect(
                R.id.label_filter_items,
                BOTTOM,
                R.id.filter_chip_scrollview,
                BOTTOM
            )
            constraints.connect(R.id.filter_text_view, START, PARENT_ID, START)
            constraints.applyTo(filterConstraintLayout)
            filterCardView.visibility = VISIBLE
        }

        filterTextView.visibility = if (showFilter) VISIBLE else GONE

        return filterItems
    }

    fun toggleVisibility() {
        showFilter = !showFilter
        updateViews()
    }

    private fun addChip(item: FilterOption) {
        val chip = Chippy(context, item, this)
        filterChipGroup.addView(chip)
    }

    override fun chipClosed(view: View, item: FilterOption) {
        viewModel.removeFilterItem(item)
    }

    override fun addFilterItem(option: FilterOption) = viewModel.addFilterItem(option)

    override fun hideKeyboard() {
        callback?.hideKeyboard()
    }

    interface FilterCallback : SeriesListFragment.SeriesListCallbacks {
        fun onFilterChanged(filter: SearchFilter)
        fun hideKeyboard()
        fun showKeyboard(focus: EditText)
    }
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

        val view = convertView ?: View.inflate(context, R.layout.filter_option_auto_complete, null)

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
                    Log.d(TAG, "Empty query")
                    allOptions
                } else {
                    allOptions.filter {
                        it.compareValue.lowercase().contains(query)
                    }
                }

                return results
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
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
