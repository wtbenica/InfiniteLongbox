package com.wtb.comiccollector.views

import android.content.Context
import android.content.res.Resources
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat.getColor
import androidx.core.view.setPadding
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

    private val filterFrame: FilterView
    private val filterBackground: LinearLayout
    private val filterBox: CardView
    private val filterHolder: ConstraintLayout
    private val myCollectionSwitch: SwitchCompat
    private val sortOptionsLabel: ImageView
    private val sortChipGroup: SortChipGroup
    private val sortChipHolder: CardView

    private val bottomSection: Set<View>

    private val filterChipHolder: CardView
    private val filterChipGroup: ChipGroup

    private val searchHolder: CardView
    private val searchTextView: SearchAutoCompleteTextView

    private val viewModel by lazy {
        ViewModelProvider(ctx as ViewModelStoreOwner).get(FilterViewModel::class.java)
    }

    init {
        val inflater = ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        val view = inflater.inflate(R.layout.filter_view, this)

        filterFrame = view.findViewById(R.id.filter_view) as FilterView
        filterBackground = view.findViewById(R.id.filter_background) as LinearLayout
        filterBox = view.findViewById(R.id.filter_box) as CardView
        filterHolder = view.findViewById(R.id.filter_holder) as ConstraintLayout
        myCollectionSwitch = view.findViewById(R.id.my_collection_switch) as SwitchCompat

        searchTextView = view.findViewById(R.id.search_tv) as SearchAutoCompleteTextView
        searchTextView.callbacks = this

        sortOptionsLabel = view.findViewById(R.id.label_sort_options) as ImageView

        sortChipHolder = view.findViewById(R.id.sort_chip_holder) as CardView
        sortChipGroup = view.findViewById(R.id.sort_chip_group) as SortChipGroup

        searchHolder = view.findViewById(R.id.search_holder) as CardView

        filterChipHolder = view.findViewById(R.id.filter_chip_holder)
        filterChipGroup = view.findViewById(R.id.filter_chip_group) as ChipGroup

        bottomSection = setOf(filterChipHolder, searchHolder)

        viewModel.filterOptions.asLiveData().observe(
            ctx as LifecycleOwner,
            { filterObjects: List<FilterOption> ->
                Log.d(TAG, "Updating filter options")
                searchTextView.setAdapter(FilterOptionsAdapter(ctx, filterObjects))
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

    private fun dpToPx(dp: Float): Int {
        val r: Resources = context.getResources()
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            r.getDisplayMetrics()
        ).toInt()
    }

    private fun updateViews() {
        filterChipGroup.removeAllViews()

        val filterItems = filter.getAll()

        filterItems.forEach { addChip(it) }

        myCollectionSwitch.isChecked = filter.mMyCollection

        filterFrame.visibility =
            if (showFilter || myCollectionSwitch.isChecked || filterItems.isNotEmpty()) {
                VISIBLE
            } else {
                GONE
            }

        filterChipHolder.visibility = if (filterItems.isNotEmpty()) VISIBLE else GONE

        if (showFilter) {
            sortChipHolder.visibility = VISIBLE
            sortOptionsLabel.visibility = VISIBLE
            myCollectionSwitch.visibility = VISIBLE
            searchHolder.visibility = VISIBLE
            val param = filterChipHolder.layoutParams as MarginLayoutParams
            param.topMargin = dpToPx(8F)
            filterChipHolder.layoutParams = param

            filterBackground.setPadding(dpToPx(8F))
            filterBox.elevation = 8F
            filterBox.setCardBackgroundColor(getColor(resources, android.R.color.white, null))
        } else {
            sortChipHolder.visibility = GONE
            sortOptionsLabel.visibility = GONE
            searchHolder.visibility = GONE

            if (!myCollectionSwitch.isChecked) {
                val param = filterChipHolder.layoutParams as MarginLayoutParams
                param.topMargin = 0
                filterChipHolder.layoutParams = param
            }

            myCollectionSwitch.visibility = if (myCollectionSwitch.isChecked) VISIBLE else GONE

            filterFrame.visibility =
                if (myCollectionSwitch.isChecked || filterItems.isNotEmpty()) VISIBLE else GONE

            filterBackground.setPadding(0)
            filterBox.elevation = 0F
            filterBox.setCardBackgroundColor(getColor(resources, R.color.fantasia_light, null))
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

    override fun addFilterItem(option: FilterOption) = viewModel.addFilterItem(option)

    interface FilterCallback : SeriesListFragment.SeriesListCallbacks {
        fun onFilterChanged(filter: SearchFilter)
        fun hideKeyboard()
        fun showKeyboard(focus: EditText)
    }
}

class FilterOptionsAdapter(ctx: Context, filterOptions: List<FilterOption>) :
    ArrayAdapter<FilterOption>(ctx, LAYOUT, filterOptions) {

    companion object {
        private val LAYOUT = R.layout.filter_option_auto_complete
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

        itemText.setText(filterOption.toString())

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
