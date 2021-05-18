package com.wtb.comiccollector

import android.app.Activity
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.wtb.comiccollector.GroupListFragments.SeriesListFragment
import com.wtb.comiccollector.Views.Chippy
import com.wtb.comiccollector.Views.SortChipGroup
import com.wtb.comiccollector.database.models.*

private const val TAG = APP + "SearchFragment"

fun Fragment.hideKeyboard() {
    view?.let { activity?.hideKeyboard(it) }
}

fun Activity.hideKeyboard() {
    hideKeyboard(currentFocus ?: View(this))
}

fun Context.hideKeyboard(view: View) {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
}

fun Fragment.showKeyboard(editTextView: EditText) {
    view?.let { activity?.showKeyboard(it, editTextView) }
}

fun Activity.showKeyboard(editTextView: EditText) {
    showKeyboard(currentFocus ?: View(this), editTextView)
}

fun Context.showKeyboard(view: View, editTextView: EditText) {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.showSoftInput(editTextView, 0)
}

class SearchFragment : Fragment(), Chippy.ChipCallbacks, SeriesListFragment.Callbacks {

    private val viewModel by lazy {
        ViewModelProvider(this).get(SearchViewModel::class.java)
    }

    private val sharedPreferences = context?.getSharedPreferences(SHARED_PREFS, MODE_PRIVATE)
    private var callbacks: Callbacks? = null

    private lateinit var sortLabelImageView: ImageView
    private lateinit var filterBox: ConstraintLayout
    private lateinit var myCollectionSwitch: SwitchCompat
    private lateinit var searchChipFrame: HorizontalScrollView
    private lateinit var searchChipGroup: ChipGroup
    private lateinit var sortChipGroup: SortChipGroup
    private lateinit var sortCardThing: CardView
    private lateinit var searchBox: LinearLayout
    private lateinit var searchTextView: AutoCompleteTextView
    private lateinit var resultsFrame: FrameLayout
    private lateinit var fab: FloatingActionButton
    private var filter = Filter()

    interface Callbacks

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as Callbacks?
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.search_fragment, container, false)
        Log.d(TAG, "onCreateView")
        sortLabelImageView = view.findViewById(R.id.imageView) as ImageView
        filterBox = view.findViewById(R.id.switch_holder)
        myCollectionSwitch = view.findViewById(R.id.my_collection_switch) as SwitchCompat
        searchChipFrame = view.findViewById(R.id.chip_holder) as HorizontalScrollView
        searchChipGroup = view.findViewById(R.id.search_chipgroup) as ChipGroup
        sortChipGroup = view.findViewById(R.id.sort_chip_group) as SortChipGroup
        sortCardThing = view.findViewById(R.id.sort_card_thing) as CardView
        searchBox = view.findViewById(R.id.search_box) as LinearLayout
        searchTextView = (view.findViewById(R.id.search_tv) as AutoCompleteTextView)
        resultsFrame = view.findViewById(R.id.results_frame) as FrameLayout
        fab = view.findViewById(R.id.fab) as FloatingActionButton

        onUpdate()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated")

        viewModel.filterOptionsLiveData.observe(
            viewLifecycleOwner,
            { filterObjects ->
                filterObjects?.let {
                    searchTextView.setAdapter(
                        ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_dropdown_item_1line,
                            it
                        )
                    )
                }
            }
        )

        viewModel.filterLiveData.observe(
            viewLifecycleOwner,
            { filter ->
                Log.d(TAG, "filter changed ${filter.getSortOptions()}")
                this.filter = filter
                sortChipGroup.filter = filter
                onUpdate()
            }
        )
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart")
        searchTextView.onItemClickListener =
            AdapterView.OnItemClickListener { parent, view, position, id ->
                Log.d(TAG, "searchTextView item clicked")
                val item = parent?.adapter?.getItem(position) as FilterOption
                viewModel.addItem(item)
                searchBox.visibility = View.GONE
                searchTextView.text.clear()
                hideKeyboard()
                onUpdate()
            }

        myCollectionSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            Log.d(TAG, "myCollection switch toggled")
            viewModel.myCollection(isChecked)
            filterBox.visibility = if (isChecked) {
                View.VISIBLE
            } else {
                minOf(View.GONE, searchBox.visibility)
            }
            onUpdate()
        }

        fab.setOnClickListener {
            searchBox.visibility = if (searchBox.visibility == View.GONE) {
                searchTextView.requestFocus()
                showKeyboard(searchTextView)
                View.VISIBLE
            } else {
                hideKeyboard()
                View.GONE
            }

            sortCardThing.visibility = searchBox.visibility
            sortLabelImageView.visibility = sortCardThing.visibility

            filterBox.visibility = if (myCollectionSwitch.isChecked) {
                View.VISIBLE
            } else {
                minOf(View.GONE, sortChipGroup.visibility)
            }
        }

        sortChipGroup.setOnCheckedChangeListener { group, checkedId ->
            if (checkedId >= 0) {
                view?.findViewById<SortChipGroup.SortChip>(checkedId)?.sortOption?.let {
                    if (it != filter.mSortOption) {
                        Log.d(TAG, "Sort $it")
                        filter.mSortOption = it
                        Log.d(TAG, "Sort2 ${filter.mSortOption}")
                        onUpdate()
                    }
                }
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        Log.d(TAG, "onDetach")
        callbacks = null
    }

    private fun addChip(item: FilterOption) {
        Log.d(TAG, "adding chip $item")
        val chip = Chippy(context, item, this@SearchFragment)
        searchChipFrame.visibility = View.VISIBLE
        searchChipGroup.addView(chip)
    }

    override fun chipClosed(view: View, item: FilterOption) {
        Log.d(TAG, "chipClosed $item")
        viewModel.removeItem(item)
        onUpdate()
    }

    private fun onUpdate() {
        Log.d(TAG, "onUpdate")
        updateUI()
        val fragment = filter.getFragment(this)
        childFragmentManager.beginTransaction()
            .replace(R.id.results_frame, fragment)
            .addToBackStack(null)
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .commit()
    }

    private fun updateUI() {
        Log.d(TAG, "updateUI")
        searchChipGroup.removeAllViews()
        filter.getAll().let { filters ->
            if (filters.isEmpty()) {
                searchChipFrame.visibility = View.GONE
            } else {
                searchChipFrame.visibility = View.VISIBLE
                filters.forEach { addChip(it) }
            }
        }
    }

    override fun onSeriesSelected(series: Series) {
        viewModel.addItem(series)
        addChip(series)
        onUpdate()
    }

    companion object {
        fun newInstance(): SearchFragment {
            val args = Bundle()

            val fragment = SearchFragment()
            fragment.arguments = args
            return fragment
        }
    }
}
