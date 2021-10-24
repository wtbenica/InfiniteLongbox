package com.wtb.comiccollector.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.R
import com.wtb.comiccollector.database.models.Creator
import com.wtb.comiccollector.database.models.FullCreator
import com.wtb.comiccollector.fragments_view_models.CreatorListViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi

private const val TAG = APP + "CreatorListFragment"

@ExperimentalCoroutinesApi
class CreatorListFragment : ListFragment<FullCreator, CreatorListFragment.CreatorHolder>() {

    override val viewModel: CreatorListViewModel by viewModels()

    override fun onResume() {
        super.onResume()
        callback?.setTitle()
    }

    override fun getLayoutManager(): RecyclerView.LayoutManager = LinearLayoutManager(context)
    override fun getAdapter() = CreatorAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        val itemOffsetDecoration = ItemOffsetDecoration(
            resources.getDimension(R.dimen.margin_wide).toInt()
        )
        listRecyclerView.addItemDecoration(itemOffsetDecoration)

        return view
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
        LayoutInflater.from(parent.context).inflate(R.layout.list_item_simple, parent, false)
    ), View.OnClickListener {

        private lateinit var item: FullCreator
        private val nameTextView: TextView =
            itemView.findViewById(R.id.list_item_issue_variant_name)
        private val alterEgoTextView: TextView = itemView.findViewById(R.id.list_item_alter_ego)
        private val publisherTextView: TextView =
            itemView.findViewById(R.id.list_item_char_publisher)
        private val bg: ImageView = itemView.findViewById(R.id.list_item_simple_bg)
        private val div: View = itemView.findViewById(R.id.divider_list_item_meta)

        init {
            itemView.setOnClickListener(this)
            div.visibility = View.GONE
        }

        fun bind(item: FullCreator) {
            this.item = item
            nameTextView.text = this.item.creator.name
//            alterEgoTextView.text = this.item.flatMap { it: FullCreator -> it.nameDetail.name }
            val nameDetails = this.item.nameDetail.fold(String(), { acc, fullCreator ->
                acc + "${fullCreator.name}, "
            })
            alterEgoTextView.text = nameDetails.removeSuffix(", ")
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