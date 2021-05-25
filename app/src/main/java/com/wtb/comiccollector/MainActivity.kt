package com.wtb.comiccollector

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import com.wtb.comiccollector.database.models.Creator
import com.wtb.comiccollector.database.models.Series
import com.wtb.comiccollector.issue_details.fragments.IssueDetailFragment
import com.wtb.comiccollector.item_lists.fragments.IssueListFragment

const val APP = "CC_"

class MainActivity : AppCompatActivity(),
    IssueListFragment.Callbacks,
    SearchFragment.Callbacks,
    SeriesInfoDialogFragment.SeriesInfoDialogListener,
    NewCreatorDialogFragment.NewCreatorDialogListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.action_bar))

        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)

        if (currentFragment == null) {
            val fragment = SearchFragment.newInstance()
            supportFragmentManager
                .beginTransaction()
                .add(R.id.fragment_container, fragment)
                .commit()
        }
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
}