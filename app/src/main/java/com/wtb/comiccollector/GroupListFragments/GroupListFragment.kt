package com.wtb.comiccollector.GroupListFragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.wtb.comiccollector.*
import com.wtb.comiccollector.GroupListViewModels.GroupListViewModel
import com.wtb.comiccollector.GroupListViewModels.SeriesListViewModel
import java.time.LocalDate

private const val TAG = APP + "GroupListFragment"

const val ARG_FILTER_ID = "Series Filter"
const val ARG_CREATOR_FILTER = "Creator Filter"
const val ARG_DATE_FILTER_START = "Date Filter Start"
const val ARG_DATE_FILTER_END = "Date Filter End"

abstract class GroupListFragment<T : DataModel, U : GroupListFragment<T, U, V>.MyAdapter<T>, V :
DataModel> :
    Fragment(), Chippy.ChipCallbacks {

    interface Callbacks {
        fun onSeriesSelected(seriesId: Int)
        fun onCreatorSelected(creatorId: Int)
        fun onNewIssue(issueId: Int)
    }

    abstract val viewModel: GroupListViewModel<T, V>

    protected var callbacks: Callbacks? = null
    protected lateinit var itemList: List<T>

    private var filterId: Int? = null
    private var dateFilterStart: LocalDate? = null
    private var dateFilterEnd: LocalDate? = null

    private lateinit var searchChips: ChipGroup
    private lateinit var search: AutoCompleteTextView
    private lateinit var itemListRecyclerView: RecyclerView

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
        val view = inflater.inflate(R.layout.fragment_item_list, container, false)

        itemList = emptyList()

        searchChips = view.findViewById(R.id.search_chipgroup) as ChipGroup
        search = view.findViewById(R.id.search_tv) as AutoCompleteTextView
        itemListRecyclerView = view.findViewById(R.id.issue_recycler_view) as RecyclerView
        itemListRecyclerView.layoutManager = LinearLayoutManager(context)
        itemListRecyclerView.adapter = getAdapter()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.filter(filterId)

        viewModel.objectListLiveData.observe(
            viewLifecycleOwner,
            { objectList: List<T>? ->
                objectList?.let { it: List<T> ->
                    this.itemList = it
                    updateUI()
                }
            }
        )

        viewModel.filterListLiveData.observe(
            viewLifecycleOwner,
            { filterObjects ->
                filterObjects?.let {
                    search.setAdapter(
                        ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_dropdown_item_1line,
                            it
                        )
                    )
                }
            }
        )
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart is happening")

        search.onItemClickListener =
            object : AdapterView.OnItemClickListener {
                override fun onItemClick(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val item = parent?.adapter?.getItem(position) as DataModel
                    viewModel.filterUpdate(item.id())
                    val chip = Chippy(context, item, this@GroupListFragment)
                    chip.text = item.toString()
                    searchChips.addView(chip)
                    search.setText("")
                    viewModel.filter(filterId = item.id())
                }
            }
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

    override fun chipClosed(id: Int) {
        viewModel.removeFilter(id)
        for (child in searchChips.children) {
            if ((child as Chippy).item.id() == id) {
                searchChips.removeView(child)
            }
        }
    }
}

class Chippy(context: Context?) : Chip(context) {

    lateinit var item: DataModel
    lateinit var caller: ChipCallbacks

    constructor(context: Context?, item: DataModel, caller: ChipCallbacks) : this(context) {
        this.item = item
        this.caller = caller
        Log.d(TAG, "Makin Chippy")
        this.width = WRAP_CONTENT
        this.height = WRAP_CONTENT
        this.closeIcon = context?.let { getDrawable(it, R.drawable.ic_close) }
        this.isCloseIconVisible = true

        this.setOnCloseIconClickListener(
            object : OnClickListener {
                override fun onClick(v: View?) {
                    caller.chipClosed(item.id())
                }
            }
        )
    }


    interface ChipCallbacks {
        fun chipClosed(id: Int)
    }
}