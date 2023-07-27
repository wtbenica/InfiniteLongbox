/*
 * Copyright (c) 2023. Wesley T. Benica
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.benica.infinite_longbox.fragments

import android.animation.ValueAnimator
import android.os.Bundle
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
import dev.benica.infinite_longbox.APP
import dev.benica.infinite_longbox.R
import dev.benica.infinite_longbox.database.models.BaseCollection
import dev.benica.infinite_longbox.database.models.FullIssue
import dev.benica.infinite_longbox.database.models.FullSeries
import dev.benica.infinite_longbox.database.models.Issue
import dev.benica.infinite_longbox.fragments_view_models.IssueListViewModel
import dev.benica.infinite_longbox.views.AddCollectionButton
import dev.benica.infinite_longbox.views.AddWishListButton
import dev.benica.infinite_longbox.views.SeriesDetailBox
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class IssueListFragment : ListFragment<FullIssue, IssueListFragment.IssueViewHolder>() {

    override val viewModel: IssueListViewModel by viewModels()
    override val minColSizeDp = 250
    private var mSeries: FullSeries? = null
    private var seriesDetailBox: SeriesDetailBox? = null

    private fun updateSeriesDetailFragment(series: FullSeries, newBox: Boolean = false) {
        seriesDetailBox.let {
            if (newBox || it == null) {
                details.removeAllViews()
                seriesDetailBox = SeriesDetailBox(requireContext(), series)
                details.addView(seriesDetailBox)
            } else {
                it.setSeries(series)
            }
        }

        updateBottomPadding()
    }

    override fun getLayoutManager(): RecyclerView.LayoutManager =
        GridLayoutManager(context, numCols)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val itemDecoration =
            ItemOffsetDecoration(
                itemOffset = resources.getDimension(R.dimen.item_offset_vert_list_item_issue)
                    .toInt(),
                itemOffsetHorizontal = resources.getDimension(R.dimen.item_offset_horz_list_item_issue)
                    .toInt(),
                numCols = numCols
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
            viewLifecycleOwner
        ) { fullSeries ->
            fullSeries?.let {
                mSeries = fullSeries
                updateSeriesDetailFragment(it)
                callback?.setTitle(fullSeries.series.seriesName)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        mSeries?.let {
            updateSeriesDetailFragment(it, true)
        }
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
        private val bg: ImageView = itemView.findViewById(R.id.list_item_issue_bg)
        private val coverImageView: ImageView = itemView.findViewById(R.id.list_item_cover)
        private val issueNameBox: ConstraintLayout =
            itemView.findViewById(R.id.list_item_issue_box)
        private val issueNumTextView: TextView =
            itemView.findViewById(R.id.list_item_issue_number_text)
        private val addCollectionButton: AddCollectionButton =
            itemView.findViewById(R.id.btn_issue_list_add_collection)
        private val addWishListButton: AddWishListButton =
            itemView.findViewById(R.id.btn_issue_list_add_wish_list)
        private val issueVariantName: TextView =
            itemView.findViewById(R.id.list_item_issue_variant_name)


        init {
            itemView.setOnClickListener(this)
            addCollectionButton.callback = this
            addWishListButton.callback = this
        }


        override fun onClick(v: View?) {
            val issue = fullIssue?.issue
            issue?.let { (callback as IssueListCallback?)?.onIssueSelected(it) }
        }

        override fun addToCollection(collId: Int) {
            this.fullIssue?.let { (callback as IssueListCallback?)?.addToCollection(it, collId) }
        }

        override fun removeFromCollection(collId: Int) {
            this.fullIssue?.let {
                (callback as IssueListCallback?)?.removeFromCollection(
                    it,
                    collId
                )
            }
        }

        fun bind(item: FullIssue?) {
            this.fullIssue = item

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
                    if (value == 1f) {
                        progressCover.visibility = GONE
                    }
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
                    if (value == 0f) {
                        progressCover.visibility = VISIBLE
                    }
                }
                animation.interpolator = AccelerateInterpolator()
                animation.start()
            }

            val collectionIds =
                this.fullIssue?.collectionItems?.map { it.userCollection } ?: emptyList()
            addCollectionButton.inCollection = BaseCollection.MY_COLL.id in collectionIds
            addWishListButton.inCollection = BaseCollection.WISH_LIST.id in collectionIds

            val issue = this.fullIssue?.issue
            if (issue?.variantName?.isNotEmpty() == true && issue.variantOf != null) {
                issueVariantName.text = issue.variantName
                issueVariantName.visibility = VISIBLE
            } else {
                issueVariantName.visibility = GONE
            }

            issueNameBox.setBackgroundResource(
                if (addCollectionButton.inCollection)
                    R.drawable.issue_list_item_in_collection_bg
                else
                    R.drawable.issue_list_item_not_in_collection_bg
            )

            issueNumTextView.text = this.fullIssue?.issue?.issueNumRaw
        }
    }

    interface IssueListCallback : ListFragmentCallback {
        fun onIssueSelected(issue: Issue)
        fun onNewIssue(issueId: Int)
        fun addToCollection(issue: FullIssue, collId: Int)
        fun removeFromCollection(issue: FullIssue, collId: Int)
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