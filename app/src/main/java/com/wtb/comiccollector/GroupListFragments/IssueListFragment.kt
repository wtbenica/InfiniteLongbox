package com.wtb.comiccollector.GroupListFragments

import android.content.Context
import android.os.Bundle
import android.view.*
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wtb.comiccollector.*
import com.wtb.comiccollector.GroupListViewModels.IssueListViewModel
import java.time.LocalDate

private const val TAG = APP + "NewIssueListFragment"
private const val ARG_SERIES_IDS = "Series Ids"

class IssueListFragment : Fragment() {

    interface Callbacks {
        fun onIssueSelected(issueId: Int)
        fun onNewIssue(issueId: Int)
    }

    private var callbacks: Callbacks? = null

    private var seriesFilterId: Int? = null
    private var creatorFilterId = mutableSetOf<Int>()
    private var dateFilterStart: LocalDate? = null
    private var dateFilterEnd: LocalDate? = null

    private val issueListViewModel by lazy {
        ViewModelProvider(this).get(IssueListViewModel::class.java)
    }

    private lateinit var issueRecyclerView: RecyclerView
    private var adapter: IssueAdapter? = IssueAdapter(emptyList())

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as Callbacks?
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        seriesFilterId = (arguments?.getSerializable(ARG_SERIES_IDS) as String?)?.toInt()
        creatorFilterId = Filter.deserialize(arguments?.getSerializable(ARG_CREATOR_IDS) as String?)
        dateFilterStart = arguments?.getSerializable(ARG_DATE_FILTER_START) as LocalDate?
        dateFilterEnd = arguments?.getSerializable(ARG_DATE_FILTER_END) as LocalDate?

        val fragment = SeriesDetailFragment.newInstance(seriesFilterId)
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

        issueRecyclerView = view.findViewById(R.id.results_frame)
        issueRecyclerView.layoutManager = LinearLayoutManager(context)
        issueRecyclerView.adapter = adapter

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        seriesFilterId?.let { issueListViewModel.loadSeries(it) }

        issueListViewModel.issueListLiveData.observe(
            viewLifecycleOwner,
            { issues ->
                issues?.let {
                    updateUI(issues)
                }
            }
        )

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
                val issue = seriesFilterId?.let { Issue(seriesId = it) } ?: Issue()
                issueListViewModel.addIssue(issue)
                callbacks?.onNewIssue(issue.issueId)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateUI(issues: List<FullIssue>) {
        adapter = IssueAdapter(issues)
        issueRecyclerView.adapter = adapter
        runLayoutAnimation(issueRecyclerView)
    }

    private fun runLayoutAnimation(view: RecyclerView) {
        val context = view.context
        val controller: LayoutAnimationController = AnimationUtils.loadLayoutAnimation(
            context, R.anim.layout_animation_fall_down
        )

        view.layoutAnimation = controller
        view.adapter?.notifyDataSetChanged()
        view.scheduleLayoutAnimation()
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
            issueNumTextView.text = this.fullIssue.issue.issueNum.toString()
//            variantNameTextView.text = this.fullIssue.issue.variantName
        }

        override fun onClick(v: View?) {
            callbacks?.onIssueSelected(fullIssue.issue.issueId)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(filter: Filter) =
            IssueListFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_SERIES_IDS, filter.seriesIds())
                    putSerializable(ARG_CREATOR_IDS, filter.creatorIds())
                    putSerializable(ARG_DATE_FILTER_START, dateFilterStart)
                    putSerializable(ARG_DATE_FILTER_END, dateFilterEnd)
                }
            }
    }
}