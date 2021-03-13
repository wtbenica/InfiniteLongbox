package com.wtb.comiccollector

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.wtb.comiccollector.GroupListFragments.CreatorListFragment
import com.wtb.comiccollector.GroupListFragments.SeriesListFragment

class TabFragment : Fragment() {

    private lateinit var collectionAdapter: CollectionAdapter
    private lateinit var viewPager: ViewPager2

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        (requireActivity() as MainActivity).supportActionBar?.title = getString(R.string.app_name)
        return inflater.inflate(R.layout.tab_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val tabLayout = view.findViewById(R.id.tab_layout) as TabLayout
        viewPager = view.findViewById(R.id.pager)
        collectionAdapter = CollectionAdapter(this)
        viewPager.adapter = collectionAdapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = resources.getStringArray(R.array.grouping_options)[position]
        }.attach()
    }

    companion object {
        fun newInstance() = TabFragment().apply {
            arguments = Bundle().apply {

            }
        }
    }
}


class CollectionAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return if (position == 0) {
            SeriesListFragment()
        } else {
            CreatorListFragment()
        }
    }
}
