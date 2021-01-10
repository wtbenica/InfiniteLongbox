package com.wtb.comiccollector

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class MainActivity : AppCompatActivity(),
    IssueListFragment.Callbacks {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)

        if (currentFragment == null) {
            val fragment = IssueListFragment.newInstance()
            supportFragmentManager
                .beginTransaction()
                .add(R.id.fragment_container, fragment)
                .commit()
        }
    }

    override fun onIssueSelected(issueId: UUID) {
        val fragment = IssueFragment.newInstance(issueId)

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}