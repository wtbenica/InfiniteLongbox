package com.wtb.comiccollector.fragments

import android.animation.ValueAnimator
import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
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
import com.wtb.comiccollector.MainActivity.Companion.getColorFromAttr
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

class IssueNameBox(context: Context, attrs: AttributeSet) :
    ConstraintLayout(context, attrs)

@ExperimentalCoroutinesApi
class IssueListFragment : ListFragment<FullIssue, IssueListFragment.IssueViewHolder>() {

    override val viewModel: IssueListViewModel by viewModels()

    private fun updateSeriesDetailFragment(series: FullSeries) {
        val seriesDetailBox = SeriesDetailBox(requireContext(), series)
        details.addView(seriesDetailBox)
        updateBottomPadding()
    }

    override fun getLayoutManager(): RecyclerView.LayoutManager {
        return GridLayoutManager(context, NUM_COLS)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val itemDecoration =
            ItemOffsetDecoration(
                resources.getDimension(R.dimen.margin_default).toInt() * 3 / 2,
                numCols = NUM_COLS
            )
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
            IssueViewHolder(parent)

        override fun onBindViewHolder(holder: IssueViewHolder, position: Int) {
            holder.bind(getItem(position))
        }
    }

    inner class IssueViewHolder(val parent: ViewGroup) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.list_item_issue, parent, false)
    ), View.OnClickListener, AddCollectionButton.AddCollectionCallback {
        private var fullIssue: FullIssue? = null
        private val progressCover: ProgressBar = itemView.findViewById(R.id.progress_cover_download)
        private val wrapper: ConstraintLayout = itemView.findViewById(R.id.wrapper)
        private val bg: ImageView = itemView.findViewById(R.id.bg_list_item_issue)
        private val coverImageView: ImageView = itemView.findViewById(R.id.list_item_cover)
        private val issueNameBox: ConstraintLayout =
            itemView.findViewById(R.id.list_item_issue_box)
        private val issueNumTextView: TextView =
            itemView.findViewById(R.id.list_item_issue_number_text)
        private val addCollectionButton: AddCollectionButton =
            itemView.findViewById(R.id.btn_issue_list_add_collection)
        private val issueVariantName: TextView =
            itemView.findViewById(R.id.list_item_issue_variant_name)


        init {
            itemView.setOnClickListener(this)
            addCollectionButton.callback = this
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

        fun bind(issue: FullIssue?) {
            this.fullIssue = issue

            this.fullIssue?.let { viewModel.updateIssueCover(it.issue.issueId) }
            val coverUri = this.fullIssue?.coverUri

            if (coverUri != null) {
                coverImageView.setImageURI(coverUri)
                val animation = ValueAnimator.ofFloat(0F, 1F)
                animation.addUpdateListener {
                    val value = it.animatedValue as Float
                    progressCover.alpha = 1 - value
                    bg.alpha = 1 - value
                    coverImageView.alpha = value
                }
                animation.interpolator = AccelerateInterpolator()
                animation.start()
            } else {
                coverImageView.setImageURI(null)
                val animation = ValueAnimator.ofFloat(0F, 1F)
                animation.addUpdateListener {
                    val value = it.animatedValue as Float
                    progressCover.alpha = value
                    bg.alpha = value
                    coverImageView.alpha = 1 - value
                }
                animation.interpolator = AccelerateInterpolator()
                animation.start()
            }

            val inCollection = fullIssue?.myCollection?.collectionId != null
            addCollectionButton.inCollection = inCollection

            val variantName: String? = this.fullIssue?.issue?.variantName
            if (variantName?.isNotEmpty() == true) {
                issueVariantName.text = variantName
                issueVariantName.visibility = VISIBLE
            } else {
                issueVariantName.visibility = GONE
            }

            val bgColor = context?.getColorFromAttr(
                if (inCollection)
                    R.attr.colorPrimary
                else
                    R.attr.colorPrimaryLight
            )

            bgColor?.let {
                wrapper.setBackgroundColor(it)
            }

            issueNameBox.setBackgroundResource(
                if (inCollection)
                    R.drawable.bg_issue_list_item_info
                else
                    R.drawable.bg_issue_list_item_not_in_collection
            )

            issueNumTextView.text = this.fullIssue?.issue?.issueNumRaw
        }
    }

    interface IssueListCallback : ListFragmentCallback {
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
        private const val NUM_COLS = 2
    }
}