package dev.benica.infinite_longbox.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import dev.benica.infinite_longbox.R
import dev.benica.infinite_longbox.database.models.BaseCollection
import dev.benica.infinite_longbox.nav.Screen
import dev.benica.infinite_longbox.nav.ScreenHost
import dev.benica.infinite_longbox.repository.Repository
import dev.benica.infinite_longbox.ui.collections.CollectionCard
import dev.benica.infinite_longbox.ui.collections.CollectionsScreen
import kotlinx.coroutines.ExperimentalCoroutinesApi

/** The Collections tab: My Collection and the wish list, each opening as an issue list. */
@ExperimentalCoroutinesApi
class CollectionsPage(host: ScreenHost) : Screen(host) {
    private val callback: CollectionsCallback get() = host.activity

    @Composable
    override fun Content() {
        val repository = remember { Repository.get() }
        val mine by remember { repository.collectionSize(BaseCollection.MY_COLL.id) }.collectAsState(0)
        val wishes by remember { repository.collectionSize(BaseCollection.WISH_LIST.id) }.collectAsState(0)
        CollectionsScreen(
            cards = listOf(
                CollectionCard(BaseCollection.MY_COLL.id, getString(R.string.filter_summary_my_collection), mine),
                CollectionCard(BaseCollection.WISH_LIST.id, getString(R.string.filter_summary_wish_list), wishes),
            ),
            onOpen = callback::openCollection,
        )
    }

    override fun onShown() {
        activity.setTitle(getString(R.string.collections_title))
    }

    interface CollectionsCallback {
        /** Opens collection [collectionId] as an issue list. */
        fun openCollection(collectionId: Int)
    }
}
