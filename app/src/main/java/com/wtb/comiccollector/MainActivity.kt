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
import android.view.ViewGroup
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
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.*
import com.wtb.comiccollector.database.models.Creator
import com.wtb.comiccollector.database.models.Series
import com.wtb.comiccollector.fragments.IssueDetailFragment
import com.wtb.comiccollector.fragments.IssueListFragment
import com.wtb.comiccollector.fragments.SeriesListFragment
import com.wtb.comiccollector.view_models.FilterViewModel
import com.wtb.comiccollector.views.FilterFragment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import java.lang.Integer.max

const val APP = "CC_"
private const val TAG = APP + "MainActivity"

@ExperimentalCoroutinesApi
class MainActivity : AppCompatActivity(),
    SeriesListFragment.SeriesListCallback,
    IssueListFragment.IssueListCallback,
    SeriesInfoDialogFragment.SeriesInfoDialogCallback,
    NewCreatorDialogFragment.NewCreatorDialogCallback,
    FilterFragment.FilterFragmentCallback {

    private val PEEK_HEIGHT
        get() = resources.getDimension(R.dimen.peek_height).toInt()

    private var filterFragment: FilterFragment? = null

    private val filterViewModel: FilterViewModel by viewModels()
    private var fragmentContainer: FragmentContainerView? = null

    private var filterFragmentContainer: FragmentContainerView? = null
    private var bottomSheetBehavior: BottomSheetBehavior<*>? = null
    private var toolbar: Toolbar? = null

    private val resultFragmentManager by lazy {
        ResultFragmentManager()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_ComicCollector)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        toolbar = findViewById(R.id.action_bar)
        setSupportActionBar(toolbar)

        filterFragment = supportFragmentManager.findFragmentByTag(
            resources.getString(R.string.tag_filter_fragment)
        ) as FilterFragment?

        lifecycleScope.launch {
            resultFragmentManager.fragment.collectLatest { frag ->
                frag?.let { fragment ->
                    supportFragmentManager.beginTransaction()
                        .setCustomAnimations(
                            R.anim.fade_in,
                            R.anim.slide_out,
                            R.anim.fade_in,
                            R.anim.slide_out
                        )
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null)
                        .commit()

                    val tt = object : OnBackPressedCallback(true) {
                        override fun handleOnBackPressed() {
                            filterFragment?.onBackPressed()
                        }
                    }

                    onBackPressedDispatcher.addCallback(fragment, tt)
                }
            }
        }

        val root: CoordinatorLayout = findViewById(R.id.main_layout)
        initWindowInsets(root)
        initBottomSheet()
        initNetwork()
    }

    private fun initBottomSheet() {
        filterFragmentContainer = findViewById(R.id.filter_fragment_container)

        bottomSheetBehavior = filterFragmentContainer?.let { from(it) }
        bottomSheetBehavior?.peekHeight = PEEK_HEIGHT
        bottomSheetBehavior?.isHideable = false

        bottomSheetBehavior?.addBottomSheetCallback(object : BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                Log.d(TAG, "onStateChanged: ${getStateName(newState)}")
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                filterFragment?.onSlide(slideOffset)
            }
        })
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

    private fun initWindowInsets(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            val posTop = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            val posBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            val imeInsetBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val bottom = max(posBottom, imeInsetBottom)

            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                updateMargins(top = posTop, bottom = bottom)
            }

            insets
        }
    }

    // SeriesListFragment.SeriesListCallbacks
    override fun onSeriesSelected(series: Series) {
        Log.d(TAG, "ADDING SERIES $series")
        filterFragment?.addFilterItem(series)
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
    override fun onIssueSelected(issueId: Int) {
        val fragment = IssueDetailFragment.newInstance(issueId, false)
        supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(
                R.anim.fade_in,
                R.anim.slide_out,
                R.anim.fade_in,
                R.anim.slide_out
            )
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()

        val tt = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                fragmentContainer?.updatePadding(bottom = PEEK_HEIGHT)

                bottomSheetBehavior?.apply {
                    isHideable = false
                    state = STATE_COLLAPSED
                }

                supportFragmentManager.popBackStack()
            }
        }

        this.onBackPressedDispatcher.addCallback(fragment, tt)

        fragmentContainer?.updatePadding(bottom = 0)

        bottomSheetBehavior?.apply {
            isHideable = true
            state = STATE_HIDDEN
        }
    }

    override fun onNewIssue(issueId: Int) {
        val fragment = IssueDetailFragment.newInstance(issueId)

        supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(
                R.anim.fade_in,
                R.anim.slide_out,
                R.anim.fade_in,
                R.anim.slide_out
            )
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
        toolbar?.title = actual
    }

    override fun setToolbarScrollFlags(flags: Int) {
        Log.d(TAG, "setToolbarScrollFlags $flags")
        toolbar?.updateLayoutParams<AppBarLayout.LayoutParams> {
            setScrollFlags(flags)
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

        fun resolveThemeAttribute(context: Context, attr: Int): Int {
            val value = TypedValue()
            context.theme.resolveAttribute(attr, value, true)
            return value.data
        }
    }

    inner class ResultFragmentManager {
        val fragment: Flow<Fragment?> = filterViewModel.filter.mapLatest {
            when (it.returnsIssueList()) {
                true -> issueListFragment
                else -> seriesListFragment
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

//        fun getFragment(): Fragment? = when (filter?.returnsIssueList()) {
//            true -> issueListFragment
//            else -> seriesListFragment
//        }
    }
}