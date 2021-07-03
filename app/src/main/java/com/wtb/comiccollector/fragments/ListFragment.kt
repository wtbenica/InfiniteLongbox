package com.wtb.comiccollector.fragments

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.RecyclerView
import com.wtb.comiccollector.R
import com.wtb.comiccollector.fragments_view_models.FilterViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
abstract class ListFragment : Fragment() {
    private val PEEK_HEIGHT
        get() = resources.getDimension(R.dimen.peek_height).toInt()

    private val viewModel: ListViewModel by viewModels()

    private val filterViewModel: FilterViewModel by viewModels({ requireActivity() })

    private lateinit var listRecyclerView: RecyclerView
    private var callback: ListCallback? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)

        callback = context as ListCallback?
    }
}

interface ListCallback

class ListViewModel: ViewModel() {

}
