package com.wtb.comiccollector.database.models

import com.wtb.comiccollector.ComicCollectorApplication
import com.wtb.comiccollector.R
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.io.Serializable
import java.time.LocalDate

abstract class DataModel(var lastUpdated: LocalDate = LocalDate.now()) : Serializable {

    abstract val id: Int
}

// Include: Series, Creator, Character, Publisher
sealed interface FilterTypeSpinnerOption {
    val displayName: String
}

/*
TODO: This should include SERIES, PUBLISHER, CHARACTER, CREATOR. The issue is with CREATOR:
 whether to use CREATOR or NAME_DETAIL or both. Should look for "name_string" in NAME_DETAIL,
 then getting results by CREATOR. This is a big TODO that could become very complicated very quickly
*/
@ExperimentalCoroutinesApi
sealed interface FilterOptionAutoCompletePopupItem : Comparable<FilterOptionAutoCompletePopupItem>,
    Serializable {
    val tagName: String

    val compareValue: String

    val textColor: Int
        get() = when (this) {
            is Series     -> ComicCollectorApplication.context?.getColor(R.color.tag_series)
            is Creator,
            is NameDetail -> ComicCollectorApplication.context?.getColor(R.color.tag_creator)
            is Publisher  -> ComicCollectorApplication.context?.getColor(R.color.tag_publisher)
            is Character -> ComicCollectorApplication.context?.getColor(R.color.tag_character)
            is TextFilter -> null
        } ?: 0xFF000000.toInt()

    override fun compareTo(other: FilterOptionAutoCompletePopupItem): Int =
        this.compareValue.compareTo(other.compareValue)

    companion object {
        private const val TAG = "FilterOptionAutoCompletePopupItem"
    }
}

interface ListItem

@ExperimentalCoroutinesApi
data class TextFilter(val text: String) : FilterOptionAutoCompletePopupItem {
    override val tagName: String
        get() = "Text"

    override val compareValue: String
        get() = text

    override fun toString(): String = "\"$text\""
}