package com.wtb.comiccollector.fragments

import android.animation.ValueAnimator
import android.content.Context
import android.os.Bundle
import android.view.*
import android.view.animation.AccelerateInterpolator
import android.widget.GridLayout.VERTICAL
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.MainActivity
import com.wtb.comiccollector.R
import com.wtb.comiccollector.database.models.FullIssue
import com.wtb.comiccollector.fragments_view_models.IssueListViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class IssueListFragment : ListFragment() {

    private val viewModel: IssueListViewModel by viewModels()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = context as IssueListCallback?
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setHasOptionsMenu(true)

        lifecycleScope.launch {
            filterViewModel.filter.collectLatest { filter ->
                updateSeriesDetailFragment(filter.mSeries?.seriesId)
                viewModel.setFilter(filter)
            }

            viewModel.series.collectLatest {
                it?.seriesName?.let { name -> callback?.setTitle(name) }
            }
        }
    }

    private fun updateSeriesDetailFragment(seriesId: Int?) {
        val fragment = SeriesDetailFragment.newInstance(seriesId)

        childFragmentManager.beginTransaction()
            .replace(R.id.details, fragment)
            .addToBackStack(null)
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .commitAllowingStateLoss()
    }

    override fun getLayoutManager(): RecyclerView.LayoutManager =
        StaggeredGridLayoutManager(2, VERTICAL).apply {
            gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val itemDecoration =
            ItemOffsetDecoration(resources.getDimension(R.dimen.card_elevation).toInt())
        listRecyclerView.addItemDecoration(itemDecoration)

        val adapter = IssueAdapter()
        listRecyclerView.adapter = adapter

        lifecycleScope.launch {
            viewModel.issueList.collectLatest { adapter.submitData(it) }
        }

        viewModel.seriesLiveData.observe(
            viewLifecycleOwner,
            {
                callback?.setTitle(it?.seriesName)
            }
        )
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_issue_list, menu)
    }

    //    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        return when (item.itemId) {
//            R.id.new_issue -> {
//                // TODO: Find solution to this. If issueNum is default (1), if there already
//                //  exists an issue number 1, then violates unique series/issue restraint in db
//                val issue = filter.mSeries?.let { Issue(seriesId = it.seriesId) } ?: Issue()
//                issueListViewModel.addIssue(issue)
//                issueListCallback?.onNewIssue(issue.issueId)
//                true
//            }
//            else           -> super.onOptionsItemSelected(item)
//        }
//    }
//
    inner class IssueAdapter :
        PagingDataAdapter<FullIssue, IssueViewHolder>(DIFF_CALLBACK) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IssueViewHolder =
            IssueViewHolder(parent)

        override fun onBindViewHolder(holder: IssueViewHolder, position: Int) {
            holder.bind(getItem(position))
        }
    }

    inner class IssueViewHolder(val parent: ViewGroup) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.list_item_issue, parent, false)
    ), View.OnClickListener {

        private var fullIssue: FullIssue? = null

        private val progressCover: ProgressBar = itemView.findViewById(R.id.progress_cover_download)
        private val coverImageView: ImageView = itemView.findViewById(R.id.list_item_cover)
        private val issueNumTextView: TextView = itemView.findViewById(R.id.list_item_issue)
        private val wrapper: ConstraintLayout = itemView.findViewById(R.id.wrapper)

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(issue: FullIssue?) {
            this.fullIssue = issue
            this.fullIssue?.let { viewModel.updateIssueCover(it) }
            val coverUri = this.fullIssue?.coverUri

            if (fullIssue?.cover != null) {
                val animation = ValueAnimator.ofFloat(0F, 1F)
                animation.addUpdateListener {
                    val value = it.animatedValue as Float
                    progressCover.alpha = 1 - value
                    coverImageView.alpha = value
                }
                animation.interpolator = AccelerateInterpolator()
                animation.start()
            } else {
                val animation = ValueAnimator.ofFloat(0F, 1F)
                animation.addUpdateListener {
                    val value = it.animatedValue as Float
                    progressCover.alpha = value
                    coverImageView.alpha = 1 - value
                }
                animation.interpolator = AccelerateInterpolator()
                animation.start()
            }

            if (coverUri != null) {
                this.coverImageView.setImageURI(coverUri)
            } else {
                coverImageView.setImageResource(R.drawable.cover_missing)
            }

            if (fullIssue?.myCollection?.collectionId != null) {
                issueNumTextView.setBackgroundColor(MainActivity.resolveThemeAttribute(
                    R.attr.colorPrimary,
                    context
                ))
            } else {
                context?.getColor(android.R.color.white)?.let {
                    issueNumTextView.setBackgroundColor(
                        it
                    )
                }
            }

            issueNumTextView.text = this.fullIssue?.issue.toString()
        }

        override fun onClick(v: View?) {
            val issueId = fullIssue?.issue?.issueId
            issueId?.let { (callback as IssueListCallback?)?.onIssueSelected(it) }
        }

    }

    interface IssueListCallback : ListFragmentCallback {
        fun onIssueSelected(issueId: Int)
        fun onNewIssue(issueId: Int)
    }

    companion object {
        @JvmStatic
        fun newInstance() = IssueListFragment()

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<FullIssue>() {
            override fun areItemsTheSame(oldItem: FullIssue, newItem: FullIssue): Boolean =
                oldItem.issue.issueId == newItem.issue.issueId

            override fun areContentsTheSame(oldItem: FullIssue, newItem: FullIssue): Boolean =
                oldItem == newItem
        }

        private const val TAG = APP + "IssueListFragment"
    }
}