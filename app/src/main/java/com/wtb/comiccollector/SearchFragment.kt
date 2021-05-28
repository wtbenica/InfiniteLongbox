package com.wtb.comiccollector

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.wtb.comiccollector.database.models.Series
import com.wtb.comiccollector.item_lists.fragments.SeriesListFragment
import com.wtb.comiccollector.views.FilterView

private const val TAG = APP + "SearchFragment"

fun Context.hideKeyboard(view: View) {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
}

fun Context.showKeyboard(view: View, focus: EditText) {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.showSoftInput(focus, 0)
}

class SearchFragment : Fragment(), SeriesListFragment.SeriesListCallbacks,
    FilterView.FilterCallback {

    private var callbacks: Callbacks? = null

    private lateinit var sortLabelImageView: ImageView
    private lateinit var filterView: FilterView
    private lateinit var resultsFrame: FrameLayout
    private lateinit var fab: FloatingActionButton

    interface Callbacks

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as Callbacks?
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.search_fragment, container, false)
        sortLabelImageView = view.findViewById(R.id.imageView) as ImageView
        filterView = view.findViewById(R.id.filter_view)
        filterView.callback = this
        resultsFrame = view.findViewById(R.id.results_frame) as FrameLayout
        fab = view.findViewById(R.id.fab) as FloatingActionButton

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        fab.setOnClickListener {
            filterView.toggleVisibility()
        }

    }

    override fun onDetach() {
        super.onDetach()
        callbacks = null
    }

    override fun onSeriesSelected(series: Series) {
        filterView.addFilterItem(series)
    }

    companion object {
        fun newInstance(): SearchFragment {
            val args = Bundle()

            val fragment = SearchFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onResultFragmentChanged(fragment: Fragment) {
        childFragmentManager.beginTransaction()
            .replace(R.id.results_frame, fragment)
            .addToBackStack(null)
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .commit()
    }

    override fun hideKeyboard() {
        view?.let { activity?.hideKeyboard(it) }
    }

    override fun showKeyboard(focus: EditText) {
        view?.let { activity?.showKeyboard(it, focus) }
    }
}
