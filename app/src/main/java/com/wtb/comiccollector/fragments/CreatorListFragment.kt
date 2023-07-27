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

package com.wtb.comiccollector.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.R
import com.wtb.comiccollector.database.models.Creator
import com.wtb.comiccollector.database.models.FullCreator
import com.wtb.comiccollector.fragments_view_models.CreatorListViewModel
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
            getItem(position)?.let { holder.bind(it) }
        }
    }

    inner class CreatorHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.list_item_simple, parent, false)
    ), View.OnClickListener {

        private lateinit var item: FullCreator
        private val nameTextView: TextView =
            itemView.findViewById(R.id.list_item_simple_name)
        private val nameDetailTextView: TextView =
            itemView.findViewById(R.id.list_item_simple_meta_1)

        //        private val bg: ImageView = itemView.findViewById(R.id.list_item_simple_bg)
        private val div: View = itemView.findViewById(R.id.divider_list_item_meta)

        init {
            itemView.setOnClickListener(this)
            div.visibility = View.GONE
        }

        fun bind(item: FullCreator) {
            this.item = item
            nameTextView.text = this.item.creator.name
//            alterEgoTextView.text = this.item.flatMap { it: FullCreator -> it.nameDetail.name }
            val nameDetails = this.item.nameDetail.fold(String()) { acc, fullCreator ->
                acc + "${fullCreator.name}, "
            }
            nameDetailTextView.text = nameDetails.removeSuffix(", ")
        }

        override fun onClick(v: View?) {
            (callback as CreatorListCallback?)?.onCreatorSelected(item.creator)
        }
    }

    interface CreatorListCallback : ListFragmentCallback {
        fun onCreatorSelected(creator: Creator)
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