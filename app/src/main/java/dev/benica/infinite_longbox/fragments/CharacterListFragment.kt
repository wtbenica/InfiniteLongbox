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
import androidx.fragment.app.viewModels
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.benica.infinite_longbox.APP
import dev.benica.infinite_longbox.R
import dev.benica.infinite_longbox.database.models.Character
import dev.benica.infinite_longbox.database.models.FullCharacter
import dev.benica.infinite_longbox.fragments_view_models.CharacterListViewModel
import dev.benica.infinite_longbox.ui.covers.placeholderKey
import dev.benica.infinite_longbox.ui.lists.NameCardState
import dev.benica.infinite_longbox.ui.lists.NameListItem
import dev.benica.infinite_longbox.ui.lists.NameListItemSkeleton
import dev.benica.infinite_longbox.ui.theme.InfiniteLongboxTheme
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
            holder.bind(getItem(position), position)
        }
    }

    inner class CharacterHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
        ComposeView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            // Recycled holders: drop the composition when the view leaves the pool or window
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
        }
    ) {

        private var item: FullCharacter? = null
        private var card by mutableStateOf<NameCardState?>(null)
        private var slot by mutableIntStateOf(0)

        init {
            (itemView as ComposeView).setContent {
                InfiniteLongboxTheme {
                    val state = card
                    if (state == null) {
                        NameListItemSkeleton(placeholderKey = "character|$slot")
                    } else {
                        NameListItem(state, onClick = {
                            item?.let { (callback as CharacterListCallback?)?.onCharacterSelected(it.character) }
                        }, onInfo = {
                            item?.let { (callback as CharacterListCallback?)?.onCharacterInfo(it.character) }
                        })
                    }
                }
            }
        }

        fun bind(item: FullCharacter?, position: Int) {
            this.slot = position
            this.item = item
            card = item?.let {
            NameCardState(
                name = item.character.name,
                details = listOf(item.character.alterEgo.orEmpty(), item.publisher.publisher),
            )
            }
        }
    }

    interface CharacterListCallback : ListFragmentCallback {
        fun onCharacterSelected(character: Character)

        /** The info button: open the character's page rather than filter by it. */
        fun onCharacterInfo(character: Character)
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