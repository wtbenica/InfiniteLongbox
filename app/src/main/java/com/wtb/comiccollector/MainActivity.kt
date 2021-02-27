package com.wtb.comiccollector

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import java.util.*

class MainActivity : AppCompatActivity(),
    IssueListFragment.Callbacks,
    SeriesListFragment.Callbacks,
    SeriesInfoDialogFragment.SeriesInfoDialogListener,
    NewCreatorDialogFragment.NewCreatorDialogListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)

        if (currentFragment == null) {
            val fragment = SeriesListFragment.newInstance()
            supportFragmentManager
                .beginTransaction()
                .add(R.id.fragment_container, fragment)
                .commit()
        }
    }

    override fun onIssueSelected(issueId: UUID) {
        val fragment = IssueDetailFragment.newInstance(issueId, false)
        supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(
                R.anim.fragment_open_enter,
                R.anim.fragment_fade_exit,
                R.anim.fragment_open_enter,
                R.anim.fragment_fade_exit
            )
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onSeriesSelected(seriesId: UUID) {
        val fragment = IssueListFragment.newInstance(seriesFilterId = seriesId)
        supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(
                R.anim.fragment_open_enter,
                R.anim.fragment_fade_exit,
                R.anim.fragment_open_enter,
                R.anim.fragment_fade_exit
            )
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onCreatorSelected(creatorId: UUID) {
        val fragment = SeriesListFragment.newInstance(creatorFilterId = creatorId)
        supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(
                R.anim.fragment_open_enter,
                R.anim.fragment_fade_exit,
                R.anim.fragment_open_enter,
                R.anim.fragment_fade_exit
            )
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onNewIssue(issueId: UUID) {
        val fragment = IssueDetailFragment.newInstance(issueId)

        supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(
                R.anim.fragment_open_enter,
                R.anim.fragment_fade_exit,
                R.anim.fragment_open_enter,
                R.anim.fragment_fade_exit
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