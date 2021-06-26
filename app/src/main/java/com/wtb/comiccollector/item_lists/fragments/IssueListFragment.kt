package com.wtb.comiccollector.item_lists.fragments

import android.animation.ValueAnimator
import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.*
import android.view.animation.AccelerateInterpolator
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
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
import com.wtb.comiccollector.item_lists.view_models.IssueListViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val TAG = APP + "IssueListFragment"

@ExperimentalCoroutinesApi
class IssueListFragment : Fragment() {

    private val issueListViewModel: IssueListViewModel by viewModels()
    private val filterViewModel: FilterViewModel by viewModels({ requireActivity() })

    private lateinit var issueGridView: RecyclerView
    private var issueListCallback: IssueListCallback? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        issueListCallback = context as IssueListCallback?
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        lifecycleScope.launch {
            filterViewModel.filter.collectLatest { filter ->
                updateSeriesDetailFragment(filter.mSeries?.seriesId)
                issueListViewModel.setFilter(filter)
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_item_list, container, false)

        issueGridView = view.findViewById(R.id.results_frame) as RecyclerView
        val itemDecoration = ItemOffsetDecoration(24)
        issueGridView.addItemDecoration(itemDecoration)
        issueGridView.layoutManager = GridLayoutManager(context, 2)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = IssueAdapter()
        issueGridView.adapter = adapter

        lifecycleScope.launch {
            issueListViewModel.issueList()?.collectLatest { adapter.submitData(it) }
        }

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
        issueListCallback = null
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
    inner class IssueViewHolder(val parent: ViewGroup) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.list_item_issue, parent, false)
    ), View.OnClickListener {

        private var fullIssue: FullIssue? = null

        private val progressCover: ProgressBar = itemView.findViewById(R.id.progress_cover_download)
        private val coverImageView: ImageView = itemView.findViewById(R.id.list_item_cover)
        private val issueNumTextView: TextView = itemView.findViewById(R.id.list_item_issue)
        private val wrapper: ConstraintLayout = itemView.findViewById(R.id.wrapper)
        private val layout: CardView = itemView.findViewById(R.id.layout)

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(issue: FullIssue?) {
            this.fullIssue = issue
            this.fullIssue?.let { issueListViewModel.updateIssueCover(it) }
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
                coverImageView.setImageResource(R.drawable.ic_issue_add_cover)
            }

            val value = TypedValue()
            context?.theme?.resolveAttribute(R.attr.colorPrimaryMuted, value, true)

            if (fullIssue?.myCollection?.collectionId != null) {
                wrapper.setBackgroundResource(R.drawable.list_item_issue_card_background_in_collection)
                layout.cardElevation = 32F
            } else {
                wrapper.setBackgroundResource(R.drawable.list_item_issue_card_background)
                layout.cardElevation = 1F
            }

            issueNumTextView.text = this.fullIssue?.issue.toString()
        }

        override fun onClick(v: View?) {
            val issueId = fullIssue?.issue?.issueId
            issueId?.let { issueListCallback?.onIssueSelected(it) }
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

    interface IssueListCallback {
        fun onIssueSelected(issueId: Int)
        fun onNewIssue(issueId: Int)
    }

    companion object {
        @JvmStatic
        fun newInstance() = IssueListFragment()
    }

}