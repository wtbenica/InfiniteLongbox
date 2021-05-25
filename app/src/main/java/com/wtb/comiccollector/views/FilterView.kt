package com.wtb.comiccollector.views

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.google.android.material.chip.ChipGroup
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.Filter
import com.wtb.comiccollector.R
import com.wtb.comiccollector.SearchViewModel
import com.wtb.comiccollector.database.models.FilterOption
import com.wtb.comiccollector.item_lists.fragments.SeriesListFragment

private const val TAG = APP + "FilterView"

class FilterView(context: Context, attributeSet: AttributeSet) :
    LinearLayout(context, attributeSet), Chippy.ChipCallbacks {

    private var filter: Filter? = null
        set(value) {
            field = value
            onFilterChanged()
        }

    var callback: FilterCallback? = null
    private val viewModel by lazy {
        ViewModelProvider(context as ViewModelStoreOwner).get(SearchViewModel::class.java)
    }
    private var showFilter: Boolean = false

    private var sortLabelImageView: ImageView
    private var myCollectionSwitch: SwitchCompat
    private var searchChipFrame: HorizontalScrollView
    private var searchChipGroup: ChipGroup
    private var sortChipGroup: SortChipGroup
    private var sortCardThing: CardView
    private var searchBox: LinearLayout
    private var searchTextView: AutoCompleteTextView

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        val view = inflater.inflate(R.layout.filter_view, this)

        sortLabelImageView = view.findViewById(R.id.imageView) as ImageView
        myCollectionSwitch = view.findViewById(R.id.my_collection_switch) as SwitchCompat
        searchChipFrame = view.findViewById(R.id.chip_holder) as HorizontalScrollView
        searchChipGroup = view.findViewById(R.id.search_chipgroup) as ChipGroup
        sortChipGroup = view.findViewById(R.id.sort_chip_group) as SortChipGroup
        sortCardThing = view.findViewById(R.id.sort_card_thing) as CardView
        searchBox = view.findViewById(R.id.search_box) as LinearLayout
        searchTextView = (view.findViewById(R.id.search_tv) as AutoCompleteTextView)

        viewModel.filterOptionsLiveData.observe(
            context as LifecycleOwner,
            { filterObjects ->
                filterObjects?.let {
                    searchTextView.setAdapter(
                        ArrayAdapter(
                            context,
                            android.R.layout.simple_dropdown_item_1line,
                            it
                        )
                    )
                }
            }
        )

        viewModel.filterLiveData.observe(
            context as LifecycleOwner,
            { filter ->
                Log.d(TAG, "filter changed ${filter.getSortOptions()}")
                this.filter = filter
                sortChipGroup.filter = filter
            }
        )

        searchTextView.onItemClickListener =
            AdapterView.OnItemClickListener { parent, v, position, id ->
                Log.d(TAG, "searchTextView item clicked")
                val item = parent?.adapter?.getItem(position) as FilterOption
                viewModel.addFilterItem(item)
                searchTextView.text.clear()
                callback?.hideKeyboard()
            }

        myCollectionSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            Log.d(TAG, "myCollection switch toggled")
            viewModel.myCollection(isChecked)
        }

        sortChipGroup.setOnCheckedChangeListener { group, checkedId ->
            if (checkedId >= 0) {
                view?.findViewById<SortChipGroup.SortChip>(checkedId)?.sortOption?.let {
                    if (it != filter?.mSortOption) {
                        Log.d(TAG, "Sort $it")
                        viewModel.setSortOption(it)
                    }
                }
            }
        }
    }

    private fun onFilterChanged() {
        Log.d(TAG, "onUpdate")
        updateViews()

        val fragment =
            (callback as SeriesListFragment.SeriesListCallbacks?)?.let { filter?.getFragment(it) }

        fragment?.let { callback?.onResultFragmentChanged(it) }
    }

    private fun updateViews() {
        Log.d(TAG, "updateViews")
        searchChipGroup.removeAllViews()
        filter?.getAll()?.let { filters ->
            if (filters.isEmpty()) {
                searchChipFrame.visibility = View.GONE
            } else {
                searchChipFrame.visibility = View.VISIBLE
                filters.forEach { addChip(it) }
            }
        }

        myCollectionSwitch.visibility = if (showFilter || myCollectionSwitch.isChecked) {
            View.VISIBLE
        } else {
            View.GONE
        }

        sortCardThing.visibility = if (showFilter) {
            View.VISIBLE
        } else {
            View.GONE
        }
        sortLabelImageView.visibility = sortCardThing.visibility
        searchBox.visibility = sortLabelImageView.visibility
    }

    fun toggleVisibility() {
        showFilter = !showFilter
        when (showFilter) {
            true -> callback?.showKeyboard(searchTextView)
            false -> callback?.hideKeyboard()
        }
        updateViews()
    }

    private fun addChip(item: FilterOption) {
        Log.d(TAG, "adding chip $item")
        val chip = Chippy(context, item, this)
        searchChipFrame.visibility = View.VISIBLE
        searchChipGroup.addView(chip)
    }

    override fun chipClosed(view: View, item: FilterOption) {
        Log.d(TAG, "chipClosed $item")
        viewModel.removeFilterItem(item)
    }

    fun addFilterItem(item: FilterOption) = viewModel.addFilterItem(item)

    interface FilterCallback : SeriesListFragment.SeriesListCallbacks {
        fun onResultFragmentChanged(fragment: Fragment)
        fun hideKeyboard()
        fun showKeyboard(focus: EditText)
    }
}