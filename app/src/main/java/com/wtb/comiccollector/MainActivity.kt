package com.wtb.comiccollector

import android.app.Activity
import android.content.Context
import android.content.res.Resources
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
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.ContentFrameLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.*
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.MutableLiveData
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.*
import com.wtb.comiccollector.database.models.Creator
import com.wtb.comiccollector.database.models.Series
import com.wtb.comiccollector.issue_details.fragments.IssueDetailFragment
import com.wtb.comiccollector.item_lists.fragments.IssueListFragment
import com.wtb.comiccollector.item_lists.fragments.SeriesListFragment
import com.wtb.comiccollector.views.FilterView
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job

const val APP = "CC_"
private const val TAG = APP + "MainActivity"

fun dpToPx(context: Context, dp: Number): Float {
    val r: Resources = context.resources
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp.toFloat(),
        r.displayMetrics
    )
}

@ExperimentalCoroutinesApi
class MainActivity : AppCompatActivity(), SeriesListFragment.SeriesListCallback,
    FilterView.FilterCallback,
    IssueListFragment.IssueListCallback,
    SeriesInfoDialogFragment.SeriesInfoDialogCallback,
    NewCreatorDialogFragment.NewCreatorDialogCallback {

    private var filterView: FilterView? = null
    private var bottomSheetBehavior: BottomSheetBehavior<*>? = null

    private var posBottom = 0
    private var posTop = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_ComicCollector)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)
        setSupportActionBar(findViewById(R.id.action_bar))
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)

        if (currentFragment == null) {
            val fragment = SearchFilter().getFragment(this)
            supportFragmentManager
                .beginTransaction()
                .add(R.id.fragment_container, fragment)
                .commit()
        }

        val root: CoordinatorLayout = findViewById(R.id.main_layout)
        initWindowInsets(root, true, false)
        initBottomSheet()
        initNetwork()
    }

    private fun initBottomSheet() {
        filterView = findViewById<FilterView>(R.id.filter_view)?.apply {
            callback = this@MainActivity
        }

        filterView?.let {
            if (it.behavior is BottomSheetBehavior<*>) {
                bottomSheetBehavior = it.behavior as BottomSheetBehavior<*>
            }
        }

        bottomSheetBehavior?.let {
            filterView?.visibleState = it.state
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

    private fun initWindowInsets(view: View, setTop: Boolean, setBottom: Boolean) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            if (posBottom == 0) {
                posTop = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
                posBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            }

            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                if (setTop && setBottom) {
                    updateMargins(top = posTop, bottom = posBottom)
                } else if (setTop) {
                    updateMargins(top = posTop)
                } else if (setBottom) {
                    Log.d(TAG, "Updating bottom margin: $posBottom")
                    updateMargins(bottom = posBottom)
                }
            }

            insets
        }
    }

    // SeriesListFragment.SeriesListCallbacks
    override fun onSeriesSelected(series: Series) {
        filterView?.addFilterItem(series)
    }

    // FilterView.FilterCallback
    override fun onFilterChanged(filter: SearchFilter) {
        try {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, filter.getFragment(this))
                .addToBackStack(null)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit()
        } catch (e: IllegalStateException) {
            Log.d(TAG, "onFilterChanged: $e")
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
    override fun onIssueSelected(issueId: Int) {
        val fragment = IssueDetailFragment.newInstance(issueId, false)
        supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(
                R.anim.nav_default_pop_enter_anim,
                R.anim.nav_default_pop_exit_anim,
                R.anim.nav_default_pop_enter_anim,
                R.anim.nav_default_pop_exit_anim
            )
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onNewIssue(issueId: Int) {
        val fragment = IssueDetailFragment.newInstance(issueId)

        supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(
                R.anim.nav_default_pop_enter_anim,
                R.anim.nav_default_pop_exit_anim,
                R.anim.nav_default_pop_enter_anim,
                R.anim.nav_default_pop_exit_anim
            )
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    // SeriesInfoDialogCallback
    override fun onSaveSeriesClick(dialog: DialogFragment, series: Series) {
        // TODO: MainActivity onSaveSeriesClick
        dialog.dismiss()
    }

    override fun onCancelClick(dialog: DialogFragment) {
        // TODO: MainActivity onCancelClick
        dialog.dismiss()
    }

    // NewCreatorDialogCallback
    override fun onSaveCreatorClick(dialog: DialogFragment, creator: Creator) {
        // TODO: Not yet implemented
        dialog.dismiss()
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
}