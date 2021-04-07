package com.wtb.comiccollector

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.chip.ChipGroup
import com.wtb.comiccollector.GroupListFragments.SeriesListFragment

private const val TAG = APP + "SearchFragment"

class SearchFragment : Fragment(), Chippy.ChipCallbacks, SeriesListFragment.Callbacks {

    private val viewModel by lazy {
        ViewModelProvider(this).get(SearchViewModel::class.java)
    }

    private val sharedPreferences = context?.getSharedPreferences(SHARED_PREFS, MODE_PRIVATE)
    private var callbacks: Callbacks? = null
    private lateinit var searchChips: ChipGroup
    private lateinit var search: AutoCompleteTextView
    private lateinit var resultsFrame: FrameLayout
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
        searchChips = view.findViewById(R.id.search_chipgroup) as ChipGroup
        search = view.findViewById(R.id.search_tv) as AutoCompleteTextView
        resultsFrame = view.findViewById(R.id.results_frame) as FrameLayout

        onUpdate()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated")

        viewModel.filterOptionsLiveData.observe(
            viewLifecycleOwner,
            { filterObjects ->
                Log.d(TAG, "filterList changed")
                filterObjects?.let {
                    search.setAdapter(
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
            {filter ->
                this.filter = filter
                onUpdate()
            }
        )
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart")
        search.onItemClickListener =
            AdapterView.OnItemClickListener { parent, view, position, id ->
                val item = parent?.adapter?.getItem(position) as Filterable
                viewModel.addItem(item)
                search.setText("")
                onUpdate()
            }
    }

    override fun onDetach() {
        super.onDetach()
        Log.d(TAG, "onDetach")
        callbacks = null
    }

    private fun addChip(item: Filterable) {
        Log.d(TAG, "adding chip $item")
        val chip = Chippy(context, item, this@SearchFragment)
        chip.text = item.toString()
        searchChips.addView(chip)
    }

    override fun chipClosed(view: View, item: Filterable) {
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
        searchChips.removeAllViews()
        filter.getAll().forEach { addChip(it) }
    }

    companion object {
        fun newInstance(): SearchFragment {
            val args = Bundle()

            val fragment = SearchFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onSeriesSelected(series: Series) {
        viewModel.addItem(series)
        addChip(series)
        onUpdate()
    }
}

