package com.wtb.comiccollector.item_lists.fragments

import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.util.TypedValue
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wtb.comiccollector.*
import com.wtb.comiccollector.database.models.FullIssue
import com.wtb.comiccollector.database.models.Issue
import com.wtb.comiccollector.item_lists.view_models.IssueListViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val TAG = APP + "IssueListFragment"

class IssueListFragment : Fragment() {

    private val issueListViewModel: IssueListViewModel by viewModels()
    private lateinit var filter: Filter
    private lateinit var issueGridView: RecyclerView
    private var callbacks: Callbacks? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as Callbacks?
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        filter = arguments?.getSerializable(ARG_FILTER) as Filter? ?: Filter()

        updateSeriesDetailFragment(filter.mSeries?.seriesId)
    }

    private fun updateSeriesDetailFragment(seriesId: Int?) {
        val fragment = SeriesDetailFragment.newInstance(seriesId)

        childFragmentManager.beginTransaction()
            .replace(R.id.details, fragment)
            .addToBackStack(null)
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .commit()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_item_list, container, false)

        issueGridView = view.findViewById(R.id.results_frame) as RecyclerView
        val itemDecoration = ItemOffsetDecoration(24)
        issueGridView.addItemDecoration(itemDecoration)
        issueGridView.layoutManager = GridLayoutManager(context, 2)
        val adapter = IssueAdapter()
        issueGridView.adapter = adapter

        lifecycleScope.launch {
            issueListViewModel.issueList(filter).collectLatest { adapter.submitData(it) }
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        issueListViewModel.setFilter(filter)

        issueListViewModel.seriesLiveData.observe(
            viewLifecycleOwner,
            {
                (requireActivity() as MainActivity).supportActionBar?.apply {
                    it?.let { title = it.seriesName }
                }
            }
        )
    }

    override fun onDetach() {
        super.onDetach()
        callbacks = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_issue_list, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.new_issue -> {
                // TODO: Find solution to this. If issueNum is default (1), if there already
                //  exists an issue number 1, then violates unique series/issue restraint in db
                val issue = filter.mSeries?.let { Issue(seriesId = it.seriesId) } ?: Issue()
                issueListViewModel.addIssue(issue)
                callbacks?.onNewIssue(issue.issueId)
                true
            }
            else           -> super.onOptionsItemSelected(item)
        }
    }

    class ItemOffsetDecoration(itemOffset: Int) : RecyclerView.ItemDecoration() {
        private var mItemOffset = itemOffset
        private var spanCount = 2

        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            super.getItemOffsets(outRect, view, parent, state)

            outRect.top = mItemOffset
            outRect.bottom = mItemOffset
            outRect.left = mItemOffset / 2
            outRect.right = mItemOffset / 2
        }
    }

    inner class IssueViewHolder(val parent: ViewGroup) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.list_item_issue, parent, false)
    ), View.OnClickListener {

        private var fullIssue: FullIssue? = null

        private val coverImageView: ImageView = itemView.findViewById(R.id.list_item_cover)
        private val issueNumTextView: TextView = itemView.findViewById(R.id.list_item_issue)
        private val wrapper = itemView.findViewById<LinearLayout>(R.id.wrapper)
        private val layout: CardView = itemView.findViewById(R.id.layout)

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(issue: FullIssue?) {
            this.fullIssue = issue
            issueListViewModel.updateIssue(this.fullIssue)
            val coverUri = this.fullIssue?.coverUri

            if (coverUri != null) {
                this.coverImageView.setImageURI(coverUri)
            } else {
                coverImageView.setImageResource(R.drawable.ic_issue_add_cover)
            }

            val value = TypedValue()
            context?.theme?.resolveAttribute(R.attr.colorPrimaryMuted, value, true)

            if (fullIssue?.myCollection?.collectionId != null) {
                wrapper.setBackgroundResource(R.drawable.card_background_in_collection)
                layout.cardElevation = 32F
//                issueNumTextView.setTextColor(Color.WHITE)
//                layout.setCardBackgroundColor(Color.BLACK)
            } else {
                wrapper.setBackgroundResource(R.drawable.card_background_regular)
                layout.cardElevation = 1F
//                issueNumTextView.setTextColor(Color.BLACK)
//                layout.setCardBackgroundColor(Color.WHITE)
            }

            issueNumTextView.text = this.fullIssue?.issue.toString()
        }

        override fun onClick(v: View?) {
            val issueId = fullIssue?.issue?.issueId
            issueId?.let { callbacks?.onIssueSelected(it) }
        }

    }

    inner class IssueAdapter :
        PagingDataAdapter<FullIssue, IssueViewHolder>(diffCallback) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IssueViewHolder {
            return IssueViewHolder(parent)
        }

        override fun onBindViewHolder(holder: IssueViewHolder, position: Int) {
            holder.bind(getItem(position))
        }
    }

    val diffCallback = object : DiffUtil.ItemCallback<FullIssue>() {
        override fun areItemsTheSame(oldItem: FullIssue, newItem: FullIssue): Boolean =
            oldItem.issue.issueId == newItem.issue.issueId

        override fun areContentsTheSame(oldItem: FullIssue, newItem: FullIssue): Boolean =
            oldItem == newItem
    }

    interface Callbacks {
        fun onIssueSelected(issueId: Int)
        fun onNewIssue(issueId: Int)
    }

    companion object {
        @JvmStatic
        fun newInstance(filter: Filter): IssueListFragment {
            return IssueListFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_FILTER, filter)
                }
            }
        }
    }

}