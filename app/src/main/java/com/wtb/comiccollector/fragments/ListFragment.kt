package com.wtb.comiccollector.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
import com.google.android.material.appbar.AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.R
import com.wtb.comiccollector.fragments_view_models.FilterViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
abstract class ListFragment : Fragment() {
    protected val PEEK_HEIGHT
        get() = resources.getDimension(R.dimen.peek_height).toInt()
    protected val filterViewModel: FilterViewModel by viewModels({ requireActivity() })
    protected lateinit var listRecyclerView: RecyclerView
    protected var callback: ListFragmentCallback? = null

    private lateinit var details: FrameLayout

    override fun onResume() {
        super.onResume()

        callback?.setToolbarScrollFlags(SCROLL_FLAG_SCROLL or SCROLL_FLAG_ENTER_ALWAYS)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_item_list, container, false)

        listRecyclerView = view.findViewById(R.id.results_frame) as RecyclerView
        listRecyclerView.layoutManager = getLayoutManager()
        details = view.findViewById(R.id.details)

        val itemDecoration =
            ItemOffsetDecoration(resources.getDimension(R.dimen.offset_list_item_issue).toInt())

        listRecyclerView.addItemDecoration(itemDecoration)

        ViewCompat.setOnApplyWindowInsetsListener(listRecyclerView) { v, insets ->
            val bottom =
                PEEK_HEIGHT + insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            v.updatePadding(bottom = bottom + details.height)

            insets
        }

        return view
    }

    abstract fun getLayoutManager(): RecyclerView.LayoutManager

    override fun onDetach() {
        super.onDetach()
        callback = null
    }

    interface ListFragmentCallback {
        fun setTitle(title: String? = null)
        fun setToolbarScrollFlags(flags: Int)
    }

    companion object {
        private const val TAG = APP + "ListFragment"
    }
}