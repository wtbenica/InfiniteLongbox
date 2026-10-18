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
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_FADE
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HALF_EXPANDED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN
import com.google.android.material.bottomsheet.BottomSheetBehavior.from
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import dev.benica.infinite_longbox.database.IssueDatabase
import dev.benica.infinite_longbox.database.models.BaseCollection
import dev.benica.infinite_longbox.database.models.Character
import dev.benica.infinite_longbox.database.models.Creator
import dev.benica.infinite_longbox.database.models.FullCreator
import dev.benica.infinite_longbox.database.models.FullIssue
import dev.benica.infinite_longbox.database.models.FullSeries
import dev.benica.infinite_longbox.database.models.Issue
import dev.benica.infinite_longbox.fragments.AboutDialogFragment
import dev.benica.infinite_longbox.fragments.CharacterListFragment
import dev.benica.infinite_longbox.fragments.CreatorListFragment
import dev.benica.infinite_longbox.fragments.FilterFragment
import dev.benica.infinite_longbox.fragments.IssueDetailFragment
import dev.benica.infinite_longbox.fragments.IssueListFragment
import dev.benica.infinite_longbox.fragments.ListFragment
import dev.benica.infinite_longbox.fragments.ListFragment.Companion.forScreen
import dev.benica.infinite_longbox.fragments.SeriesListFragment
import dev.benica.infinite_longbox.fragments_view_models.FilterViewModel
import dev.benica.infinite_longbox.repository.Repository
import dev.benica.infinite_longbox.repository.pushCoversToServer
import dev.benica.infinite_longbox.views.ProgressUpdateCard
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.reflect.KClass

const val APP = "CC_"
private const val TAG = APP + "MainActivity"

private const val READ_EXTERNAL_STORAGE_REQUEST = 1
private const val CAMERA_REQUEST = 8

