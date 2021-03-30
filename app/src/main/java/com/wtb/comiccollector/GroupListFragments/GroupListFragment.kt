package com.wtb.comiccollector.GroupListFragments

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wtb.comiccollector.GroupListViewModels.GroupListViewModel
import com.wtb.comiccollector.GroupListViewModels.SeriesListViewModel
import com.wtb.comiccollector.Issue
import com.wtb.comiccollector.R
import java.time.LocalDate

const val ARG_FILTER_ID = "Series Filter"
const val ARG_CREATOR_FILTER = "Creator Filter"
const val ARG_DATE_FILTER_START = "Date Filter Start"
const val ARG_DATE_FILTER_END = "Date Filter End"

abstract class GroupListFragment<T : GroupListFragment.Indexed, U : GroupListFragment<T, U>.MyAdapter<T>>
    : Fragment() {

    interface Callbacks {
        fun onSeriesSelected(seriesId: Int)
        fun onCreatorSelected(creatorId: Int)
        fun onNewIssue(issueId: Int)
    }

    interface Indexed {
        fun getIndex(): Char
    }

    abstract val viewModel: GroupListViewModel<T>

    protected var callbacks: Callbacks? = null
    protected lateinit var itemList: List<T>

    private var filterId: Int? = null
    private var dateFilterStart: LocalDate? = null
    private var dateFilterEnd: LocalDate? = null

    private lateinit var search: EditText
    private lateinit var itemListRecyclerView: RecyclerView
    private lateinit var indexRecyclerView: RecyclerView

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as Callbacks?
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        filterId = arguments?.getSerializable(ARG_FILTER_ID) as Int?
        dateFilterStart = arguments?.getSerializable(ARG_DATE_FILTER_START) as LocalDate?
        dateFilterEnd = arguments?.getSerializable(ARG_DATE_FILTER_END) as LocalDate?
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_issue_list, container, false)

        itemList = emptyList()

        search = view.findViewById(R.id.search_tv) as EditText
        itemListRecyclerView = view.findViewById(R.id.issue_recycler_view) as RecyclerView
        itemListRecyclerView.layoutManager = LinearLayoutManager(context)
        itemListRecyclerView.adapter = getAdapter()

        indexRecyclerView = view.findViewById(R.id.index_recycler_view) as RecyclerView
        indexRecyclerView.layoutManager = LinearLayoutManager(context)
        indexRecyclerView.adapter = IndexAdapter(getIndexList())

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        filterId?.let { viewModel.filter(it) }

        viewModel.objectListLiveData.observe(
            viewLifecycleOwner,
            { objectList ->
                objectList?.let {
                    this.itemList = it
                    updateUI()
                }
            }
        )

        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.filter(text = s.toString())
            }

            override fun afterTextChanged(s: Editable?) {

            }
        })
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

    private fun updateUI() {
        itemListRecyclerView.adapter = getAdapter()
        runLayoutAnimation(itemListRecyclerView)
        indexRecyclerView.adapter = IndexAdapter(getIndexList())
    }

    private fun getIndexList(): List<Pair<Char, Int>> {
        val result = LinkedHashMap<Char, Int>()
        itemList.forEachIndexed { index, item ->
            val item1 = item.getIndex()

            if (index == 0) {
                result[item1] = index
            } else {
                val item2 = try {
                    itemList[index + 1]
                } catch (e: IndexOutOfBoundsException) {
                    '#'
                }

                if (item1 != item2 && result[item1] == null) {
                    result[item1] = index
                }
            }
        }

        return result.toList()
    }

    abstract fun getAdapter(): U

    private fun runLayoutAnimation(view: RecyclerView) {
        val context = view.context
        val controller: LayoutAnimationController =
            AnimationUtils.loadLayoutAnimation(context, R.anim.layout_animation_fall_down)

        view.layoutAnimation = controller
        view.adapter?.notifyDataSetChanged()
        view.scheduleLayoutAnimation()
    }

    abstract inner class MyAdapter<T>(var itemList: List<T>) :
        RecyclerView.Adapter<MyHolder<T>>() {

        private var lastPosition = -1

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder<T> {
            val view = layoutInflater.inflate(R.layout.list_item_series, parent, false)
            return getHolder(view)
        }

        override fun onBindViewHolder(holder: MyHolder<T>, position: Int) {
            val item = itemList[position]
            holder.bind(item)
        }

        abstract fun getHolder(view: View): MyHolder<T>

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

    enum class SeriesFilter(
        private val s: String,
        val onSelect: (viewModel: SeriesListViewModel) -> Unit
    ) {
        NONE("None", { }),
        CREATOR("Creator", { }),
        DATE("Date Range", { });

        override fun toString(): String {
            return s
        }
    }

    enum class CreatorFilter(private val s: String, val onSelect: () -> Unit) {
        NONE("None", { }),
        COCREATOR("Cocreator", { }),
        DATE("Date Range", { });

        override fun toString(): String {
            return s
        }
    }

    inner class IndexAdapter(private val indexList: List<Pair<Char, Int>>) :
        RecyclerView.Adapter<IndexViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IndexViewHolder {
            return IndexViewHolder(layoutInflater.inflate(R.layout.index_item, parent, false))
        }

        override fun onBindViewHolder(holder: IndexViewHolder, position: Int) {
            holder.bind(indexList[position])
        }

        override fun getItemCount(): Int = indexList.size
    }

    inner class IndexViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {

        private lateinit var item: Pair<Char, Int>
        private var indexTextView: TextView = itemView.findViewById(R.id.index_label)

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(item: Pair<Char, Int>) {
            this.item = item
            indexTextView.text = item.first.toString()
        }

        override fun onClick(v: View?) {
            (itemListRecyclerView.layoutManager as LinearLayoutManager)
                .scrollToPositionWithOffset(item.second, 0)
        }
    }
}