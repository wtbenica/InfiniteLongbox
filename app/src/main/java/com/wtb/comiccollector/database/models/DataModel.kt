package com.wtb.comiccollector.database.models

import com.wtb.comiccollector.ComicCollectorApplication.Companion.context
import com.wtb.comiccollector.R
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.io.Serializable
import java.time.LocalDate

sealed class DataModel(var lastUpdated: LocalDate = LocalDate.now()) : Serializable {

    abstract val id: Int
}

/**
 * Filter type
 * A static class/companion object that can be used to limit search autocomplete dropdown
 * results: FilterModels + All
 */
sealed interface FilterType {
    val displayName: String
}

/**
 * Filter models (series, character, creator, publisher, namedetail) + Text Filter: items that
 * can appear in the search autocomplete box.
 */
sealed interface FilterItem


/*
TODO: This should include SERIES, PUBLISHER, CHARACTER, CREATOR. The issue is with CREATOR:
 whether to use CREATOR or NAME_DETAIL or both. Should look for "name_string" in NAME_DETAIL,
 then getting results by CREATOR. This is a big TODO that could become very complicated very quickly
*/
/**
 * A model that can be used in a filter (fullseries, character, creator, publisher, namedetail) and
 * can show up in the search autocomplete dropdown list
 */
@ExperimentalCoroutinesApi
sealed interface FilterModel : FilterItem, Comparable<FilterModel>, Serializable {

    val tagName: String
    val compareValue: String

    val textColor: Int
        get() = when (this) {
            is FullSeries    -> context?.getColor(R.color.text_series)
            is Creator,
            is NameDetail,
                         -> context?.getColor(R.color.text_creator)
            is Publisher -> context?.getColor(R.color.text_publisher)
            is Character -> context?.getColor(R.color.text_character)
            else         -> throw IllegalStateException("Invalid type: $this")
        } ?: 0xFF000000.toInt()

    override fun compareTo(other: FilterModel): Int =
        this.compareValue.compareTo(other.compareValue)

    companion object {
        private const val TAG = "FilterOptionAutoCompletePopupItem"
    }
}

/**
 * A model type that appears as a list item
 */
sealed interface ListItem

@ExperimentalCoroutinesApi
class All {
    companion object : FilterType {
        override val displayName: String = context!!.getString(R.string.filter_type_all)

        override fun toString(): String = displayName
    }
}


@ExperimentalCoroutinesApi
data class TextFilter(
    val text: String,
) : FilterItem {
    override fun toString(): String = "\"$text\""
}

@ExperimentalCoroutinesApi
data class DateFilter(
    val date: LocalDate,
    val isStart: Boolean
) : FilterItem {
    override fun toString(): String {
        return when (date) {
            LocalDate.MIN -> "Start Date"
            LocalDate.MAX -> "End Date"
            else -> date.toString()
        }
    }
}