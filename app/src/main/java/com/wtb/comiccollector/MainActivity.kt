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
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.ContentFrameLayout
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.MutableLiveData
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
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
class MainActivity : AppCompatActivity(), SeriesListFragment.SeriesListCallbacks,
    FilterView.FilterCallback,
    IssueListFragment.Callbacks,
    SeriesInfoDialogFragment.SeriesInfoDialogListener,
    NewCreatorDialogFragment.NewCreatorDialogListener {

    private var fab: FloatingActionButton? = null
    private var filterView: FilterView? = null
    private var bottomSheetBehavior: BottomSheetBehavior<FilterView>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)
        setSupportActionBar(findViewById(R.id.action_bar))

        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)

        if (currentFragment == null) {
            val fragment = SearchFilter().getFragment(this)
            supportFragmentManager
                .beginTransaction()
                .add(R.id.fragment_container, fragment)
                .commit()
        }


        initBottomSheet()
        initNetwork()

        fab = findViewById(R.id.fab2)
        fab?.setOnClickListener {
            Log.d(TAG, "FAB PUNCHED!")
            bottomSheetBehavior?.state?.let {
                bottomSheetBehavior?.state = when (it) {
                    STATE_EXPANDED -> STATE_HALF_EXPANDED
                    STATE_HALF_EXPANDED -> STATE_COLLAPSED
                    STATE_COLLAPSED -> STATE_EXPANDED
                    else -> STATE_EXPANDED
                }
                bottomSheetBehavior?.state?.let { state -> filterView?.setVisibleState(state) }
            }
        }
    }

    private fun initBottomSheet() {
        filterView = findViewById<FilterView>(R.id.filter_view).apply {
            callback = this@MainActivity
        }

        filterView?.let {
            bottomSheetBehavior = from(it)
        }

        bottomSheetBehavior?.apply {
            isHideable = false
            state = STATE_EXPANDED
            addBottomSheetCallback(
                object : BottomSheetCallback() {
                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                        val stateName = getStateName(newState)
                        Log.d(TAG, "onStateChanged: $stateName")
                        filterView?.setVisibleState(newState)
                    }

                    override fun onSlide(bottomSheet: View, slideOffset: Float) {
                        Log.d(TAG, "onSlide: $slideOffset")
                    }
                }
            )
        }?.let {
            filterView?.setVisibleState(it.state)
        }
    }

    private fun initNetwork() {
        val connManager = getSystemService(ConnectivityManager::class.java)

        connManager.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
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

    override fun onSeriesSelected(series: Series) {
        filterView?.addFilterItem(series)
    }

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

    override fun onSaveSeriesClick(dialog: DialogFragment, series: Series) {
        // TODO: MainActivity onSaveSeriesClick
        dialog.dismiss()
    }

    override fun onSaveCreatorClick(dialog: DialogFragment, creator: Creator) {
        // TODO: Not yet implemented
        dialog.dismiss()
    }

    override fun onCancelClick(dialog: DialogFragment) {
        // TODO: MainActivity onCancelClick
        dialog.dismiss()
    }

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