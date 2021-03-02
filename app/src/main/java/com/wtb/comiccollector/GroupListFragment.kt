package com.wtb.comiccollector

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDate
import java.util.*

const val ARG_FILTER_ID = "Series Filter"
const val ARG_CREATOR_FILTER = "Creator Filter"
const val ARG_DATE_FILTER_START = "Date Filter Start"
const val ARG_DATE_FILTER_END = "Date Filter End"

abstract class GroupListFragment<T> : Fragment() {

    interface Callbacks {
        fun onSeriesSelected(seriesId: UUID)
        fun onCreatorSelected(creatorId: UUID)
        fun onNewIssue(issueId: UUID)
    }

    abstract var recyclerView: RecyclerView

    abstract var filterId: UUID?
    abstract var dateFilterStart: LocalDate?
    abstract var dateFilterEnd: LocalDate?

    abstract var callbacks: Callbacks?

    abstract val viewModel: GroupListViewModel<T>
    abstract var itemList: List<T>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        filterId = arguments?.getSerializable(ARG_FILTER_ID) as UUID?
        dateFilterStart = arguments?.getSerializable(ARG_DATE_FILTER_START) as LocalDate?
        dateFilterEnd = arguments?.getSerializable(ARG_DATE_FILTER_END) as LocalDate?
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
                val issue = Issue()
                viewModel.addIssue(issue)
                callbacks?.onNewIssue(issue.issueId)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun runLayoutAnimation(view: RecyclerView) {
        val context = view.context
        val controller: LayoutAnimationController =
            AnimationUtils.loadLayoutAnimation(context, R.anim.layout_animation_fall_down)

        view.layoutAnimation = controller
        view.adapter?.notifyDataSetChanged()
        view.scheduleLayoutAnimation()
    }

    abstract class MyAdapter<T>(var itemList: List<T>) :
        RecyclerView.Adapter<MyHolder<T>>() {

        private var lastPosition = -1

        override fun onBindViewHolder(holder: MyHolder<T>, position: Int) {
            val item = itemList[position]
            holder.bind(item)
        }

        override fun getItemCount(): Int = itemList.size
    }

    abstract class MyHolder<T>(view: View) : RecyclerView.ViewHolder(view),
        View.OnClickListener {
        abstract var item: T

        init {
            @Suppress("LeakingThis")
            itemView.setOnClickListener(this)
        }

        abstract fun bind(item: T)
    }
}