package com.wtb.comiccollector.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.ChipGroup
import com.google.android.material.shape.CornerFamily
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.R
import com.wtb.comiccollector.SearchFilter
import com.wtb.comiccollector.SortType
import com.wtb.comiccollector.database.models.Creator
import com.wtb.comiccollector.database.models.FilterOption
import com.wtb.comiccollector.database.models.Publisher
import com.wtb.comiccollector.database.models.Series
import com.wtb.comiccollector.fragments_view_models.FilterViewModel
import com.wtb.comiccollector.views.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val TAG = APP + "FilterFragment"

@ExperimentalCoroutinesApi
class FilterFragment : Fragment(),
    SearchAutoComplete.SearchTextViewCallback,
    FilterChip.FilterChipCallbacks, OptionChipGroup.OptionChipGroupCallback,
    SortChipGroup.SortChipGroupCallback {

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
//            myCollectionSwitch.isEnabled = field == BottomSheetBehavior.STATE_EXPANDED
            if (field == BottomSheetBehavior.STATE_EXPANDED) {
                handleBox.visibility = GONE
                optionChipGroup.isEnabled = true
                sortChipGroup.isEnabled = true
                filterChipGroup.isEnabled = true
            } else {
                handleBox.visibility = VISIBLE
                optionChipGroup.isEnabled = false
                sortChipGroup.isEnabled = false
                filterChipGroup.isEnabled = false
            }
        }

    // Views
    private lateinit var filterView: ConstraintLayout
    private lateinit var handleBox: View

    private lateinit var sectionCardSwitch: CardView
    private lateinit var optionChipGroup: OptionChipGroup
