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

package dev.benica.infinite_longbox

import android.graphics.Point
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dev.benica.infinite_longbox.database.IssueDatabase
import dev.benica.infinite_longbox.database.models.BaseCollection
import dev.benica.infinite_longbox.database.models.Character
import dev.benica.infinite_longbox.database.models.Creator
import dev.benica.infinite_longbox.database.models.FullIssue
import dev.benica.infinite_longbox.database.models.FullSeries
import dev.benica.infinite_longbox.database.models.Issue
import dev.benica.infinite_longbox.viewmodels.FilterViewModel
import dev.benica.infinite_longbox.nav.ListKind
import dev.benica.infinite_longbox.nav.Navigator
import dev.benica.infinite_longbox.nav.Route
import dev.benica.infinite_longbox.nav.Screen
import dev.benica.infinite_longbox.nav.ScreenHost
import dev.benica.infinite_longbox.repository.LoadTracker
import dev.benica.infinite_longbox.repository.Repository
import dev.benica.infinite_longbox.repository.pushCoversToServer
import dev.benica.infinite_longbox.screens.CharacterListScreen
import dev.benica.infinite_longbox.screens.CharacterPage
import dev.benica.infinite_longbox.screens.CreatorListScreen
import dev.benica.infinite_longbox.screens.CreatorPage
import dev.benica.infinite_longbox.screens.IssueListScreen
import dev.benica.infinite_longbox.screens.IssuePage
import dev.benica.infinite_longbox.screens.ScreenCallback
import dev.benica.infinite_longbox.screens.ScanPage
import dev.benica.infinite_longbox.screens.SeriesListScreen
import dev.benica.infinite_longbox.ui.dialogs.AboutDialog
import dev.benica.infinite_longbox.ui.dialogs.showDialog
import dev.benica.infinite_longbox.ui.dialogs.showStoragePlaces
import dev.benica.infinite_longbox.network.Connectivity
import dev.benica.infinite_longbox.repository.CollectionTransfer
import dev.benica.infinite_longbox.ui.filter.FilterSheetHost
import dev.benica.infinite_longbox.ui.shell.AppScreen
import dev.benica.infinite_longbox.ui.theme.InfiniteLongboxTheme
import dev.benica.infinite_longbox.ui.toolbar.BarAction
import dev.benica.infinite_longbox.ui.toolbar.SelectionBar
import dev.benica.infinite_longbox.ui.update.UpdateProgress
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import dev.benica.infinite_longbox.filter.SearchFilter

const val APP = "CC_"
private const val TAG = APP + "MainActivity"

private const val KEY_NAV = "navigator"

