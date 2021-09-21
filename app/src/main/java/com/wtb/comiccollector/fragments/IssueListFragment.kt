package com.wtb.comiccollector.fragments

import android.animation.ValueAnimator
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.animation.AccelerateInterpolator
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.R
import com.wtb.comiccollector.database.models.FullIssue
import com.wtb.comiccollector.database.models.FullSeries
import com.wtb.comiccollector.database.models.Issue
import com.wtb.comiccollector.fragments_view_models.IssueListViewModel
import com.wtb.comiccollector.views.AddCollectionButton
import com.wtb.comiccollector.views.SeriesDetailBox
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class IssueListFragment : ListFragment<FullIssue, IssueListFragment.IssueViewHolder>() {

    override val viewModel: IssueListViewModel by viewModels()

    private fun updateSeriesDetailFragment(series: FullSeries) {
        val seriesDetailBox = SeriesDetailBox(requireContext(), series)
        details.addView(seriesDetailBox)
        updateBottomPadding()
    }

    override fun getLayoutManager(): RecyclerView.LayoutManager = GridLayoutManager(context, 2)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val itemDecoration =
            ItemOffsetDecoration(resources.getDimension(R.dimen.margin_narrow).toInt())
        listRecyclerView.addItemDecoration(itemDecoration)

        val adapter = getAdapter()
        listRecyclerView.adapter = adapter

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.itemList.collectLatest { pagingData ->
                    adapter.submitData(pagingData)
                }
            }
        }

        viewModel.seriesLiveData.observe(
            viewLifecycleOwner,
            { fullSeries ->
                fullSeries?.let { updateSeriesDetailFragment(it) }
                callback?.setTitle(fullSeries?.series?.seriesName)
            }
        )
    }

    override fun getAdapter(): IssueAdapter = IssueAdapter()

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_issue_list, menu)
    }

    override fun onDestroyView() {
        viewModel.saveIssueListState(listRecyclerView.layoutManager?.onSaveInstanceState())
        super.onDestroyView()
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
            if (viewType == 0)
                PrimaryViewHolder(parent)
            else
                VariantViewHolder(parent)


        override fun onBindViewHolder(holder: IssueViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        override fun getItemViewType(position: Int): Int {
            val fullIssue = getItem(position) as FullIssue
            val variantName = fullIssue.issue.variantName
            Log.d(
                TAG,
                "Variant name ${fullIssue.series.seriesName} ${fullIssue.issue.issueNumRaw} is $variantName"
            )
            return if (variantName.isBlank()) {
                0
            } else {
                1
            }
        }
    }

    abstract inner class IssueViewHolder(val parent: ViewGroup, val view: View) :
        RecyclerView.ViewHolder(view), View.OnClickListener,
        AddCollectionButton.AddCollectionCallback {
        protected var fullIssue: FullIssue? = null
        protected val progressCover: ProgressBar =
            itemView.findViewById(R.id.progress_cover_download)
        protected val coverImageView: ImageView = itemView.findViewById(R.id.list_item_cover)
        protected val issueNameBox: ConstraintLayout =
            itemView.findViewById(R.id.list_item_issue_box)
        protected val issueNumTextView: TextView =
            itemView.findViewById(R.id.list_item_issue_number_text)
        protected val addCollectionButton: AddCollectionButton =
            itemView.findViewById(R.id.btn_issue_list_add_collection)

        init {
            itemView.setOnClickListener(this)
            addCollectionButton.callback = this
        }

        abstract fun bind(issue: FullIssue?)

        override fun onClick(v: View?) {
            val issue = fullIssue?.issue
            issue?.let { (callback as IssueListCallback?)?.onIssueSelected(it) }
        }

        override fun addToCollection() {
            this.fullIssue?.let { (callback as IssueListCallback?)?.addToCollection(it) }
        }

        override fun removeFromCollection() {
            this.fullIssue?.let { (callback as IssueListCallback?)?.removeFromCollection(it) }
        }
    }

    inner class PrimaryViewHolder(parent: ViewGroup) : IssueViewHolder(
        parent,
        LayoutInflater.from(parent.context).inflate(R.layout.list_item_issue, parent, false)
    ) {
        override fun bind(issue: FullIssue?) {
            this.fullIssue = issue

            this.fullIssue?.let { viewModel.updateIssueCover(it.issue.issueId) }
            val coverUri = this.fullIssue?.coverUri

            if (coverUri != null) {
                coverImageView.setImageURI(coverUri)
            } else {
                coverImageView.setImageURI(null)
            }

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

            val inCollection = fullIssue?.myCollection?.collectionId != null
            addCollectionButton.inCollection = inCollection
            val bgColor = if (inCollection) {
                context?.getColor(R.color.fantasia_transparent)
            } else {
                context?.getColor(R.color.transparent_white)
            }
            bgColor?.let { issueNameBox.setBackgroundColor(it) }

            issueNumTextView.text = this.fullIssue?.issue?.issueNumRaw
        }
    }

    inner class VariantViewHolder(parent: ViewGroup) : IssueViewHolder(
        parent,
        LayoutInflater.from(parent.context).inflate(R.layout.list_item_issue_variant, parent, false)
    ) {
        private val issueVariantName: TextView =
            itemView.findViewById(R.id.list_item_issue_variant_name)

        override fun bind(issue: FullIssue?) {
            this.fullIssue = issue

            this.fullIssue?.let { viewModel.updateIssueCover(it.issue.issueId) }
            val coverUri = this.fullIssue?.coverUri

            if (coverUri != null) {
                coverImageView.setImageURI(coverUri)
            } else {
                coverImageView.setImageURI(null)
            }

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

            val inCollection = fullIssue?.myCollection?.collectionId != null
            addCollectionButton.inCollection = inCollection
            val bgColor = if (inCollection) {
                context?.getColor(R.color.fantasia_transparent)
            } else {
                context?.getColor(R.color.transparent_white)
            }
            bgColor?.let { issueNameBox.setBackgroundColor(it) }

            issueNumTextView.text = this.fullIssue?.issue?.issueNumRaw

            val variantName = this.fullIssue?.issue?.variantName
            issueVariantName.text = variantName
        }

        override fun onClick(v: View?) {
            val issue = fullIssue?.issue
            issue?.let { (callback as IssueListCallback?)?.onIssueSelected(it) }
        }

        override fun addToCollection() {
            this.fullIssue?.let { (callback as IssueListCallback?)?.addToCollection(it) }
        }

        override fun removeFromCollection() {
            this.fullIssue?.let { (callback as IssueListCallback?)?.removeFromCollection(it) }
        }
    }

    interface IssueListCallback : ListFragment.ListFragmentCallback {
        fun onIssueSelected(issue: Issue)
        fun onNewIssue(issueId: Int)
        fun addToCollection(issue: FullIssue)
        fun removeFromCollection(issue: FullIssue)
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