//    private lateinit var myCollectionSwitch: SwitchCompat

    private lateinit var sectionCardSort: CardView
    private lateinit var sortChipGroup: SortChipGroup

    private lateinit var sectionCardFilter: CardView
    private lateinit var filterConstraintLayout: ConstraintLayout

    private lateinit var filterOptionsLabel: ImageView

    private lateinit var contentCardFilterChips: MaterialCardView
    private lateinit var filterChipGroup: ChipGroup

    private lateinit var contentCardSearchAuto: MaterialCardView
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
            viewModel.filter.collectLatest { filter ->
                this@FilterFragment.filter = filter
                sortChipGroup.update(filter)
                optionChipGroup.update(filter)
            }

            viewModel.filterOptions.collectLatest { filterObjects: List<FilterOption> ->
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
                    if (it != filter.mSortType) {
                        viewModel.setSortOption(it)
                    }
                }
            }
        }

        sortChipGroup.callback = this@FilterFragment

        filterChipGroup.removeAllViews()

        searchAutoComplete.callbacks = this
    }

    private fun onCreateViewFindViews(view: View) {
        filterView = view.findViewById(R.id.layout_filter_fragment)
        handleBox = view.findViewById(R.id.layout_filter_fragment_handle)

        sectionCardSwitch = view.findViewById(R.id.section_card_options)
        optionChipGroup = view.findViewById(R.id.chip_group_option)
        optionChipGroup.callback = this
//        myCollectionSwitch = view.findViewById(R.id.my_collection_switch) as SwitchCompat

        sectionCardSort = view.findViewById(R.id.section_card_sort) as CardView
        sortChipGroup = view.findViewById(R.id.chip_group_sort) as SortChipGroup

        sectionCardFilter = view.findViewById(R.id.section_card_filter) as CardView
        filterConstraintLayout = view.findViewById(R.id.layout_filter_card)

        contentCardFilterChips = view.findViewById(R.id.content_card_filter_chips)
        filterChipGroup = view.findViewById(R.id.chip_group_filter) as ChipGroup
        filterOptionsLabel = view.findViewById(R.id.image_label_filter_items) as ImageView

        contentCardSearchAuto = view.findViewById(R.id.content_card_search_auto) as MaterialCardView
        searchAutoComplete = view.findViewById(R.id.search_auto) as SearchAutoComplete
    }

    internal fun onSlide(slideOffset: Float) {
        val inverseOffset = 1 - slideOffset
        handleBox.alpha = inverseOffset
        sectionCardSwitch.alpha = slideOffset
        sectionCardSort.alpha = slideOffset
        sectionCardFilter.alpha = slideOffset
    }

    private fun updateViews() {
        updateFilterCard()
//        myCollectionSwitch.isChecked = filter.mMyCollection
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
        adjustConstraintsOnExpandDEAD()
        val smallCorner = resources.getDimension(R.dimen.margin_default)
        val bigCorner = resources.getDimension(R.dimen.margin_wide)
        val shapeAppearanceModel = contentCardSearchAuto.shapeAppearanceModel.toBuilder()
            .setBottomLeftCorner(CornerFamily.ROUNDED, smallCorner)
            .setBottomRightCorner(CornerFamily.ROUNDED, bigCorner)
            .setTopLeftCorner(CornerFamily.ROUNDED, 0F)
            .setTopRightCorner(CornerFamily.ROUNDED, 0F)
            .build()

        contentCardSearchAuto.shapeAppearanceModel = shapeAppearanceModel

        sectionCardFilter.visibility = VISIBLE
        contentCardFilterChips.visibility = VISIBLE
    }

    private fun collapseFilterCard() {
        adjustConstraintsOnCollapseDEAD()
        val smallCorner = resources.getDimension(R.dimen.margin_default)
        val bigCorner = resources.getDimension(R.dimen.margin_wide)
        val shapeAppearanceModel = contentCardSearchAuto.shapeAppearanceModel.toBuilder()
            .setBottomLeftCorner(CornerFamily.ROUNDED, smallCorner)
            .setBottomRightCorner(CornerFamily.ROUNDED, bigCorner)
            .setTopLeftCorner(CornerFamily.ROUNDED, smallCorner)
            .setTopRightCorner(CornerFamily.ROUNDED, bigCorner)
            .build()

        contentCardSearchAuto.shapeAppearanceModel = shapeAppearanceModel
        contentCardFilterChips.visibility = GONE
    }

    private fun adjustConstraintsOnExpandDEAD() {
        //        val constraints = ConstraintSet()
        //        constraints.clone(filterConstraintLayout)
        //        constraints.connect(
        //            R.id.image_label_filter_items, ConstraintSet.TOP,
        //            R.id.content_card_filter_chips, ConstraintSet.TOP
        //        )
        //        constraints.connect(
        //            R.id.image_label_filter_items, ConstraintSet.BOTTOM,
        //            R.id.content_card_filter_chips, ConstraintSet.BOTTOM
        //        )
        //        constraints.connect(
        //            R.id.content_card_search_auto, ConstraintSet.START,
        //            ConstraintSet.PARENT_ID, ConstraintSet.START
        //        )
        //        constraints.connect(
        //            R.id.image_label_filter_items, ConstraintSet.END,
        //            R.id.content_card_filter_chips, ConstraintSet.START
        //        )
        //        constraints.setMargin(
        //            R.id.image_label_filter_items,
        //            ConstraintSet.END,
        //            resources.getDimension(R.dimen.margin_wide).toInt()
        //        )
        //        constraints.applyTo(filterConstraintLayout)
        //
    }

    private fun adjustConstraintsOnCollapseDEAD() {
        //        val constraints = ConstraintSet()
        //        constraints.clone(filterConstraintLayout)
        //        constraints.connect(
        //            R.id.image_label_filter_items, ConstraintSet.TOP,
        //            R.id.content_card_search_auto, ConstraintSet.TOP
        //        )
        //        constraints.connect(
        //            R.id.image_label_filter_items, ConstraintSet.BOTTOM,
        //            R.id.content_card_search_auto, ConstraintSet.BOTTOM
        //        )
        //        constraints.connect(
        //            R.id.content_card_search_auto, ConstraintSet.START,
        //            R.id.image_label_filter_items, ConstraintSet.END
        //        )
        //        constraints.connect(
        //            R.id.image_label_filter_items, ConstraintSet.END,
        //            R.id.content_card_search_auto, ConstraintSet.START
        //        )
        //
        //        constraints.setMargin(
        //            R.id.image_label_filter_items,
        //            ConstraintSet.END,
        //            resources.getDimension(R.dimen.margin_wide).toInt()
        //        )
        //        constraints.applyTo(filterConstraintLayout)
        //
    }

    private fun addChip(item: FilterOption) {
        val chip = FilterChip(context, item, this)
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
    override fun chipClosed(item: FilterOption) {
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
                convertView ?: inflate(context, R.layout.filter_option_auto_complete, null)

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

    override fun checkChanged(action: (FilterViewModel, Boolean) -> Unit, isChecked: Boolean) {
        Log.d(TAG, "checkChanged: $isChecked")
        action(viewModel, isChecked)
    }

    override fun sortOrderChanged(sortType: SortType) {
        Log.d(TAG, "Telling the viewModel to set the sort option: ${sortType.sortString}")
        viewModel.setSortOption(sortType)
    }
}

data class Figueroa<T>(private val function: (T) -> Unit, private val item: T) {
    fun evaluate() = function(item)
}
