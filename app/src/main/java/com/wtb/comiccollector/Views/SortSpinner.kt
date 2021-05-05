package com.wtb.comiccollector.Views

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatSpinner
import com.wtb.comiccollector.APP

private const val TAG = APP + "SortSpinner"

class SortSpinner(context: Context, attributeSet: AttributeSet?) :
    AppCompatSpinner(context, attributeSet)

abstract class SortOption(
    val tag: String,
    val sortColumn: String
) {

    override fun toString() = tag
}

class IssueSortOption(
    tag: String, sortColumn: String) : SortOption(tag, sortColumn)

val issueSortOptions: List<SortOption> = listOf(
    IssueSortOption("Issue Number (Low to High)", "issueNum ASC"),
    IssueSortOption("Issue Number (High to Low)", "issueNum DESC"),
    IssueSortOption("Date (Oldest to Newest)", "releaseDate ASC"),
    IssueSortOption("Date (Newest to Oldest)", "releaseDate DESC")
)

class SeriesSortOption(
    tag: String, sortColumn: String) : SortOption(tag, sortColumn)

val seriesSortOptions: List<SortOption> = listOf(
    SeriesSortOption("Series Name (A-Z)", "seriesName ASC"),
    SeriesSortOption("Series Name (Z-A)", "seriesName DESC"),
    SeriesSortOption("Date (Oldest to Newest)", "startDate ASC"),
    SeriesSortOption("Date (Newest to Oldest)", "startDate DESC")
)