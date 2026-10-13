/*
 * Copyright (c) 2023. Wesley T. Benica
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.benica.infinite_longbox.fragments

import android.content.Context
import android.util.Log
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.TextView
import androidx.annotation.LayoutRes
import dev.benica.infinite_longbox.APP
import dev.benica.infinite_longbox.R
import dev.benica.infinite_longbox.database.models.Character
import dev.benica.infinite_longbox.database.models.FilterModel
import dev.benica.infinite_longbox.database.models.FullSeries
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * Adapter for the filter-box popup. It is created once; every keystroke (and every change of
 * filter type) re-runs [search] on the Filter's worker thread, so the popup never holds more than
 * a page of results.
 *
 * @param search returns the options matching the text typed so far
 */
@ExperimentalCoroutinesApi
class FilterOptionsAdapter(context: Context, private val search: (String) -> List<FilterModel>) :
    ArrayAdapter<FilterModel>(context, LAYOUT, mutableListOf()) {

    companion object {
        @LayoutRes
        private val LAYOUT = R.layout.auto_complete_filter_item
        private const val TAG = APP + "FilterOptionsAdapter"
    }

    private var mOptions: List<FilterModel> = emptyList()

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
                val results = FilterResults()
                // Runs on the Filter's own worker thread, so a blocking lookup is fine here.
                val options = try {
                    search(constraint?.toString().orEmpty())
                } catch (e: Exception) {
                    Log.e(TAG, "Couldn't load filter options", e)
                    emptyList()
                }
                results.values = options
                results.count = options.size
                return results
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                mOptions = (results?.values as? List<*>)?.filterIsInstance<FilterModel>()
                    ?: emptyList()
                if (mOptions.isEmpty()) notifyDataSetInvalidated() else notifyDataSetChanged()
            }
        }
    }
}
