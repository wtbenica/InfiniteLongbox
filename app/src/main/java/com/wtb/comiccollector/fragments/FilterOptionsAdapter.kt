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
import com.wtb.comiccollector.database.models.FilterAutoCompleteType
import com.wtb.comiccollector.database.models.Series
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class FilterOptionsAdapter(ctx: Context, filters: List<FilterAutoCompleteType>) :
    ArrayAdapter<FilterAutoCompleteType>(ctx, LAYOUT, filters) {

    companion object {
        private const val LAYOUT = R.layout.list_item_filter_option_auto_complete
        private const val TAG = APP + "FilterOptionsAdapter"
    }

    private var alls: List<FilterAutoCompleteType> = filters
    private var mOptions: List<FilterAutoCompleteType> = filters

    override fun getCount(): Int = mOptions.size

    override fun getItem(position: Int): FilterAutoCompleteType = mOptions[position]

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

        val filter: FilterAutoCompleteType = getItem(position)

        itemText.text = filter.toString()
        optionTypeText.text = filter.tagName
        optionTypeText.setTextColor(filter.textColor)
        if (filter is Series) {
            itemFormatText.visibility = VISIBLE
            itemFormatText.text = filter.publishingFormat
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
                    alls
                } else {
                    alls.filter {
                        it.compareValue.lowercase().contains(query)
                    }
                }

                return results
            }

            override fun publishResults(
                constraint: CharSequence?,
                results: FilterResults?
            ) {
                val optionsList: MutableList<FilterAutoCompleteType> = mutableListOf()

                for (item in (results?.values as List<*>)) {
                    if (item is FilterAutoCompleteType) {
                        optionsList.add(item)
                    }
                }

                mOptions = optionsList
                notifyDataSetChanged()
            }
        }
    }
}