/** The app's one activity: holds what the screens report and decides what a filter change means for the stack. */
@ExperimentalCoroutinesApi
class MainActivity : ComponentActivity(),
    SeriesListScreen.SeriesListCallback,
    IssueListScreen.IssueListCallback,
    CharacterListScreen.CharacterListCallback,
    CharacterPage.CharacterDetailCallback,
    CreatorPage.CreatorDetailCallback,
    CreatorListScreen.CreatorListCallback {

    internal val screenSizeInDp: Point
        get() {
            val dm = resources.displayMetrics
            return Point(
                (dm.widthPixels / dm.density).toInt(),
                (dm.heightPixels / dm.density).toInt()
            )
        }

    internal val navigator: Navigator by viewModels()
    private val filterViewModel: FilterViewModel by viewModels()

    private val collectionTransfer = CollectionTransfer(this)

    internal lateinit var filterSheet: FilterSheetHost
    internal lateinit var progressUpdate: UpdateProgress

    // What the screens report, for the top bar to show
    internal var barTitle by mutableStateOf("")
    internal var barSubtitle by mutableStateOf<String?>(null)
    internal var barSelection by mutableStateOf<SelectionBar?>(null)

    internal var loading by mutableStateOf(false)
    private var filterUpdating = false
    private var fetching = false

    /** The screen an entry of the stack stands for. */
    internal fun createScreen(host: ScreenHost): Screen = when (val route = host.entry.route) {
        is Route.List -> when (route.kind) {
            ListKind.SERIES -> SeriesListScreen(host)
            ListKind.ISSUES -> IssueListScreen(host)
            ListKind.CHARACTERS -> CharacterListScreen(host)
            ListKind.CREATORS -> CreatorListScreen(host)
        }
        is Route.Issue -> IssuePage(host)
        is Route.Character -> CharacterPage(host)
        is Route.Creator -> CreatorPage(host)
        Route.Scan -> ScanPage(host)
    }

    // WHAT THE FILTER MEANS FOR THE STACK

    /** Editing the filter changes the list on screen; changing what's listed, or leaving a page, opens a new screen. */
    private fun showFilter(filter: SearchFilter) {
        val entry = navigator.top
        val list = (entry?.route as? Route.List)
        when {
            entry == null -> navigator.pushList(filter)

            list != null && list.kind == ListKind.of(filter.mViewOption) -> {
                entry.listViewModel()?.let { vm ->
                    if (filter != vm.filter.value || !vm.hasFilter) vm.setFilter(filter)
                }
            }

            // Only the view changed: the list takes the place of the one showing
            list != null && entry.filter?.sameItemsAs(filter) == true -> navigator.replaceTopWithList(filter)

            else -> navigator.pushList(filter)
        }
    }

    /** Back one screen; the sheet follows the filter of the list it lands on. */
    internal fun goBack() {
        if (navigator.pop()) followTopList()
    }

    private fun followTopList() {
        navigator.top?.filter?.let {
            if (filterViewModel.filter.value != it) filterViewModel.setFilter(it)
        }
    }

    /** The activity came back on a page: the filter's first delivery is not a request for a list. */
    private var ignoreFilterReplay = false

    /** A killed app's top list is rebuilding its filter: until it has, the sheet's default is not a request. */
    private var awaitingRestore = false

    /** Drops the restored screens for a fresh default list. */
    private fun startOver() {
        Log.d(TAG, "Couldn't rebuild a restored screen's filter; starting over")
        awaitingRestore = false
        navigator.clear()
        filterViewModel.setFilter(SearchFilter())
    }

    // THE ACTIVITY

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_InfiniteLongbox)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        barTitle = applicationInfo.loadLabel(packageManager).toString()
        progressUpdate = UpdateProgress(this)
        filterSheet = FilterSheetHost(this, filterViewModel, onUpdating = ::setProgressBar)

        // Anything being fetched from the server shows in the bar, once it has taken a moment
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                LoadTracker.busy().collect {
                    fetching = it
                    showProgress()
                }
            }
        }

        setContent { InfiniteLongboxTheme { AppScreen(this) } }

        Connectivity.watch(this)

        Repository.get().beginStaticUpdate(progressUpdate, this)

        val savedStack = savedInstanceState?.getBundle(KEY_NAV)
        when {
            navigator.stack.isNotEmpty() -> {
                // Only the activity was recreated: its screens still know their filters
                val top = navigator.top
                if (top?.isDetail == true) ignoreFilterReplay = filterViewModel.filter.value != null
                else followTopList()
            }

            savedStack != null -> {
                // Killed process: the top list is rebuilding its filter, so ignore the sheet's default until it is back
                navigator.restore(savedStack, onListFailed = ::startOver)
                val top = navigator.top
                if (top?.isDetail == true) {
                    ignoreFilterReplay = filterViewModel.filter.value != null
                } else if (top != null) {
                    awaitingRestore = true
                    val vm = top.listViewModel()
                    vm?.filter?.observe(this, object : Observer<SearchFilter> {
                        override fun onChanged(value: SearchFilter) {
                            if (vm.hasFilter) {
                                vm.filter.removeObserver(this)
                                awaitingRestore = false
                                if (filterViewModel.filter.value != value) filterViewModel.setFilter(value)
                            }
                        }
                    })
                }
            }
        }

        filterViewModel.filter.observe(this) { filter ->
            if (ignoreFilterReplay) {
                ignoreFilterReplay = false
                return@observe
            }
            if (!awaitingRestore) showFilter(filter)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBundle(KEY_NAV, navigator.save())
    }

    override fun onStop() {
        Repository.get().cleanUpImages()
        super.onStop()
    }

    /** The overflow menu. */
    internal fun menuEntries() = buildList {
        add(BarAction(getString(R.string.menu_about)) { showDialog { dismiss -> AboutDialog(dismiss) } })
        add(BarAction(getString(R.string.menu_clear_db)) {
            IssueDatabase.getInstance(this@MainActivity).transactionDao().cleanDatabase()
        })
        add(BarAction(getString(R.string.menu_export_collection)) { collectionTransfer.export() })
        add(BarAction(getString(R.string.menu_import_collection)) { collectionTransfer.import() })
        add(BarAction(getString(R.string.menu_storage_places)) { showStoragePlaces() })
        // pushing covers to the dev server is a development convenience
        if (BuildConfig.DEBUG) add(BarAction(getString(R.string.menu_push_covers)) { pushLocalCovers() })
    }

    internal fun startBarcodeScan() {
        navigator.push(Route.Scan)
    }

    /** The scanner is done: the issue it added opens on a page of its own. */
    internal fun showScannedIssue(issueId: Int, variantOf: Int?) {
        navigator.pop()
        navigator.push(Route.Issue(issueId, variantOf, BaseCollection.MY_COLL.id))
    }

    private fun pushLocalCovers() {
        Toast.makeText(this, "Uploading covers…", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val result = pushCoversToServer(applicationContext)
            Toast.makeText(this@MainActivity, result.toString(), Toast.LENGTH_LONG).show()
        }
    }

    // WHAT THE SCREENS ASK

    // SeriesListScreen.SeriesListCallback
    override fun onSeriesSelected(series: FullSeries) {
        filterViewModel.addFilterItem(series)
    }

    // CharacterListScreen.CharacterListCallback
    override fun onCharacterSelected(character: Character) {
        filterViewModel.addFilterItem(character)
    }

    override fun onCharacterInfo(character: Character) {
        openCharacter(character.characterId)
    }

    // CharacterPage.CharacterDetailCallback
    override fun openCharacter(characterId: Int) {
        navigator.push(Route.Character(characterId))
    }

    override fun openIssue(issueId: Int) {
        navigator.push(Route.Issue(issueId))
    }

    override fun filterByCharacter(character: Character) {
        // Back to the list the character page was opened from, then add it to that list's filter
        while (navigator.top?.route is Route.Character && navigator.pop()) continue
        followTopList()
        filterViewModel.addFilterItem(character)
    }

    override fun onCreatorInfo(creator: Creator) {
        navigator.push(Route.Creator(creator.creatorId))
    }

    // CreatorPage.CreatorDetailCallback
    override fun filterByCreator(creator: Creator) {
        // Back to the list the creator page was opened from, then add them to that list's filter
        while (navigator.top?.route is Route.Creator && navigator.pop()) continue
        followTopList()
        filterViewModel.addFilterItem(creator)
    }

    override fun onCreatorSelected(creator: Creator) {
        filterViewModel.addFilterItem(creator)
    }

    // IssueListScreen.IssueListCallback
    override fun onIssueSelected(issue: Issue) {
        // From a collection's list, the issue page's jump bar goes through that collection
        val collection = navigator.top?.filter?.takeIf { it.mMyCollection }?.mCollectionId
        navigator.push(Route.Issue(issue.issueId, issue.variantOf, collection))
    }

    override fun onNewIssue(issueId: Int) {
        navigator.push(Route.Issue(issueId))
    }

    // ScreenCallback
    override fun setTitle(title: String?, subtitle: String?) {
        barTitle = title ?: applicationInfo.loadLabel(packageManager).toString()
        barSubtitle = subtitle
    }

    override fun setSelection(selection: SelectionBar?) {
        barSelection = selection
    }

    override fun updateFilter(filter: SearchFilter) {
        filterViewModel.setFilter(filter)
    }

    override fun addToCollection(issue: FullIssue, collId: Int) {
        Repository.get().addToCollection(issue, collId)
    }

    override fun removeFromCollection(issue: FullIssue, collId: Int) {
        Repository.get().removeFromCollection(issue.issue.issueId, collId)
    }

    /** The bar shows while the filter's items are being updated or anything is being fetched. */
    private fun showProgress() {
        loading = filterUpdating || fetching
    }

    private fun setProgressBar(isHidden: Boolean) {
        runOnUiThread {
            filterUpdating = !isHidden
            showProgress()
        }
    }
}
