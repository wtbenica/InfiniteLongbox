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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.viewModels
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.benica.infinite_longbox.APP
import dev.benica.infinite_longbox.R
import dev.benica.infinite_longbox.database.models.Creator
import dev.benica.infinite_longbox.database.models.FullCreator
import dev.benica.infinite_longbox.fragments_view_models.CreatorListViewModel
import dev.benica.infinite_longbox.ui.covers.placeholderKey
import dev.benica.infinite_longbox.ui.lists.NameCardState
import dev.benica.infinite_longbox.ui.lists.NameListItem
import dev.benica.infinite_longbox.ui.lists.NameListItemSkeleton
import dev.benica.infinite_longbox.ui.theme.InfiniteLongboxTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi

private const val TAG = APP + "CreatorListFragment"

@ExperimentalCoroutinesApi
class CreatorListFragment : ListFragment<FullCreator, CreatorListFragment.CreatorHolder>() {

    override val viewModel: CreatorListViewModel by viewModels()

    override val minColSizeDp: Int
        get() = 600

    override fun onResume() {
        super.onResume()
        callback?.setTitle()
    }

    override fun getLayoutManager(): RecyclerView.LayoutManager =
        GridLayoutManager(context, numCols)

    override fun getAdapter() = CreatorAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        val itemOffsetDecoration =
            ItemOffsetDecoration(
                itemOffset = resources.getDimension(R.dimen.item_offset_vert_list_item_simple)
                    .toInt(),
                itemOffsetHorizontal = resources.getDimension(R.dimen.item_offset_horz_list_item_simple)
                    .toInt(),
                numCols = numCols
            )
        listRecyclerView.addItemDecoration(itemOffsetDecoration)

        return view
    }

    inner class CreatorAdapter :
        PagingDataAdapter<FullCreator, CreatorHolder>(DIFF_CALLBACK) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CreatorHolder =
            CreatorHolder(parent)

        override fun onBindViewHolder(holder: CreatorHolder, position: Int) {
            holder.bind(getItem(position), position)
        }
    }

    inner class CreatorHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
        ComposeView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            // Recycled holders: drop the composition when the view leaves the pool or window
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
        }
    ) {

        private var item: FullCreator? = null
        private var card by mutableStateOf<NameCardState?>(null)
        private var slot by mutableIntStateOf(0)

        init {
            (itemView as ComposeView).setContent {
                InfiniteLongboxTheme {
                    val state = card
                    if (state == null) {
                        NameListItemSkeleton(placeholderKey = "creator|$slot")
                    } else {
                        NameListItem(state, onClick = {
                            item?.let { (callback as CreatorListCallback?)?.onCreatorSelected(it.creator) }
                        }, infoDescription = stringResource(R.string.creator_info_description), onInfo = {
                            item?.let { (callback as CreatorListCallback?)?.onCreatorInfo(it.creator) }
                        })
                    }
                }
            }
        }

        fun bind(item: FullCreator?, position: Int) {
            this.slot = position
            this.item = item
            card = item?.let {
            NameCardState(
                name = item.creator.name,
                details = listOf(item.nameDetail.joinToString(", ") { it.name }),
            )
            }
        }
    }

    interface CreatorListCallback : ListFragmentCallback {
        fun onCreatorSelected(creator: Creator)

        /** The info button: open the creator's page rather than filter by them. */
        fun onCreatorInfo(creator: Creator)
    }

    companion object {
        @JvmStatic
        fun newInstance() = CreatorListFragment()

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<FullCreator>() {
            override fun areItemsTheSame(oldItem: FullCreator, newItem: FullCreator): Boolean =
                oldItem.creator.creatorId == newItem.creator.creatorId

            override fun areContentsTheSame(
                oldItem: FullCreator,
                newItem: FullCreator
            ): Boolean =
                oldItem == newItem
        }
    }
}