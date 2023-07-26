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
import com.wtb.comiccollector.database.models.Character
import com.wtb.comiccollector.database.models.FullCharacter
import com.wtb.comiccollector.fragments_view_models.CharacterListViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi

private const val TAG = APP + "CharacterListFragment"

@ExperimentalCoroutinesApi
class CharacterListFragment : ListFragment<FullCharacter, CharacterListFragment.CharacterHolder>() {

    override val viewModel: CharacterListViewModel by viewModels()
    override val minColSizeDp: Int
        get() = 600

    override fun onResume() {
        super.onResume()
        callback?.setTitle()
    }

    override fun getLayoutManager(): RecyclerView.LayoutManager =
        GridLayoutManager(context, numCols)

    override fun getAdapter(): CharacterAdapter = CharacterAdapter()

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

    inner class CharacterAdapter :
        PagingDataAdapter<FullCharacter, CharacterHolder>(DIFF_CALLBACK) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CharacterHolder =
            CharacterHolder(parent)

        override fun onBindViewHolder(holder: CharacterHolder, position: Int) {
            getItem(position)?.let { holder.bind(it) }
        }
    }

    inner class CharacterHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.list_item_simple, parent, false)
    ), View.OnClickListener {

        private lateinit var item: FullCharacter
        private val nameTextView: TextView =
            itemView.findViewById(R.id.list_item_simple_name)
        private val alterEgoTextView: TextView = itemView.findViewById(R.id.list_item_simple_meta_1)
        private val publisherTextView: TextView =
            itemView.findViewById(R.id.list_item_simple_meta_2)
        private val div: View = itemView.findViewById(R.id.divider_list_item_meta)

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(item: FullCharacter) {
            this.item = item
            nameTextView.text = this.item.character.name
            alterEgoTextView.text = this.item.character.alterEgo
            publisherTextView.text = this.item.publisher.publisher
            div.visibility =
                if (alterEgoTextView.text.isBlank() || publisherTextView.text.isBlank()) {
                    View.GONE
                } else {
                    View.VISIBLE
                }
        }

        override fun onClick(v: View?) {
            (callback as CharacterListCallback?)?.onCharacterSelected(item.character)
        }
    }

    interface CharacterListCallback : ListFragmentCallback {
        fun onCharacterSelected(character: Character)
    }

    companion object {
        @JvmStatic
        fun newInstance() = CharacterListFragment()

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<FullCharacter>() {
            override fun areItemsTheSame(oldItem: FullCharacter, newItem: FullCharacter): Boolean =
                oldItem.character.characterId == newItem.character.characterId


            override fun areContentsTheSame(
                oldItem: FullCharacter,
                newItem: FullCharacter,
            ): Boolean =
                oldItem == newItem
        }
    }

}