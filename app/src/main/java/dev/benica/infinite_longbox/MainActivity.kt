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

import android.content.Intent
import android.graphics.Point
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
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
import dev.benica.infinite_longbox.fragments_view_models.FilterViewModel
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
import dev.benica.infinite_longbox.screens.ListFragmentCallback
import dev.benica.infinite_longbox.screens.SeriesListScreen
import dev.benica.infinite_longbox.screens.StoragePrompt
import dev.benica.infinite_longbox.ui.dialogs.AboutDialog
import dev.benica.infinite_longbox.ui.dialogs.ChoiceDialog
import dev.benica.infinite_longbox.ui.dialogs.showDialog
import dev.benica.infinite_longbox.ui.filter.FilterSheetHost
import dev.benica.infinite_longbox.ui.shell.AppScreen
import dev.benica.infinite_longbox.ui.theme.InfiniteLongboxTheme
import dev.benica.infinite_longbox.ui.toolbar.BarAction
import dev.benica.infinite_longbox.ui.toolbar.SelectionBar
import dev.benica.infinite_longbox.ui.update.UpdateProgress
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.Instant
import java.time.LocalDate

const val APP = "CC_"
private const val TAG = APP + "MainActivity"

private const val KEY_NAV = "navigator"

/**
 * The app's one activity. What is on screen is [AppScreen]; this holds what the screens report to
 * (the title, the picked issues, the filter) and decides what a change of the filter means for the
 * navigator's stack.
 */
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
    }

    // WHAT THE FILTER MEANS FOR THE STACK

    /**
     * Each list screen carries the filter it shows; Back just pops to the previous screen, which
     * still has its filter and its scroll position. Editing the filter in the sheet changes the list
     * on screen. Changing what's listed (series, issues, creators, characters) or leaving a page
     * opens a new screen.
     */
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

            // Only the view was switched (series, characters, creators): the same list seen another
            // way, so it takes the place of the one showing. Keeping each in the stack piles them
            // up in memory for as long as the app runs.
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

        initNetwork()

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
                // The process was killed. The screens are rebuilding their filters from what they
                // saved; until the visible one reports in, the sheet's default must not start a new screen.
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
        add(BarAction(getString(R.string.menu_export_collection)) { exportCollection.launch(collectionFileName()) })
        add(BarAction(getString(R.string.menu_import_collection)) {
            importCollection.launch(arrayOf("application/json", "text/plain", "*/*"))
        })
        add(BarAction(getString(R.string.menu_storage_places)) { showStoragePlaces() })
        // pushing covers to the dev server is a development convenience
        if (BuildConfig.DEBUG) add(BarAction(getString(R.string.menu_push_covers)) { pushLocalCovers() })
    }

    /** Every storage place with its copy count; a tap renames it (to a name already used merges the two). */
    private fun showStoragePlaces() {
        lifecycleScope.launch {
            val places = Repository.get().storagePlaces()
            if (places.isEmpty()) {
                Toast.makeText(this@MainActivity, R.string.storage_places_none, Toast.LENGTH_SHORT).show()
                return@launch
            }
            showDialog { dismiss ->
                ChoiceDialog(
                    R.string.storage_places_title, places.map { "${it.name} (${it.copies})" }, R.string.scan_done,
                    onChoose = { which -> which?.let { renameStoragePlace(places[it].name) } },
                    dismiss = dismiss,
                )
            }
        }
    }

    private fun renameStoragePlace(name: String) {
        StoragePrompt.show(
            this, R.string.storage_rename_title,
            getString(R.string.storage_rename_message, name), name, R.string.storage_rename_clear,
        ) { newName ->
            lifecycleScope.launch {
                val copies = Repository.get().renameStorage(name, newName)
                Toast.makeText(
                    this@MainActivity,
                    if (newName == null) getString(R.string.storage_cleared, copies, name)
                    else getString(R.string.storage_renamed, copies, newName),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private val scanLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            // Details on an issue the scanner added opens its page
            val data = result.data ?: return@registerForActivityResult
            val issueId = data.getIntExtra(ScanActivity.EXTRA_ISSUE_ID, -1)
            if (issueId >= 0) {
                val variantOf = data.getIntExtra(ScanActivity.EXTRA_VARIANT_OF, -1).takeIf { it >= 0 }
                navigator.push(Route.Issue(issueId, variantOf, BaseCollection.MY_COLL.id))
            }
        }

    internal fun startBarcodeScan() {
        scanLauncher.launch(Intent(this, ScanActivity::class.java))
    }

    private val exportCollection =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            uri?.let { writeCollection(it) }
        }

    private val importCollection =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { readCollection(it) }
        }

    private fun collectionFileName() = "infinite-longbox-collection-${LocalDate.now()}.json"

    /** Saves My Collection and the Wish List to the file the person picked. */
    private fun writeCollection(uri: Uri) {
        lifecycleScope.launch {
            val saved = try {
                val json = Repository.get().exportCollections(Instant.now().toString())
                withContext(Dispatchers.IO) {
                    contentResolver.openOutputStream(uri, "wt")?.use { it.write(json.toByteArray()) }
                        ?: throw IOException("could not open $uri")
                }
                true
            } catch (e: IOException) {
                Log.e(TAG, "export collection", e)
                false
            }
            Toast.makeText(
                this@MainActivity,
                if (saved) R.string.collection_export_done else R.string.collection_export_failed,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /** Puts what is in the file the person picked into My Collection and the Wish List. */
    private fun readCollection(uri: Uri) {
        lifecycleScope.launch {
            val text = try {
                withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
                }
            } catch (e: IOException) {
                Log.e(TAG, "read collection", e)
                null
            }
            val parsed = text?.let { CollectionBackup.parse(it) }
            if (parsed !is CollectionBackup.Parsed.Ok) {
                val reason = (parsed as? CollectionBackup.Parsed.Unreadable)?.reason
                    ?: getString(R.string.collection_import_failed)
                Toast.makeText(this@MainActivity, reason, Toast.LENGTH_LONG).show()
                return@launch
            }
            Toast.makeText(this@MainActivity, R.string.collection_import_working, Toast.LENGTH_SHORT).show()
            val result = Repository.get().importCollections(parsed.collections)
            Toast.makeText(this@MainActivity, result.summary(), Toast.LENGTH_LONG).show()
        }
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

    // ListFragmentCallback
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

    private fun initNetwork() {
        val connManager = getSystemService(ConnectivityManager::class.java)

        connManager.registerDefaultNetworkCallback(object :
            ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                hasConnection.postValue(true)
                activeJob?.let { job ->
                    if (job.isCompleted) {
                        job.start()
                    }
                }
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                hasConnection.postValue(false)
                activeJob?.cancel(CancellationException("Network Lost"))
            }

            override fun onUnavailable() {
                super.onUnavailable()
                hasConnection.postValue(false)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                hasUnmeteredConnection.postValue(
                    networkCapabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_NOT_METERED
                    )
                )
            }
        })
    }

    companion object {
        internal var activeJob: Job? = null
        internal val hasConnection = MutableLiveData(false)
        internal val hasUnmeteredConnection = MutableLiveData(false)

        init {
            hasConnection.observeForever {
                Log.d(TAG, "HAS CONNECTION: $it")
            }
        }
    }
}
