/*
 * Copyright (c) 2023. Wesley T. Benica
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.benica.infinite_longbox.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.updatePadding
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
import com.google.android.material.appbar.AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL
import dev.benica.infinite_longbox.APP
import dev.benica.infinite_longbox.MainActivity
import dev.benica.infinite_longbox.R
import dev.benica.infinite_longbox.SearchFilter
import dev.benica.infinite_longbox.SortType
import dev.benica.infinite_longbox.database.models.FilterModel
import dev.benica.infinite_longbox.database.models.ListItem
import dev.benica.infinite_longbox.fragments_view_models.FilterViewModel
import dev.benica.infinite_longbox.fragments_view_models.ListViewModel
import dev.benica.infinite_longbox.views.FastScrollHandle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
abstract class ListFragment<T : ListItem, VH : RecyclerView.ViewHolder> : Fragment() {
    private val peekHeight
        get() = resources.getDimension(R.dimen.peek_height).toInt()

    private val filterViewModel: FilterViewModel by viewModels({ requireActivity() })
    protected abstract val viewModel: ListViewModel<T>

    protected lateinit var outerScrollView: NestedScrollView
    protected lateinit var listRecyclerView: RecyclerView
    private lateinit var appBar: AppBarLayout
    protected lateinit var details: FrameLayout
    private lateinit var fastScrollBubble: TextView

    /** Whether dragging the scroll handle shows the first letter of the name it's at. */
    protected open val showsLetterBubble: Boolean = true

    protected var callback: ListFragmentCallback? = null

    protected var numCols: Int = 1
    abstract val minColSizeDp: Int

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = context as ListFragmentCallback
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        numCols = (context as MainActivity).screenSizeInDp.x / minColSizeDp + 1
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

        listRecyclerView = view.findViewById(R.id.results_frame)
        listRecyclerView.layoutManager = getLayoutManager()

        // Items update in place (a cover arrives, a collection button flips). The default
        // cross-fade for that leaves a recycled view displaced or half-faded if it's scrolled away
        // mid-animation, which showed up as a card sitting on top of the row above.
        (listRecyclerView.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false

        fastScrollBubble = view.findViewById(R.id.fast_scroll_bubble)
        setUpFastScrollHandle(view)

        appBar = view.findViewById(R.id.app_bar)

        details = view.findViewById(R.id.details)

        // While an app bar is expanded, the view it scrolls with is taller than its container by
        // that bar's scroll range. The list is affected twice: by this fragment's app bar, and
        // (through the fragment container) by the activity's top bar. Unless the bottom padding
        // covers both, the last rows sit below the bottom sheet. Both bars' content changes after
        // the first layout (animated, filled in as data arrives), so keep the padding in step with
        // their current ranges instead of measuring once.
        appBar.addOnLayoutChangeListener(appBarLayoutListener)
        activityAppBar?.addOnLayoutChangeListener(appBarLayoutListener)

        return view
    }

    /** Hooks the scroll handle to the list, and labels it with a letter while it's dragged. */
    private fun setUpFastScrollHandle(view: View) {
        val handle = view.findViewById<FastScrollHandle>(R.id.fast_scroll_handle)
        handle.attach(listRecyclerView)
        if (!showsLetterBubble) return

        handle.listener = object : FastScrollHandle.Listener {
            override fun onDrag(position: Int) {
                if (!sortedByName()) return
                // The list moves on the next frame; read the letter once it has
                listRecyclerView.post {
                    val letter = firstVisibleLetter() ?: return@post
                    fastScrollBubble.text = letter
                    fastScrollBubble.translationY = handle.thumbCenterY - handle.height / 2f
                    fastScrollBubble.visibility = View.VISIBLE
                }
            }

            override fun onDragEnd() {
                fastScrollBubble.postDelayed({ fastScrollBubble.visibility = View.GONE }, 400)
            }
        }
    }

    private fun sortedByName(): Boolean =
        filterViewModel.filter.value?.mSortType?.sortColumn?.contains("name", ignoreCase = true) == true

    /** First letter of the first visible item's name, "#" for numbers; null while it isn't loaded. */
    private fun firstVisibleLetter(): String? {
        val layoutManager = listRecyclerView.layoutManager as? LinearLayoutManager ?: return null
        val position = layoutManager.findFirstVisibleItemPosition()
        if (position < 0) return null

        val item = (listRecyclerView.adapter as? PagingDataAdapter<*, *>)?.peek(position)
        val name = (item as? FilterModel)?.compareValue ?: return null
        val first = SortType.sortKeyOf(name).firstOrNull() ?: return null
        return if (first.isLetter()) first.uppercase() else "#"
    }

    private val activityAppBar: AppBarLayout?
        get() = activity?.findViewById(R.id.app_bar_layout)

    private val appBarLayoutListener =
        View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            listRecyclerView.post {
                updateBottomPadding(appBar.totalScrollRange + (activityAppBar?.totalScrollRange ?: 0))
            }
        }

    override fun onDestroyView() {
        appBar.removeOnLayoutChangeListener(appBarLayoutListener)
        activityAppBar?.removeOnLayoutChangeListener(appBarLayoutListener)
        super.onDestroyView()
    }

    protected fun updateBottomPadding(height: Int = 0) {
        val bottom = peekHeight + height
        if (listRecyclerView.paddingBottom != bottom) {
            listRecyclerView.updatePadding(bottom = bottom)
            view?.requestLayout()
        }
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
            viewLifecycleOwner
        ) { filter ->
            viewModel.setFilter(filter)
        }
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