package com.wtb.comiccollector

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatSpinner

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
    SeriesSortOption("A-Z", "seriesName ASC"),
    SeriesSortOption("Z-A", "seriesName DESC"),
    SeriesSortOption("Date - Earliest", "startDate ASC"),
    SeriesSortOption("Date - Most Recent", "startDate DESC")
)