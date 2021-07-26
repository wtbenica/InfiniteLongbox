package com.wtb.comiccollector.database.models

import com.wtb.comiccollector.ComicCollectorApplication.Companion.context
import com.wtb.comiccollector.R
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.io.Serializable
import java.time.LocalDate

sealed class DataModel(var lastUpdated: LocalDate = LocalDate.now()) : Serializable {

    abstract val id: Int
}

// Include: Series, Creator, Character, Publisher
sealed interface FilterTypeSpinnerOption {
    val displayName: String
}

sealed interface FilterType

/*
TODO: This should include SERIES, PUBLISHER, CHARACTER, CREATOR. The issue is with CREATOR:
 whether to use CREATOR or NAME_DETAIL or both. Should look for "name_string" in NAME_DETAIL,
 then getting results by CREATOR. This is a big TODO that could become very complicated very quickly
*/
@ExperimentalCoroutinesApi
sealed interface FilterAutoCompleteType : FilterType, Comparable<FilterAutoCompleteType>,
    Serializable {
    val tagName: String

    val compareValue: String

    val textColor: Int
        get() = when (this) {
            is Series     -> context?.getColor(R.color.tag_series)
            is Creator,
            is NameDetail -> context?.getColor(R.color.tag_creator)
            is Publisher  -> context?.getColor(R.color.tag_publisher)
            is Character  -> context?.getColor(R.color.tag_character)
            else          -> throw IllegalStateException("Invalid type: $this")
        } ?: 0xFF000000.toInt()

    override fun compareTo(other: FilterAutoCompleteType): Int =
        this.compareValue.compareTo(other.compareValue)

    companion object {
        private const val TAG = "FilterOptionAutoCompletePopupItem"
    }
}

sealed interface ListItem

@ExperimentalCoroutinesApi
class All {
    companion object : FilterTypeSpinnerOption {
        override val displayName: String = context!!.getString(R.string.filter_type_all)

        override fun toString(): String = displayName
    }
}

@ExperimentalCoroutinesApi
data class TextFilter(val text: String) : FilterType