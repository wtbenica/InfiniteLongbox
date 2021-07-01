package com.wtb.comiccollector.views

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
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
import com.wtb.comiccollector.database.models.Creator
import com.wtb.comiccollector.database.models.FilterOption
import com.wtb.comiccollector.database.models.Publisher
import com.wtb.comiccollector.database.models.Series
import com.wtb.comiccollector.fragments_view_models.FilterViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

private const val TAG = APP + "FilterFragment"

@ExperimentalCoroutinesApi
class FilterFragment : Fragment(),
    SearchAutoComplete.SearchTextViewCallback,
    Chippy.ChipCallbacks {

    private val viewModel: FilterViewModel by viewModels({ requireActivity() })
    private var callback: FilterFragmentCallback? = null

    private var filter: SearchFilter = SearchFilter()
        set(value) {
            if (prevFilter != value) {
                prevFilter = field
                filterOptionsQueue.add(
                    Figueroa({ viewModel.setFilter(it) }, prevFilter ?: SearchFilter())
                )
            }
            field = value
            updateViews()
        }
    private var prevFilter: SearchFilter? = null

    internal var visibleState: Int = BottomSheetBehavior.STATE_EXPANDED
        set(value) {
            field = value
            if (field == BottomSheetBehavior.STATE_EXPANDED) {
                handle.visibility = GONE
            } else {
                handle.visibility = VISIBLE
            }
        }

    // Views
    private lateinit var filterView: ConstraintLayout
    private lateinit var handle: View

    private lateinit var switchCardView: CardView
    private lateinit var myCollectionSwitch: SwitchCompat

    private lateinit var sortCardView: CardView
    private lateinit var sortChipGroup: SortChipGroup

    private lateinit var filterCardView: CardView
    private lateinit var filterConstraintLayout: ConstraintLayout
    private lateinit var filterChipGroup: ChipGroup
    private lateinit var filterOptionsLabel: ImageView
    private lateinit var searchAutoComplete: SearchAutoComplete

    private val filterOptionsQueue = ArrayDeque<Figueroa<*>>()

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
                this@FilterFragment.filter = filter
                sortChipGroup.filter = filter
            }

            viewModel.filterOptions.asLiveData()
                .observe(context as LifecycleOwner) { filterObjects: List<FilterOption> ->
                    searchAutoComplete.setAdapter(
                        FilterOptionsAdapter(
                            requireContext(),
                            filterObjects
                        )
                    )
                }
        }

        return view
    }

    private fun onCreateViewInitViews() {
        ViewCompat.setOnApplyWindowInsetsListener(filterView) { v, insets ->
            val posBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            val imeInsetBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            Log.d(TAG, "UPDATING WIndoW INSeTs")
            v.updatePadding(bottom = posBottom)

            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = imeInsetBottom
            }

            insets
        }

        handle.setOnClickListener {
            Log.d(TAG, "CLICK! CCKLI! LICCK!")
            callback?.onHandleClick()
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

        filterChipGroup.removeAllViews()

        searchAutoComplete.callbacks = this
    }

    private fun onCreateViewFindViews(view: View) {
        filterView = view.findViewById(R.id.filter_view)
        handle = view.findViewById(R.id.handle)
        switchCardView = view.findViewById(R.id.switch_card_view)
        myCollectionSwitch = view.findViewById(R.id.my_collection_switch) as SwitchCompat
        sortCardView = view.findViewById(R.id.sort_card_view) as CardView
        sortChipGroup = view.findViewById(R.id.sort_chip_group) as SortChipGroup
        filterCardView = view.findViewById(R.id.filter_card_view) as CardView
        filterConstraintLayout = view.findViewById(R.id.filter_contraint_layout)
        filterChipGroup = view.findViewById(R.id.filter_chip_group) as ChipGroup
        filterOptionsLabel = view.findViewById(R.id.label_filter_items) as ImageView
        searchAutoComplete = view.findViewById(R.id.filter_text_view) as SearchAutoComplete
    }

    internal fun onSlide(slideOffset: Float) {
        val inverseOffset = 1 - slideOffset
        handle.alpha = inverseOffset
        switchCardView.alpha = slideOffset
        sortCardView.alpha = slideOffset
        filterCardView.alpha = slideOffset
    }

    private fun updateViews() {
        updateFilterCard()
        myCollectionSwitch.isChecked = filter.mMyCollection
    }

    private fun updateFilterCard() {
        val newFilters: Set<FilterOption> = filter.getAll()

        filterChipGroup.removeAllViews()
        newFilters.forEach { addChip(it) }

        if (newFilters.isEmpty()) {
            collapseFilterCard()
        } else {
            expandFilterCard()
        }
    }

    private fun expandFilterCard() {
        val constraints = ConstraintSet()
        constraints.clone(filterConstraintLayout)
        constraints.connect(
            R.id.label_filter_items, ConstraintSet.TOP,
            R.id.filter_chip_scrollview, ConstraintSet.TOP
        )
        constraints.connect(
            R.id.label_filter_items, ConstraintSet.BOTTOM,
            R.id.filter_chip_scrollview, ConstraintSet.BOTTOM
        )
        constraints.connect(
            R.id.filter_text_view, ConstraintSet.START,
            ConstraintSet.PARENT_ID, ConstraintSet.START
        )
        constraints.applyTo(filterConstraintLayout)
        filterCardView.visibility = CardView.VISIBLE
    }

    private fun collapseFilterCard() {
        val constraints = ConstraintSet()
        constraints.clone(filterConstraintLayout)
        constraints.connect(
            R.id.label_filter_items, ConstraintSet.TOP,
            R.id.filter_text_view, ConstraintSet.TOP
        )
        constraints.connect(
            R.id.label_filter_items, ConstraintSet.BOTTOM,
            R.id.filter_text_view, ConstraintSet.BOTTOM
        )
        constraints.connect(
            R.id.filter_text_view, ConstraintSet.START,
            R.id.label_filter_items, ConstraintSet.END
        )
        constraints.applyTo(filterConstraintLayout)
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

    interface FilterFragmentCallback {
        fun onHandleClick()
        fun hideKeyboard()
        fun showKeyboard(focus: EditText)
    }

    companion object {
        fun newInstance() = FilterFragment()
    }
}

data class Figueroa<T>(private val function: (T) -> Unit, private val item: T) {
    fun evaluate() = function(item)
}
