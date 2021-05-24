package com.wtb.comiccollector.GroupListFragments

import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.ArrayAdapter
import android.widget.ImageView
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
import com.wtb.comiccollector.GroupListViewModels.IssueListViewModel
import com.wtb.comiccollector.database.models.FullIssue
import com.wtb.comiccollector.database.models.Issue
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

        val fragment = SeriesDetailFragment.newInstance(filter.mSeries?.seriesId)

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
        val itemDecoration = ItemOffsetDecoration(16)
        issueGridView.addItemDecoration(itemDecoration)
        issueGridView.layoutManager = GridLayoutManager(context, 3)
        val adapter = IssAdapter()
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
            else -> super.onOptionsItemSelected(item)
        }
    }

    //    private fun updateUI() {
//        adapter = parentFragment?.context?.let { GridAdapt(it, this.issueList) }
//        issueGridView.adapter = adapter
////        runLayoutAnimation(issueGridView)
//    }
//
    private fun runLayoutAnimation(view: RecyclerView) {
        val context = view.context
        val controller: LayoutAnimationController = AnimationUtils.loadLayoutAnimation(
            context, R.anim.layout_animation_fall_down
        )

        view.layoutAnimation = controller
        view.adapter?.notifyDataSetChanged()
        view.scheduleLayoutAnimation()
    }

    private inner class GridAdapt(context: Context, issues: List<FullIssue>) :
        ArrayAdapter<FullIssue>(context, R.layout.list_item_issue, issues) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var listItemView = convertView

            if (listItemView == null) {
                listItemView = LayoutInflater.from(context).inflate(
                    R.layout.list_item_issue, parent, false
                )
            }

            val issue: FullIssue? = getItem(position)
            issueListViewModel.updateIssue(issue)
            val layout: CardView? = listItemView?.findViewById(R.id.layout)
            val coverImageView: ImageView? = listItemView?.findViewById(R.id.list_item_cover)
            val issueNumTextView: TextView? = listItemView?.findViewById(R.id.list_item_issue)
            val variantNameTextView: TextView? = listItemView?.findViewById(R.id.list_item_name)

            coverImageView?.scaleType = ImageView.ScaleType.FIT_CENTER
            if (issue?.coverUri != null) {
                coverImageView?.setImageURI(issue.coverUri)
                coverImageView?.scaleType = ImageView.ScaleType.FIT_XY
            } else {
                coverImageView?.setImageResource(R.drawable.ic_issue_add_cover)
            }

//            if (issue?.myCollection?.collectionId != null) {
//                layout?.setBackgroundResource(R.drawable.secondary_outline_white_background)
//            } else {
//                layout?.setBackgroundResource(R.drawable.primary_outline_white_background)
//            }
//
            issueNumTextView?.text = issue?.issue.toString()

//            listItemView?.setOnClickListener {
//                issue?.issue?.issueId?.let { id ->
//                    callbacks?.onIssueSelected(
//                        id,
//                        this@IssueListFragment.filter
//                    )
//                }
//            }

            return listItemView!!
        }
    }

    private inner class IssueAdapter(var issues: List<FullIssue>) :
        RecyclerView.Adapter<IssueHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IssueHolder {
            val view = layoutInflater.inflate(R.layout.list_item_issue, parent, false)
            return IssueHolder(view)
        }

        override fun onBindViewHolder(holder: IssueHolder, position: Int) {
            val issue = issues[position]
            holder.bind(issue)
        }

        override fun getItemCount(): Int = issues.size

    }

    private inner class IssueHolder(view: View) : RecyclerView.ViewHolder(view),
        View.OnClickListener {
        private lateinit var fullIssue: FullIssue

        private val coverImageView: ImageView = itemView.findViewById(R.id.list_item_cover)
        private val issueNumTextView: TextView = itemView.findViewById(R.id.list_item_issue)
        private val variantNameTextView: TextView = itemView.findViewById(R.id.list_item_name)

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(issue: FullIssue) {
            this.fullIssue = issue
            this.coverImageView.scaleType = ImageView.ScaleType.FIT_CENTER
            if (this.fullIssue.coverUri != null) {
                this.coverImageView.setImageURI(this.fullIssue.coverUri)
            } else {
                coverImageView.setImageResource(R.drawable.ic_issue_add_cover)
            }

            issueNumTextView.text = if (filter.mMyCollection) {
                this.fullIssue.issue.toString()
            } else {
                this.fullIssue.issue.issueNum.toString()
            }
        }

        override fun onClick(v: View?) {
            callbacks?.onIssueSelected(fullIssue.issue.issueId)
        }
    }

    interface Callbacks {
        fun onIssueSelected(issueId: Int)
        fun onNewIssue(issueId: Int)
    }

    companion object {
        @JvmStatic
        fun newInstance(filter: Filter): IssueListFragment {
            Log.d(TAG, "newInstance: ${filter.mSeries}")
            return IssueListFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_FILTER, filter)
                }
            }
        }
    }


    class ItemOffsetDecoration(val itemOffset: Int) : RecyclerView.ItemDecoration() {
        private var mItemOffset = itemOffset
        private var spanCount = 2;

        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            super.getItemOffsets(outRect, view, parent, state)

            outRect.top = mItemOffset
            outRect.right = mItemOffset / 2
            outRect.left = mItemOffset / 2
            outRect.bottom = mItemOffset
        }
    }

    inner class IssViewHolder(val parent: ViewGroup) :
        RecyclerView.ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.list_item_issue, parent, false)
        ), View.OnClickListener {

        private var fullIssue: FullIssue? = null
            private set

        private val coverImageView: ImageView = itemView.findViewById(R.id.list_item_cover)
        private val issueNumTextView: TextView = itemView.findViewById(R.id.list_item_issue)
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

            if (fullIssue?.myCollection?.collectionId != null) {
                layout.setBackgroundResource(R.drawable.card_background_alt)
            } else {
                layout.setBackgroundResource(R.drawable.card_background)
            }

            issueNumTextView.text = this.fullIssue?.issue.toString()
        }

        override fun onClick(v: View?) {
            val issueId = fullIssue?.issue?.issueId
            issueId?.let { callbacks?.onIssueSelected(it) }
        }

    }

    inner class IssAdapter() :
        PagingDataAdapter<FullIssue, IssViewHolder>(diffCallback) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IssViewHolder {
            return IssViewHolder(parent)
        }

        override fun onBindViewHolder(holder: IssViewHolder, position: Int) {
            holder.bind(getItem(position))
        }
    }

    val diffCallback = object : DiffUtil.ItemCallback<FullIssue>() {
        override fun areItemsTheSame(oldItem: FullIssue, newItem: FullIssue): Boolean =
            oldItem.issue.issueId == newItem.issue.issueId

        override fun areContentsTheSame(oldItem: FullIssue, newItem: FullIssue): Boolean =
            oldItem == newItem
    }

}