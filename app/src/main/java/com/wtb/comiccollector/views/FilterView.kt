package com.wtb.comiccollector.views

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.google.android.material.chip.ChipGroup
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.Filter
import com.wtb.comiccollector.FilterViewModel
import com.wtb.comiccollector.R
import com.wtb.comiccollector.database.models.Creator
import com.wtb.comiccollector.database.models.FilterOption
import com.wtb.comiccollector.database.models.Publisher
import com.wtb.comiccollector.database.models.Series
import com.wtb.comiccollector.item_lists.fragments.SeriesListFragment

private const val TAG = APP + "FilterView"

class FilterView(ctx: Context, attributeSet: AttributeSet) :
    LinearLayout(ctx, attributeSet), Chippy.ChipCallbacks {

    private var filter: Filter = Filter()
        set(value) {
            field = value
            onFilterChanged()
        }

    var callback: FilterCallback? = null
    private var showFilter: Boolean = false

    private var topSection: LinearLayout
    private var myCollectionSwitch: SwitchCompat
    private var sortOptionsBox: LinearLayout
    private var sortChipGroup: SortChipGroup

    private var bottomSection: LinearLayout
    private var filterChipGroup: ChipGroup
    private var filterTextView: AutoCompleteTextView

    private val viewModel by lazy {
        ViewModelProvider(ctx as ViewModelStoreOwner).get(FilterViewModel::class.java)
    }

    init {
        val inflater = ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        val view = inflater.inflate(R.layout.filter_view, this)

        topSection = view.findViewById(R.id.top_section) as LinearLayout
        myCollectionSwitch = view.findViewById(R.id.my_collection_switch) as SwitchCompat
        sortOptionsBox = view.findViewById(R.id.sort_options_box) as LinearLayout

        bottomSection = view.findViewById(R.id.bottom_section) as LinearLayout
        filterChipGroup = view.findViewById(R.id.filter_chip_group) as ChipGroup
        sortChipGroup = view.findViewById(R.id.sort_chip_group) as SortChipGroup
        filterTextView = view.findViewById(R.id.search_tv) as AutoCompleteTextView

        viewModel.filterOptionsLiveData.observe(
            ctx as LifecycleOwner,
            { filterObjects ->
                filterObjects?.let { filterTextView.setAdapter(FilterOptionsAdapter(ctx, it)) }
            }
        )

        viewModel.filterLiveData.observe(
            ctx as LifecycleOwner,
            { filter ->
                this.filter = filter
                sortChipGroup.filter = filter
            }
        )

        filterTextView.onItemClickListener =
            AdapterView.OnItemClickListener { parent, v, position, id ->
                val item = parent?.adapter?.getItem(position) as FilterOption
                viewModel.addFilterItem(item)
                filterTextView.text.clear()
                callback?.hideKeyboard()
            }

        myCollectionSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
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
        filterChipGroup.removeAllViews()

        val filterItems = filter.getAll()

        filterItems.forEach { addChip(it) }

        myCollectionSwitch.isChecked = filter.mMyCollection
        
        topSection.visibility = if (showFilter || myCollectionSwitch.isChecked) {
            View.VISIBLE
        } else {
            View.GONE
        }

        sortOptionsBox.visibility = if (showFilter) {
            View.VISIBLE
        } else {
            View.GONE
        }

        filterTextView.visibility = sortOptionsBox.visibility

        bottomSection.visibility = if (showFilter || filterItems.isNotEmpty()) {
            View.VISIBLE
        } else {
            View.GONE
        }

        if (!showFilter) {
            callback?.hideKeyboard()
        }
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

    internal fun addFilterItem(item: FilterOption) = viewModel.addFilterItem(item)

    interface FilterCallback : SeriesListFragment.SeriesListCallbacks {
        fun onFilterChanged(filter: Filter)
        fun hideKeyboard()
        fun showKeyboard(focus: EditText)
    }
}

class FilterOptionsAdapter(ctx: Context, items: List<FilterOption>) :
    ArrayAdapter<FilterOption>(ctx, R.layout.filter_option_auto_complete, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

        val view = convertView ?: View.inflate(context, R.layout.filter_option_auto_complete, null)

        val itemText: TextView = view.findViewById(R.id.item_text)
        val optionTypeText: TextView = view.findViewById(R.id.filter_option_type)

        val filterOption: FilterOption? = getItem(position)

        itemText.setText(filterOption?.toString())

        optionTypeText.text = when (filterOption) {
            is Series    -> "Series"
            is Creator   -> "Creator"
            is Publisher -> "Publisher"
            else         -> ""
        }

        return view
    }
}
