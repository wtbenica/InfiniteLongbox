package com.wtb.comiccollector.fragments

import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.wtb.comiccollector.R
import com.wtb.comiccollector.fragments_view_models.FilterViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
open class ListFragment : Fragment() {
    protected val PEEK_HEIGHT
        get() = resources.getDimension(R.dimen.peek_height).toInt()
    protected val filterViewModel: FilterViewModel by viewModels({ requireActivity() })
    protected lateinit var listRecyclerView: RecyclerView
}