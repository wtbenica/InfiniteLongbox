package com.wtb.comiccollector

import android.app.Activity
import android.content.Context
import android.graphics.Point
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ProgressBar
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.ContentFrameLayout
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.*
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_FADE
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.*
import com.wtb.comiccollector.database.models.*
import com.wtb.comiccollector.fragments.*
import com.wtb.comiccollector.fragments_view_models.FilterViewModel
import com.wtb.comiccollector.repository.Repository
import com.wtb.comiccollector.views.ProgressUpdateCard
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job

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

    internal val PEEK_HEIGHT
        get() = resources.getDimension(R.dimen.peek_height).toInt()

    internal val screenSizeInDp: Point
        get() {
            val dm = resources.displayMetrics
            return Point(
                (dm.widthPixels / dm.density).toInt(),
                (dm.heightPixels / dm.density).toInt()
            )
        }

    private val filterViewModel: FilterViewModel by viewModels()
    private var filterFragment: FilterFragment? = null
    private lateinit var mainLayout: CoordinatorLayout
    private lateinit var appBarLayout: AppBarLayout
    private lateinit var toolbar: Toolbar
    private lateinit var resultFragmentContainer: FragmentContainerView
    private lateinit var bottomSheet: FragmentContainerView
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>
    private lateinit var mAdView: AdView
    private lateinit var progressUpdate: ProgressUpdateCard
    private lateinit var progressBar: ProgressBar

    private fun setFragment(fragment: ListFragment<out ListItem, out RecyclerView.ViewHolder>) {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_ComicCollector)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d(TAG, "Flavor: ${BuildConfig.FLAVOR}")
        if (BuildConfig.FLAVOR == "free") {
            MobileAds.initialize(this)

            mAdView = findViewById(R.id.ad_view)
            val adRequest = AdRequest.Builder().build()
            mAdView.loadAd(adRequest)
            mAdView.visibility = View.VISIBLE
        }

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

        initWindowInsets()
        initBottomSheet()
        initNetwork()

        Repository.get().beginStaticUpdate(progressUpdate, this)

        filterViewModel.fragment.observe(
            this,
            { frag ->
                frag?.let { setFragment(it) }
            }
        )
    }

    override fun onStop() {
        Repository.get().cleanUpImages()
        super.onStop()
    }

    private fun initBottomSheet() {
        bottomSheetBehavior = from(bottomSheet)
        bottomSheetBehavior.apply {
            peekHeight = PEEK_HEIGHT
            isHideable = false
        }

        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
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
        filterFragment?.addFilterItem(series)
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

    override fun setProgressBar(isHidden: Boolean) {
        runOnUiThread {
            progressBar.visibility = if (isHidden) View.GONE else View.VISIBLE
        }
    }

    // IssueListFragment.IssueListCallback
    override fun onIssueSelected(issue: Issue) {
        val fragment = IssueDetailFragment.newInstance(
            issueSelectedId = issue.issueId,
            variantOf = issue.variantOf
        )
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

        @ColorInt
        fun Context.getColorFromAttr(
            @AttrRes attrColor: Int,
            resolveRefs: Boolean = true
        ): Int {
            TypedValue().let {
                theme.resolveAttribute(attrColor, it, resolveRefs)
                return it.data
            }
        }
    }

    override fun updateFilter(filter: SearchFilter) {
        filterViewModel.setFilter(filter)
    }

    override fun addToCollection(issue: FullIssue) {
        Repository.get().addToCollection(issue)
    }

    override fun removeFromCollection(issue: FullIssue) {
        Repository.get().removeFromCollection(issue.issue.issueId)
    }
}