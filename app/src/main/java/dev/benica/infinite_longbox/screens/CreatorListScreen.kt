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

package dev.benica.infinite_longbox.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import dev.benica.infinite_longbox.nav.ScreenHost
import dev.benica.infinite_longbox.R
import dev.benica.infinite_longbox.database.models.Creator
import dev.benica.infinite_longbox.database.models.FullCreator
import dev.benica.infinite_longbox.fragments_view_models.CreatorListViewModel
import dev.benica.infinite_longbox.ui.lists.NameCardState
import dev.benica.infinite_longbox.ui.lists.NameListItem
import dev.benica.infinite_longbox.ui.lists.NameListItemSkeleton
import kotlinx.coroutines.ExperimentalCoroutinesApi

/** The creators, with the names they have been credited under. */
@ExperimentalCoroutinesApi
class CreatorListScreen(host: ScreenHost) : ComposeListScreen<FullCreator>(host) {

    override val viewModel: CreatorListViewModel by lazy { host.entry.viewModel<CreatorListViewModel>() }
    override val minColSizeDp: Int
        get() = 600

    override fun onShown() {
        callback.setTitle()
    }

    override fun key(item: FullCreator): Any = item.creator.creatorId

    @Composable
    override fun Skeleton(index: Int) = NameListItemSkeleton(placeholderKey = "creator|$index")

    @Composable
    override fun Row(item: FullCreator, index: Int) {
        val state = remember(item) {
            NameCardState(
                name = item.creator.name,
                details = listOf(item.nameDetail.joinToString(", ") { it.name }),
            )
        }
        NameListItem(
            state,
            onClick = { (callback as CreatorListCallback).onCreatorSelected(item.creator) },
            infoDescription = stringResource(R.string.creator_info_description),
            onInfo = { (callback as CreatorListCallback).onCreatorInfo(item.creator) },
        )
    }

    override fun scrollLabel(item: FullCreator): String? =
        if (sortedByName()) initialOf(item.creator.compareValue) else null

    interface CreatorListCallback : ListFragmentCallback {
        fun onCreatorSelected(creator: Creator)

        /** The info button: open the creator's page rather than filter by them. */
        fun onCreatorInfo(creator: Creator)
    }

}
