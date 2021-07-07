package com.wtb.comiccollector.fragments

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.TextView
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.R
import com.wtb.comiccollector.database.models.FilterOptionAutoCompletePopupItem
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class FilterOptionsAdapter(ctx: Context, filterOptions: List<FilterOptionAutoCompletePopupItem>) :
    ArrayAdapter<FilterOptionAutoCompletePopupItem>(ctx, LAYOUT, filterOptions) {

    companion object {
        private const val LAYOUT = R.layout.list_item_filter_option_auto_complete
        private const val TAG = APP + "FilterOptionsAdapter"
    }

    private var allOptions: List<FilterOptionAutoCompletePopupItem> = filterOptions
    private var mOptions: List<FilterOptionAutoCompletePopupItem> = filterOptions

    override fun getCount(): Int = mOptions.size

    override fun getItem(position: Int): FilterOptionAutoCompletePopupItem = mOptions[position]

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

        val view =
            convertView ?: View.inflate(context, R.layout.list_item_filter_option_auto_complete, null)

        val itemText: TextView = view.findViewById(R.id.item_text)
        val optionTypeText: TextView = view.findViewById(R.id.filter_option_type_text)
        val itemFormatText: TextView = view.findViewById(R.id.format_text)

        val filterOption: FilterOptionAutoCompletePopupItem = getItem(position)

        itemText.text = filterOption.toString()

        optionTypeText.text = filterOption.tagName

        optionTypeText.setTextColor(filterOption.textColor)

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
                val optionsList: MutableList<FilterOptionAutoCompletePopupItem> = mutableListOf()

                for (item in (results?.values as List<*>)) {
                    if (item is FilterOptionAutoCompletePopupItem) {
                        optionsList.add(item)
                    }
                }

                mOptions = optionsList
                notifyDataSetChanged()
            }
        }
    }
}