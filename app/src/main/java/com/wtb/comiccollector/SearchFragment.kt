package com.wtb.comiccollector

import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.chip.ChipGroup
import com.wtb.comiccollector.NewGroupListFragments.NewSeriesListFragment

private const val TAG = APP + "SearchFragment"

class SearchFragment : Fragment(), Chippy.ChipCallbacks, Filter.FilterObserver,
    NewSeriesListFragment.Callbacks {

    val viewModel by lazy {
        ViewModelProvider(this).get(SearchViewModel::class.java)
    }

    protected var callbacks: Callbacks? = null
    private var filter: Filter = Filter(this)
    private lateinit var searchChips: ChipGroup
    private lateinit var search: AutoCompleteTextView
    private lateinit var resultsFrame: FrameLayout

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as Callbacks?
    }

    interface Callbacks {

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.search_fragment, container, false)

        searchChips = view.findViewById(R.id.search_chipgroup) as ChipGroup
        search = view.findViewById(R.id.search_tv) as AutoCompleteTextView
        resultsFrame = view.findViewById(R.id.results_frame) as FrameLayout

        onUpdate()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.filterListLiveData.observe(
            viewLifecycleOwner,
            { filterObjects ->
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
    }

    override fun onStart() {
        super.onStart()
        search.onItemClickListener =
            object : AdapterView.OnItemClickListener {
                override fun onItemClick(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val item = parent?.adapter?.getItem(position) as Filterable
                    filter.addItem(item)
                    val chip = Chippy(context, item, this@SearchFragment)
                    chip.text = item.toString()
                    searchChips.addView(chip)
                    search.setText("")
                }
            }
    }

    override fun onDetach() {
        super.onDetach()
        callbacks = null
    }

    override fun chipClosed(view: View, item: Filterable) {
        filter.removeItem(item)
        searchChips.removeView(view)
    }

    override fun onUpdate() {
        val fragment = filter.getFragment(this)
        childFragmentManager.beginTransaction()
            .replace(R.id.results_frame, fragment)
            .addToBackStack(null)
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .commit()
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
        filter.addItem(series)
    }
}

