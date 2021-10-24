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
import com.wtb.comiccollector.database.models.Character
import com.wtb.comiccollector.database.models.FilterModel
import com.wtb.comiccollector.database.models.FullSeries
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class FilterOptionsAdapter(context: Context, filterOptions: List<FilterModel>) :
    ArrayAdapter<FilterModel>(context, LAYOUT, filterOptions) {

    companion object {
        private const val LAYOUT = R.layout.auto_complete_filter_item
        private const val TAG = APP + "FilterOptionsAdapter"
    }

    private var allOptions: List<FilterModel> = filterOptions
    private var mOptions: List<FilterModel> = filterOptions

    override fun getCount(): Int = mOptions.size

    override fun getItem(position: Int): FilterModel = mOptions[position]

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view =
            convertView ?: View.inflate(context, R.layout.auto_complete_filter_item, null)

        val itemText: TextView = view.findViewById(R.id.item_text)
        val optionTypeText: TextView = view.findViewById(R.id.filter_option_type_text)
        val itemFormatText: TextView = view.findViewById(R.id.format_text)

        val modelItem: FilterModel = getItem(position)

        itemText.text = modelItem.toString()
        optionTypeText.text = modelItem.tagName
        optionTypeText.setTextColor(modelItem.textColor)
        when (modelItem) {
            is FullSeries -> {
                modelItem.series.publishingFormat.let {
                    if (it?.isNotBlank() == true) {
                        itemFormatText.text = it
                        itemFormatText.visibility = VISIBLE
                    } else {
                        itemFormatText.visibility = GONE
                    }
                }
            }
            is Character -> {
                itemFormatText.visibility = GONE
                // TODO: Once this is switched to FullCharacter, put publisher in itemFormatText.
                //  Maybe add a dedicated textbox for publisher, as it's used in series also
            }
            else -> itemFormatText.visibility = GONE
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
                    allOptions.filter { fm ->
                        val newQs = query.split(' ')
                        newQs.map { fm.compareValue.lowercase().contains(it) }
                            .reduce { acc, b -> acc && b }
                    }
                }

                return results
            }

            override fun publishResults(
                constraint: CharSequence?,
                results: FilterResults?,
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