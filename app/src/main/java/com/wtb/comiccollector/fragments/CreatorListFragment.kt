package com.wtb.comiccollector.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.R
import com.wtb.comiccollector.database.models.Creator
import com.wtb.comiccollector.database.models.FullCreator
import com.wtb.comiccollector.database.models.Series
import com.wtb.comiccollector.fragments_view_models.CreatorListViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val TAG = APP + "CreatorListFragment"

@ExperimentalCoroutinesApi
class CreatorListFragment : ListFragment<Series>() {

    private val viewModel: CreatorListViewModel by viewModels()

    override fun onResume() {
        super.onResume()
        callback?.setTitle()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                filterViewModel.filter.collectLatest { filter ->
                    Log.d(TAG, "Updating filter: ${filter.mSortType?.order}")
                    viewModel.setFilter(filter)
                }
            }
        }
    }

    override fun getLayoutManager(): RecyclerView.LayoutManager = LinearLayoutManager(context)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = CreatorAdapter()
        listRecyclerView.adapter = adapter

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.creatorList.collectLatest {
                    adapter.submitData(it)
                }
            }
        }
    }

    inner class CreatorAdapter :
        PagingDataAdapter<FullCreator, CreatorHolder>(DIFF_CALLBACK) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CreatorHolder =
            CreatorHolder(parent)

        override fun onBindViewHolder(holder: CreatorHolder, position: Int) {
            getItem(position)?.let { holder.bind(it) }
        }
    }

    inner class CreatorHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.list_item_character, parent, false)
    ), View.OnClickListener {

        private lateinit var item: FullCreator
        private val nameTextView: TextView =
            itemView.findViewById(R.id.list_item_character_name_text)
        private val alterEgoTextView: TextView = itemView.findViewById(R.id.list_item_alter_ego)
        private val publisherTextView: TextView =
            itemView.findViewById(R.id.list_item_char_publisher)

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(item: FullCreator) {
            this.item = item
            nameTextView.text = this.item.creator.name
//            alterEgoTextView.text = this.item.flatMap { it: FullCreator -> it.nameDetail.name }
            val bb = this.item.nameDetail.fold(String(), { acc, fullCreator ->
                acc + "${fullCreator.name}, "
            })
            alterEgoTextView.text = bb
        }

        override fun onClick(v: View?) {
            (callback as CreatorListCallback?)?.onCreatorSelected(item.creator)
        }
    }

    interface CreatorListCallback : ListFragmentCallback {
        fun onCreatorSelected(creator: Creator)
    }

    companion object {
        @JvmStatic
        fun newInstance() = CreatorListFragment()

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<FullCreator>() {
            override fun areItemsTheSame(oldItem: FullCreator, newItem: FullCreator): Boolean =
                oldItem.creator.creatorId == newItem.creator.creatorId

            override fun areContentsTheSame(
                oldItem: FullCreator,
                newItem: FullCreator
            ): Boolean =
                oldItem == newItem
        }
    }
}