package com.wtb.comiccollector

import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class IssueListFragment : Fragment() {

    interface Callbacks {
        fun onIssueSelected(issueId: UUID)
        fun onNewIssue()
    }

    private var callbacks: Callbacks? = null

    private val issueListViewModel by lazy { ViewModelProvider(this).get(IssueListViewModel::class.java) }
    private lateinit var issueRecyclerView: RecyclerView
    private var adapter: IssueAdapter? = IssueAdapter(emptyList())

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as Callbacks?
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_issue_list, container, false)

        issueRecyclerView = view.findViewById(R.id.issue_recycler_view)
        issueRecyclerView.layoutManager = LinearLayoutManager(context)
        issueRecyclerView.adapter = adapter

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        issueListViewModel.issueListLiveData.observe(
            viewLifecycleOwner,
            { issues ->
                issues?.let {
                    updateUI(issues)
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
                val issue = Issue()
                issueListViewModel.addIssue(issue)
                callbacks?.onIssueSelected(issue.issueId)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateUI(issues: List<Issue>) {
        adapter = IssueAdapter(issues)
        issueRecyclerView.adapter = adapter
    }

    private inner class IssueAdapter(var issues: List<Issue>) :
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
        private lateinit var issue: Issue

        private val coverImageView: ImageView = itemView.findViewById(R.id.list_item_cover)
        private val seriesTextView: TextView = itemView.findViewById(R.id.list_item_title)
        private val issueNumTextView: TextView = itemView.findViewById(R.id.list_item_issue)

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(issue: Issue) {
            this.issue = issue
            seriesTextView.text = this.issue.series
            issueNumTextView.text = this.issue.issueNum.toString()

//            TODO("Set cover image")
        }

        override fun onClick(v: View?) {
            callbacks?.onIssueSelected(issue.issueId)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = IssueListFragment()
    }
}