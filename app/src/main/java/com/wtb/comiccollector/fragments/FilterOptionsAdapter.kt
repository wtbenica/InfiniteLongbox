package com.wtb.comiccollector.fragments

import android.content.Context
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.TextView
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.R
import com.wtb.comiccollector.database.models.FilterModel
import com.wtb.comiccollector.database.models.FullSeries
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class FilterOptionsAdapter(context: Context, filterOptions: List<FilterModel>) :
    ArrayAdapter<FilterModel>(context, LAYOUT, filterOptions) {

    companion object {
        private const val LAYOUT = R.layout.list_item_filter_option_auto_complete
        private const val TAG = APP + "FilterOptionsAdapter"
    }

    private var allOptions: List<FilterModel> = filterOptions
    private var mOptions: List<FilterModel> = filterOptions

    override fun getCount(): Int = mOptions.size

    override fun getItem(position: Int): FilterModel = mOptions[position]

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

        val view =
            convertView ?: View.inflate(
                context,
                R.layout.list_item_filter_option_auto_complete,
                null
            )

        val itemText: TextView = view.findViewById(R.id.item_text)
        val optionTypeText: TextView = view.findViewById(R.id.filter_option_type_text)
        val itemFormatText: TextView = view.findViewById(R.id.format_text)

        val filter: FilterModel = getItem(position)

        itemText.text = filter.toString()
        optionTypeText.text = filter.tagName
        optionTypeText.setTextColor(filter.textColor)
        if (filter is FullSeries) {
            itemFormatText.visibility = VISIBLE
            itemFormatText.text = filter.series.publishingFormat
        } else {
            itemFormatText.visibility = GONE
        }
        return view
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val query = constraint?.toString()?.lowercase()

                val results = FilterResults()
                results.values = if (query == null || query.isEmpty()) {
                    allOptions
                } else {
                    allOptions.filter {
                        it.compareValue.lowercase().contains(query)
                    }
                }

                return results
            }

            override fun publishResults(
                constraint: CharSequence?,
                results: FilterResults?
            ) {
                val optionsList: MutableList<FilterModel> = mutableListOf()

                for (item in (results?.values as List<*>)) {
                    if (item is FilterModel) {
                        optionsList.add(item)
                    }
                }

                mOptions = optionsList
                notifyDataSetChanged()
            }
        }
    }
}