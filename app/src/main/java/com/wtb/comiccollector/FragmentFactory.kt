package com.wtb.comiccollector

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * A simple [Fragment] subclass.
 * Use the [FragmentFactory.newInstance] factory method to
 * create an instance of this fragment.
 */
@ExperimentalCoroutinesApi
class FragmentFactory : Fragment() {

    private val filterViewModel: FilterViewModel by viewModels({ requireActivity() })
    private var prevFilter: SearchFilter? = null
    private var filter: SearchFilter? = null
        set(value) {
            prevFilter = field
            field = value
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            filterViewModel.filter.collectLatest { filter ->
                this@FragmentFactory.filter = filter
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_factory, container, false)
    }

    companion object {
        @JvmStatic
        fun newInstance() = FragmentFactory()
    }
}