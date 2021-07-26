package com.wtb.comiccollector

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.ContentFrameLayout
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.*
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_FADE
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.*
import com.wtb.comiccollector.database.models.*
import com.wtb.comiccollector.fragments.*
import com.wtb.comiccollector.fragments_view_models.FilterViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch

const val APP = "CC_"
private const val TAG = APP + "MainActivity"

@ExperimentalCoroutinesApi
class MainActivity : AppCompatActivity(),
    SeriesListFragment.SeriesListCallback,
    IssueListFragment.IssueListCallback,
    SeriesInfoDialogFragment.SeriesInfoDialogCallback,
    CharacterListFragment.CharacterListCallback,
    NewCreatorDialogFragment.NewCreatorDialogCallback,
    FilterFragment.FilterFragmentCallback {

    private val PEEK_HEIGHT
        get() = resources.getDimension(R.dimen.peek_height).toInt()

    private val filterViewModel: FilterViewModel by viewModels()

    private var filterFragment: FilterFragment? = null
    private lateinit var mainLayout: CoordinatorLayout
    private lateinit var appBarLayout: AppBarLayout
    private lateinit var toolbar: Toolbar
    private lateinit var resultFragmentContainer: FragmentContainerView
    private lateinit var bottomSheet: FragmentContainerView
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>

    private val resultFragmentManager by lazy {
        ResultFragmentManager()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_ComicCollector)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        filterFragment = supportFragmentManager.findFragmentByTag(
            resources.getString(R.string.tag_filter_fragment)
        ) as FilterFragment?
        mainLayout = findViewById(R.id.main_activity)
        appBarLayout = findViewById(R.id.app_bar_layout)
        toolbar = findViewById(R.id.action_bar)
        setSupportActionBar(toolbar)
        resultFragmentContainer = findViewById(R.id.fragment_container)
        bottomSheet = findViewById(R.id.bottom_sheet)

        initWindowInsets()
        initBottomSheet()
        initNetwork()

        lifecycleScope.launch {
            resultFragmentManager.fragment.collectLatest { frag ->
                frag?.let { fragment ->
                    supportFragmentManager.beginTransaction()
                        .setTransition(TRANSIT_FRAGMENT_FADE)
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null)
                        .commit()

                    val tt = object : OnBackPressedCallback(true) {
                        override fun handleOnBackPressed() {
                            filterFragment?.onBackPressed()
                        }
                    }

                    onBackPressedDispatcher.addCallback(fragment, tt)

                    if (this@MainActivity.bottomSheetBehavior.state == STATE_HIDDEN) {
                        this@MainActivity.bottomSheetBehavior.state = STATE_COLLAPSED
                    }
                }
            }
        }
    }

    private fun initBottomSheet() {
        bottomSheetBehavior = from(bottomSheet)
        bottomSheetBehavior.apply {
            peekHeight = PEEK_HEIGHT
            isHideable = false
            saveFlags = SAVE_ALL
        }

        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                Log.d(TAG, "onStateChanged: ${getStateName(newState)}")
                filterFragment?.visibleState = newState
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

        ViewCompat.setOnApplyWindowInsetsListener(resultFragmentContainer) { view, insets ->
            val posBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom

            view.updatePadding(bottom = posBottom)

            insets
        }
    }

    private fun initNetwork() {
        val connManager = getSystemService(ConnectivityManager::class.java)

        connManager.registerDefaultNetworkCallback(object :
                                                       ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                Log.d(TAG, "NetworkCallback onAvailable")
                hasConnection.postValue(true)
                activeJob?.let { job ->
                    if (job.isCancelled) {
                        job.start()
                    }
                }
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                Log.d(TAG, "NetworkCallback onLost")
                hasConnection.postValue(false)
                activeJob?.cancel()
            }

            override fun onUnavailable() {
                super.onUnavailable()
                Log.d(TAG, "NetworkCallback onUnavailable")
                hasConnection.postValue(true)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
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
    override fun onSeriesSelected(series: Series) {
        Log.d(TAG, "ADDING SERIES $series")
        filterFragment?.addFilterItem(series)
    }

    // CharacterListFragment.CharacterListCallbacks
    override fun onCharacterSelected(character: Character) {
        Log.d(TAG, "ADDING CHARACTER $character")
        filterFragment?.addFilterItem(character)
    }

    // FilterFragmentCallback
    override fun onHandleClick() {
        if (bottomSheetBehavior.state == STATE_COLLAPSED) {
            bottomSheetBehavior.state = STATE_EXPANDED
        }
    }

    override fun hideKeyboard() {
        val view = this.findViewById(android.R.id.content) as ContentFrameLayout
        val inputMethodManager =
            getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    override fun showKeyboard(focus: EditText) {
        val inputMethodManager =
            getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showSoftInput(focus, 0)
    }

    // IssueListFragment.IssueListCallback
    override fun onIssueSelected(issue: Issue) {
        val fragment = IssueDetailFragment.newInstance(issue.issueId, false, issue.variantOf)
        val prevState = bottomSheetBehavior.state
        supportFragmentManager
            .beginTransaction()
            .setTransition(TRANSIT_FRAGMENT_FADE)
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()

        val tt = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                resultFragmentContainer.updatePadding(bottom = PEEK_HEIGHT)

                bottomSheetBehavior.apply {
                    isHideable = false
                    state = prevState
                    peekHeight = PEEK_HEIGHT
                }

                supportFragmentManager.popBackStack()
            }
        }

        this.onBackPressedDispatcher.addCallback(fragment, tt)

        bottomSheetBehavior.apply {
            isHideable = true
            state = STATE_HIDDEN
        }

        resultFragmentContainer.updatePadding(bottom = 0)
    }

    override fun onNewIssue(issueId: Int) {
        val fragment = IssueDetailFragment.newInstance(issueId)

        supportFragmentManager
            .beginTransaction()
            .setTransition(TRANSIT_FRAGMENT_FADE)
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    // SeriesInfoDialogCallback
    override fun onSaveSeriesClick(
        dialog: DialogFragment,
        series: Series
    ) {
        // TODO: MainActivity onSaveSeriesClick
        dialog.dismiss()
    }

    override fun onCancelClick(dialog: DialogFragment) {
        // TODO: MainActivity onCancelClick
        dialog.dismiss()
    }

    // NewCreatorDialogCallback
    override fun onSaveCreatorClick(
        dialog: DialogFragment,
        creator: Creator
    ) {
        // TODO: Not yet implemented
        dialog.dismiss()
    }

    // ListFragmentCallback
    override fun setTitle(title: String?) {
        val actual = title ?: applicationInfo.loadLabel(packageManager)
        Log.d(TAG, "setTitle: $actual")
        toolbar.title = actual
    }

    override fun setToolbarScrollFlags(flags: Int) {
        Log.d(TAG, "setToolbarScrollFlags $flags")
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

        fun getStateName(newState: Int): String {
            return when (newState) {
                STATE_EXPANDED      -> "EXPANDED"
                STATE_HALF_EXPANDED -> "HALF-EXPANDED"
                STATE_COLLAPSED     -> "COLLAPSED"
                STATE_DRAGGING      -> "DRAGGING"
                STATE_HIDDEN        -> "HIDDEN"
                STATE_SETTLING      -> "SETTLING"
                else                -> "THAT'S ODD!"
            }
        }

        fun resolveThemeAttribute(
            attr: Int,
            context: Context? = ComicCollectorApplication.context
        ): Int {
            val value = TypedValue()
            context?.theme?.resolveAttribute(attr, value, true)
            return value.data
        }
    }

    inner class ResultFragmentManager {
        val fragment: Flow<Fragment?> = filterViewModel.filter.mapLatest {
            when (it.viewOption) {
                FullIssue::class            -> issueListFragment
                Character::class            -> characterListFragment
                FullSeries::class           -> seriesListFragment
                NameDetailAndCreator::class -> seriesListFragment
                else                        -> throw IllegalStateException("illegal viewOption: ${it.viewOption}")
            }
        }

        private var seriesListFragment: SeriesListFragment? = null
            get() {
                if (field == null) {
                    field = SeriesListFragment.newInstance()
                }
                return field
            }
        private var issueListFragment: IssueListFragment? = null
            get() {
                if (field == null) {
                    field = IssueListFragment.newInstance()
                }
                return field
            }
        private var characterListFragment: CharacterListFragment? = null
            get() {
                if (field == null) {
                    field = CharacterListFragment.newInstance()
                }
                return field
            }
    }

    override fun updateFilter(filter: SearchFilter) {
        Log.d(TAG, "updateFilter")
        filterViewModel.setFilter(filter)
    }
}