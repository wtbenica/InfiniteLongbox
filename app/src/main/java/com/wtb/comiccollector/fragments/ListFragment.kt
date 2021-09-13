package com.wtb.comiccollector.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
import com.google.android.material.appbar.AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.R
import com.wtb.comiccollector.SearchFilter
import com.wtb.comiccollector.database.models.ListItem
import com.wtb.comiccollector.fragments_view_models.FilterViewModel
import com.wtb.comiccollector.fragments_view_models.ListViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
abstract class ListFragment<T : ListItem, VH : RecyclerView.ViewHolder> : Fragment() {
    private val PEEK_HEIGHT
        get() = resources.getDimension(R.dimen.peek_height).toInt()

    protected val filterViewModel: FilterViewModel by viewModels({ requireActivity() })
    protected abstract val viewModel: ListViewModel<T>
    protected lateinit var listRecyclerView: RecyclerView
    protected lateinit var appBar: AppBarLayout
    protected lateinit var details: FrameLayout
    protected var callback: ListFragmentCallback? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = context as ListFragmentCallback
    }

    override fun onResume() {
        super.onResume()
        callback?.setToolbarScrollFlags(SCROLL_FLAG_SCROLL or SCROLL_FLAG_ENTER_ALWAYS)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.fragment_item_list, container, false)

        listRecyclerView = view.findViewById(R.id.results_frame) as RecyclerView
        listRecyclerView.layoutManager = getLayoutManager()
        appBar = view.findViewById(R.id.app_bar)
        details = view.findViewById(R.id.details)
        details.viewTreeObserver.addOnGlobalLayoutListener(
            object: ViewTreeObserver.OnGlobalLayoutListener {
                var max = 0
                override fun onGlobalLayout() {
                    val height = details.height
                    if (height > max) {
                        updateBottomPadding(height)
                        details.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        max = height
                    }
                }
            }
        )
        return view
    }

    protected fun updateBottomPadding(height: Int = 0) {
        val bottom = PEEK_HEIGHT + height
        listRecyclerView.updatePadding(bottom = bottom)
        view?.requestLayout()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateBottomPadding()

        val adapter = getAdapter()
        listRecyclerView.adapter = adapter

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.itemList.collectLatest {
                    adapter.submitData(it)
                }
            }
        }

        filterViewModel.filter.observe(
            viewLifecycleOwner,
            { filter ->
                viewModel.setFilter(filter)
            }
        )
    }

    protected abstract fun getLayoutManager(): RecyclerView.LayoutManager
    protected abstract fun getAdapter(): PagingDataAdapter<T, VH>

    override fun onDetach() {
        super.onDetach()
        callback = null
    }

    interface ListFragmentCallback {
        fun setTitle(title: String? = null)
        fun setToolbarScrollFlags(flags: Int)
        fun updateFilter(filter: SearchFilter)
    }

    companion object {
        private const val TAG = APP + "ListFragment"
    }
}