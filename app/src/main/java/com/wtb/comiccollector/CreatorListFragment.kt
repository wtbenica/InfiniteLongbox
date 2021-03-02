package com.wtb.comiccollector

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDate
import java.util.*

private const val TAG = "CreatorListFragment"

class CreatorListFragment : GroupListFragment<Creator>() {

    override lateinit var recyclerView: RecyclerView

    override var filterId: UUID? = null
    override var dateFilterStart: LocalDate? = null
    override var dateFilterEnd: LocalDate? = null

    override var callbacks: Callbacks? = null

    override val viewModel by lazy {
        ViewModelProvider(this).get(CreatorListViewModel::class.java)
    }

    override lateinit var itemList: List<Creator>

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as Callbacks?
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_issue_list, container, false)

        itemList = emptyList()

        recyclerView = view.findViewById(R.id.issue_recycler_view) as RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = CreatorAdapter(itemList)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        filterId?.let { viewModel.filterByCreator(it) }

        viewModel.creatorListLiveData.observe(
            viewLifecycleOwner,
            { creatorList ->
                creatorList?.let {
                    this.itemList = it
                    updateUI()
                }
            }
        )
    }

    private fun updateUI() {
        recyclerView.adapter = CreatorAdapter(itemList)
        runLayoutAnimation(recyclerView)
    }

    private inner class CreatorAdapter(creatorlist: List<Creator>) :
        GroupListFragment.MyAdapter<Creator>(itemList = creatorlist) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CreatorHolder {
            val view = layoutInflater.inflate(R.layout.list_item_series, parent, false)
            return CreatorHolder(view)
        }
    }

    private inner class CreatorHolder(view: View) : GroupListFragment.MyHolder<Creator>(view) {
        override lateinit var item: Creator

        private val coverImageView: ImageView = itemView.findViewById(R.id.list_item_cover)
        private val nameTextView: TextView = itemView.findViewById(R.id.list_item_name)

        override fun onClick(v: View?) {
            callbacks?.onCreatorSelected(item.creatorId)
        }

        override fun bind(item: Creator) {
            this.item = item
            nameTextView.text = this.item.sortName
        }

    }

    enum class SeriesFilter(val s: String, val onSelect: (viewModel: SeriesListViewModel) -> Unit) {
        NONE("None", { }),
        CREATOR("Creator", { }),
        DATE("Date Range", { });

        override fun toString(): String {
            return s
        }
    }

    enum class CreatorFilter(val s: String, val onSelect: () -> Unit) {
        NONE("None", { }),
        COCREATOR("Cocreator", { }),
        DATE("Date Range", { });

        override fun toString(): String {
            return s
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(
            seriesFilterId: UUID? = null,
            creatorFilterId: UUID? = null,
            dateFilterStart: LocalDate? = null,
            dateFilterEnd: LocalDate? = null
        ) =
            SeriesListFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_FILTER_ID, seriesFilterId)
                    putSerializable(ARG_CREATOR_FILTER, creatorFilterId)
                    putSerializable(ARG_DATE_FILTER_START, dateFilterStart)
                    putSerializable(ARG_DATE_FILTER_END, dateFilterEnd)
                }
            }
    }
}