@ExperimentalCoroutinesApi
class MainActivity : AppCompatActivity(),
    SeriesListFragment.SeriesListCallback,
    IssueListFragment.IssueListCallback,
    CharacterListFragment.CharacterListCallback,
    CreatorListFragment.CreatorListCallback,
    NewCreatorDialogFragment.NewCreatorDialogCallback,
    FilterFragment.FilterFragmentCallback {

    internal val peekHeight
        get() = resources.getDimension(R.dimen.peek_height).toInt()

    internal val screenSizeInDp: Point
        get() {
            val dm = resources.displayMetrics
            return Point(
                (dm.widthPixels / dm.density).toInt(),
                (dm.heightPixels / dm.density).toInt()
            )
        }

    internal val screenSize: Point
        get() {
            val dm = resources.displayMetrics
            return Point(dm.widthPixels, dm.heightPixels)
        }

    private val filterViewModel: FilterViewModel by viewModels()
    private var filterFragment: FilterFragment? = null
    private lateinit var mainLayout: CoordinatorLayout
    private lateinit var appBarLayout: AppBarLayout
    private lateinit var toolbar: Toolbar
    private lateinit var resultFragmentContainer: FragmentContainerView
    private lateinit var bottomSheet: FragmentContainerView
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>
    private lateinit var progressUpdate: ProgressUpdateCard
    private lateinit var progressBar: ProgressBar
    private lateinit var scanFab: FloatingActionButton
    private var restoreNonHideable = false

    /**
     * Each list screen is its own fragment on the back stack and carries the filter it shows; Back
     * just pops to the previous screen, which still has its filter and its scroll position.
     * Editing the filter in the sheet changes the list on screen. Changing what's listed (series,
     * issues, creators, characters) or leaving the issue page opens a new screen.
     */
    private fun listClassFor(filter: SearchFilter): KClass<*> = when (filter.mViewOption) {
        FullIssue::class -> IssueListFragment::class
        Character::class -> CharacterListFragment::class
        FullSeries::class -> SeriesListFragment::class
        FullCreator::class -> CreatorListFragment::class
        else -> throw IllegalStateException("illegal view option: ${filter.mViewOption.simpleName}")
    }

    private fun newListFor(filter: SearchFilter): Fragment = when (listClassFor(filter)) {
        IssueListFragment::class -> IssueListFragment.newInstance()
        CharacterListFragment::class -> CharacterListFragment.newInstance()
        SeriesListFragment::class -> SeriesListFragment.newInstance()
        else -> CreatorListFragment.newInstance()
    }.forScreen(filterViewModel.registerScreen(filter))

    private val currentScreen: Fragment?
        get() = supportFragmentManager.findFragmentById(R.id.fragment_container)

    /** Puts what [filter] asks for on screen: the list already showing, or a new screen. */
    private fun showFilter(filter: SearchFilter) {
        val top = currentScreen
        when {
            top == null -> supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, newListFor(filter))
                .commitNow()

            top is ListFragment<*, *> && top::class == listClassFor(filter) ->
                top.applyFilter(filter)

            else -> {
                supportFragmentManager.beginTransaction()
                    .setTransition(TRANSIT_FRAGMENT_FADE)
                    .replace(R.id.fragment_container, newListFor(filter))
                    .addToBackStack(null)
                    .commit()
                // So the next filter change finds this screen, not the one being replaced
                supportFragmentManager.executePendingTransactions()
            }
        }
    }

    private var awaitingRestore = false

    /** Drops the restored screens for a fresh default list. */
    private fun startOver() {
        Log.d(TAG, "Couldn't rebuild a restored screen's filter; starting over")
        awaitingRestore = false
        supportFragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, newListFor(SearchFilter()))
            .commitNow()
        backStackSize = 0
        filterViewModel.setFilter(SearchFilter())
    }

    override fun onFilterRestored(list: Fragment, filter: SearchFilter) {
        awaitingRestore = false
        if (list === currentScreen && filterViewModel.filter.value != filter) {
            filterViewModel.setFilter(filter)
        }
    }

    override fun onRestoreFailed() {
        if (awaitingRestore || supportFragmentManager.backStackEntryCount > 0) startOver()
    }

    private var backStackSize = 0
    private var sheetStateBeforeDetail = STATE_COLLAPSED

    private fun onBackStackChanged() {
        awaitingRestore = false
        val size = supportFragmentManager.backStackEntryCount
        val popped = size < backStackSize
        backStackSize = size

        val top = currentScreen
        if (popped && top is ListFragment<*, *>) {
            // Back to a list: the filter sheet follows the filter that list was showing
            top.currentFilter?.let {
                if (filterViewModel.filter.value != it) filterViewModel.setFilter(it)
            }
        }

        syncSheet(top)
        updateScanFab()
    }

    /** The filter sheet covers the lists, not an issue's page. */
    private fun syncSheet(top: Fragment?) {
        if (top is IssueDetailFragment) {
            if (bottomSheetBehavior.state != STATE_HIDDEN) {
                // Only a resting state can be restored; DRAGGING/SETTLING would throw
                sheetStateBeforeDetail = bottomSheetBehavior.state
                    .takeIf { it == STATE_EXPANDED || it == STATE_COLLAPSED } ?: STATE_COLLAPSED
            }
            resultFragmentContainer.updatePadding(bottom = 0)
            bottomSheetBehavior.apply {
                isHideable = true
                state = STATE_HIDDEN
            }
        } else if (bottomSheetBehavior.state == STATE_HIDDEN) {
            resultFragmentContainer.updatePadding(bottom = peekHeight)
            // Slide up from where it was hidden; making it non-hideable first would snap it
            // somewhere else and animate from there. It becomes non-hideable once it settles.
            restoreNonHideable = true
            bottomSheetBehavior.apply {
                state = sheetStateBeforeDetail
                peekHeight = this@MainActivity.peekHeight
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_InfiniteLongbox)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        filterFragment =
            supportFragmentManager.findFragmentByTag(resources.getString(R.string.tag_filter_fragment)) as FilterFragment?
        mainLayout = findViewById(R.id.main_activity)
        appBarLayout = findViewById(R.id.app_bar_layout)
        toolbar = findViewById(R.id.action_bar)
        setSupportActionBar(toolbar)
        resultFragmentContainer = findViewById(R.id.fragment_container)
        bottomSheet = findViewById(R.id.bottom_sheet)
        progressUpdate = findViewById(R.id.progress_update_card)
        progressBar = findViewById(R.id.progress_bar_item_list_update)

        scanFab = findViewById(R.id.scan_fab)
        scanFab.setOnClickListener { startBarcodeScan() }
        (scanFab.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin =
            peekHeight + resources.getDimensionPixelSize(R.dimen.margin_default)
        supportFragmentManager.addOnBackStackChangedListener { onBackStackChanged() }

        initWindowInsets()
        initBottomSheet()
        initNetwork()

        Repository.get().beginStaticUpdate(progressUpdate, this)

        if (savedInstanceState != null) {
            backStackSize = supportFragmentManager.backStackEntryCount
            if (filterViewModel.hasScreens) {
                // Only the activity was recreated: its screens still know their filters, and the
                // sheet starts from the visible one
                (currentScreen as? ListFragment<*, *>)?.currentFilter
                    ?.let { filterViewModel.setFilter(it) }
            } else {
                // The process was killed. The screens came back and are rebuilding their filters
                // from what they saved; until the visible one reports in, the filter sheet's
                // default must not start a new screen.
                awaitingRestore = true
            }
        }

        filterViewModel.filter.observe(this) { filter ->
            if (!awaitingRestore) showFilter(filter)
        }
    }

    override fun onStop() {
        Repository.get().cleanUpImages()
        super.onStop()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.activity_main, menu)
        // pushing covers to the dev server is a development convenience
        menu?.findItem(R.id.menu_main_push_covers)?.isVisible = BuildConfig.DEBUG
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.app_about -> AboutDialogFragment().show(supportFragmentManager, "app_about")
            R.id.menu_main_clear_db -> IssueDatabase.getInstance(this).transactionDao()
                .cleanDatabase()

            R.id.menu_main_push_covers -> pushLocalCovers()
            else -> super.onOptionsItemSelected(item)
        }
        return true
    }

    /** Uploads covers stored on this phone that the server doesn't have (debug builds). */
    /** The scan button belongs to the lists: not on an issue's page, not over the open filter. */
    private fun updateScanFab() {
        val onDetail =
            supportFragmentManager.findFragmentById(R.id.fragment_container) is IssueDetailFragment
        val sheetOpen = bottomSheetBehavior.state == STATE_EXPANDED ||
                bottomSheetBehavior.state == STATE_HALF_EXPANDED
        if (onDetail || sheetOpen) scanFab.hide() else scanFab.show()
    }

    private val scanLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            result.data?.getStringExtra(ScanActivity.EXTRA_UPC)?.let { onBarcodeScanned(it) }
        }

    private fun startBarcodeScan() {
        scanLauncher.launch(Intent(this, ScanActivity::class.java))
    }

    /**
     * [code] is the UPC (12 digits) followed by the add-on (5 digits) if it was read. The add-on
     * says which issue, cover and printing, so with it there's normally exactly one match.
     */
    private fun onBarcodeScanned(code: String) {
        val digits = code.filter { it.isDigit() }
        val upc = digits.take(12)
        val addOn = digits.drop(12).takeIf { it.length == 5 }

        lifecycleScope.launch {
            if (addOn == null) {
                // Only the UPC was read, so it covers the whole series: don't list a slice of it,
                // ask for the issue number (checking first whether it's actually one issue).
                val probe = lookUpScan(upc, limit = 2)
                when {
                    probe == null -> showScanMessage(getString(R.string.scan_lookup_failed))
                    probe.isEmpty() -> showScanMessage(getString(R.string.scan_no_match, upc))
                    probe.size == 1 -> addScannedIssue(probe[0])
                    else -> askIssueNumber(upc)
                }
                return@launch
            }

            // Exact barcode first
            var matches = lookUpScan(digits, limit = 30)

            // The add-on isn't always recorded the same way; fall back to the series and the
            // issue number (the first 3 digits of the add-on for most publishers)
            if (matches != null && matches.isEmpty()) {
                val number = addOn.take(3).trimStart('0').ifEmpty { null }
                if (number != null) matches = lookUpScan(upc, number)
            }

            when {
                matches == null -> showScanMessage(getString(R.string.scan_lookup_failed))
                matches.isEmpty() -> showScanMessage(getString(R.string.scan_no_match, digits))
                matches.size == 1 -> addScannedIssue(matches[0])
                else -> chooseScannedIssue(matches)
            }
        }
    }

    private suspend fun lookUpScan(upc: String, number: String? = null, limit: Int? = null) =
        withContext(Dispatchers.IO) { Repository.get().getIssuesByUpc(upc, number, limit) }

    private fun askIssueNumber(upc: String) {
        val input = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            hint = getString(R.string.scan_issue_number_hint)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.scan_which_number)
            .setMessage(R.string.scan_which_number_message)
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val number = input.text.toString().trim().trimStart('0')
                lifecycleScope.launch {
                    val matches = lookUpScan(upc, number.ifEmpty { null })
                    when {
                        matches == null -> showScanMessage(getString(R.string.scan_lookup_failed))
                        matches.isEmpty() ->
                            showScanMessage(getString(R.string.scan_no_match_number, number))

                        matches.size == 1 -> addScannedIssue(matches[0])
                        else -> chooseScannedIssue(matches)
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    /** The barcode alone doesn't always pin down the issue; ask which one it is. */
    private fun chooseScannedIssue(matches: List<FullIssue>) {
        AlertDialog.Builder(this)
            .setTitle(R.string.scan_choose_issue)
            .setItems(matches.map { it.scanLabel() }.toTypedArray()) { _, which ->
                addScannedIssue(matches[which])
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun addScannedIssue(issue: FullIssue) {
        Repository.get().addToCollection(issue, BaseCollection.MY_COLL.id)

        // Show what was just added; the snackbar stays on the issue's page for Undo
        onIssueSelected(issue.issue)

        Snackbar.make(
            mainLayout,
            getString(R.string.scan_added, issue.scanLabel()),
            Snackbar.LENGTH_LONG
        )
            .setAction(R.string.scan_undo) {
                Repository.get()
                    .removeFromCollection(issue.issue.issueId, BaseCollection.MY_COLL.id)
            }
            .show()
    }

    /** Series, number and variant, so the copies sharing one barcode can be told apart. */
    private fun FullIssue.scanLabel(): String {
        val variant = issue.variantName.ifBlank {
            if (issue.variantOf == null) getString(R.string.scan_standard_edition) else ""
        }
        return if (variant.isBlank()) "$this" else "$this ($variant)"
    }

    private fun showScanMessage(message: String) {
        Snackbar.make(mainLayout, message, Snackbar.LENGTH_LONG).setAnchorView(scanFab).show()
    }

    private fun pushLocalCovers() {
        Toast.makeText(this, "Uploading covers…", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val result = pushCoversToServer(applicationContext)
            Toast.makeText(this@MainActivity, result.toString(), Toast.LENGTH_LONG).show()
        }
    }

    private fun initBottomSheet() {
        bottomSheetBehavior = from(bottomSheet)
        bottomSheetBehavior.apply {
            peekHeight = this@MainActivity.peekHeight
            isHideable = false
        }

        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                filterFragment?.visibleState = newState
                updateScanFab()
                if (restoreNonHideable &&
                    (newState == STATE_COLLAPSED || newState == STATE_EXPANDED)
                ) {
                    restoreNonHideable = false
                    bottomSheetBehavior.isHideable = false
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                filterFragment?.onSlide(slideOffset)
            }
        })
    }

    private fun initWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(appBarLayout) { view, insets ->
            val posTop = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top

            view.updatePadding(top = posTop)

            insets
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

    // SeriesListFragment.SeriesListCallbacks
    override fun onSeriesSelected(series: FullSeries) {
        filterViewModel.addFilterItem(series)
    }

    // CharacterListFragment.CharacterListCallbacks
    override fun onCharacterSelected(character: Character) {
        filterViewModel.addFilterItem(character)
//        updateFilter(SearchFilter(character = character, myCollection = false))
    }

    override fun onCreatorSelected(creator: Creator) {
        filterViewModel.addFilterItem(creator)
//        updateFilter(SearchFilter(creators = setOf(creator), myCollection = false))
    }

    // FilterFragmentCallback
    override fun onHandleClick() {
        if (bottomSheetBehavior.state == STATE_COLLAPSED) {
            bottomSheetBehavior.state = STATE_EXPANDED
        }
    }

    override fun hideKeyboard() {
        val view: View? = this.findViewById(android.R.id.content)
        val inputMethodManager =
            getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        view?.let { inputMethodManager.hideSoftInputFromWindow(it.windowToken, 0) }
    }

    override fun showKeyboard(focus: EditText) {
        val inputMethodManager =
            getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showSoftInput(focus, 0)
    }

    override fun setProgressBar(isHidden: Boolean) {
        runOnUiThread {
            progressBar.visibility = if (isHidden) View.GONE else View.VISIBLE
        }
    }

    // IssueListFragment.IssueListCallback
    override fun onIssueSelected(issue: Issue) {
        openIssue(IssueDetailFragment.newInstance(issue.issueId, issue.variantOf))
    }

    private fun openIssue(fragment: IssueDetailFragment) {
        supportFragmentManager.beginTransaction()
            .setTransition(TRANSIT_FRAGMENT_FADE)
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onNewIssue(issueId: Int) {
        openIssue(IssueDetailFragment.newInstance(issueId))
    }

    //    // SeriesInfoDialogCallback
//    override fun onSaveSeriesClick(dialog: DialogFragment, series: Series) {
//        // TODO: MainActivity onSaveSeriesClick
//        dialog.dismiss()
//    }
//
    // NewCreatorDialogCallback
    override fun onSaveCreatorClick(dialog: DialogFragment, creator: Creator) {
        // TODO: Not yet implemented
        dialog.dismiss()
    }

    override fun onCancelClick(dialog: DialogFragment) {
        // TODO: MainActivity onCancelClick
        dialog.dismiss()
    }

    // ListFragmentCallback
    override fun setTitle(title: String?) {
        val actual = title ?: applicationInfo.loadLabel(packageManager)
        toolbar.title = actual
    }

    override fun setToolbarScrollFlags(flags: Int) {
        toolbar.updateLayoutParams<AppBarLayout.LayoutParams> { scrollFlags = flags }
        if (flags and SCROLL_FLAG_SCROLL == SCROLL_FLAG_SCROLL) {
            appBarLayout.setExpanded(true, true)
        }
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

    override fun updateFilter(filter: SearchFilter) {
        filterViewModel.setFilter(filter)
    }

    override fun addToCollection(issue: FullIssue, collId: Int) {
        Repository.get().addToCollection(issue, collId)
    }

    override fun removeFromCollection(issue: FullIssue, collId: Int) {
        Repository.get().removeFromCollection(issue.issue.issueId, collId)
    }
}
