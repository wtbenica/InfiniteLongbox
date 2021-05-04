package com.wtb.comiccollector

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import androidx.appcompat.widget.AppCompatSpinner
import com.wtb.comiccollector.database.models.Filterable
import com.wtb.comiccollector.database.models.FullIssue
import com.wtb.comiccollector.database.models.Series

private const val TAG = APP + "SortSpinner"

class SortSpinner(context: Context, attributeSet: AttributeSet?) :
    AppCompatSpinner(context, attributeSet)

abstract class SortOption(
    val tag: String,
    val sortColumn: String,
    pCompare: (Filterable, Filterable) -> Int
) {
    var compare = { a: Filterable, b: Filterable ->
        Log.d(TAG, "comparing")
        pCompare(a, b)
    }

    override fun toString() = tag
}

class IssueSortOption(
    tag: String, sortColumn: String, compare: (a: Filterable, b: Filterable) ->
    Int
) : SortOption(tag, sortColumn, compare)

val issueSortOptions: List<SortOption> = listOf(
    IssueSortOption("Issue Number (Low to High)", "issueNum ASC") { a: Filterable, b: Filterable ->
        a.sortValue().compareTo(b.sortValue())
    },
    IssueSortOption("Issue Number (High to Low)", "issueNum DESC") { a: Filterable, b: Filterable ->
        b.sortValue().compareTo(a.sortValue())
    },
    IssueSortOption("Date (Oldest to Newest)", "releaseDate ASC") { a: Filterable, b: Filterable ->
        if (a is FullIssue && b is FullIssue && a.issue.releaseDate != null && b.issue.releaseDate != null) {
            a.issue.releaseDate!!.compareTo(b.issue.releaseDate)
        } else {
            a.sortValue().compareTo(b.sortValue())
        }
    },
    IssueSortOption("Date (Newest to Oldest)", "releaseDate DESC") { a: Filterable, b: Filterable ->
        if (a is FullIssue && b is FullIssue && a.issue.releaseDate != null && b.issue.releaseDate != null) {
            b.issue.releaseDate!!.compareTo(a.issue.releaseDate)
        } else {
            b.sortValue().compareTo(a.sortValue())
        }
    }
)

class SeriesSortOption(
    tag: String, sortColumn: String, compare: (a: Filterable, b: Filterable) ->
    Int
) : SortOption(tag, sortColumn, compare)

val seriesSortOptions: List<SortOption> = listOf(
    SeriesSortOption("A-Z", "seriesName ASC") { a: Filterable, b: Filterable ->
        a.sortValue().compareTo(b.sortValue())
    },
    SeriesSortOption("Z-A", "seriesName DESC") { a: Filterable, b: Filterable ->
        b.sortValue().compareTo(a.sortValue())
    },
    SeriesSortOption("Date - Earliest", "startDate ASC") { a: Filterable, b: Filterable ->
        if (a is Series && b is Series && a.startDate != null && b.startDate != null) {
            a.startDate!!.compareTo(b.startDate)
        } else {
            a.sortValue().compareTo(b.sortValue())
        }
    },
    SeriesSortOption("Date - Most Recent", "startDate DESC") { a: Filterable, b: Filterable ->
        if (a is Series && b is Series && a.startDate != null && b.startDate != null) {
            b.startDate!!.compareTo(a.startDate)
        } else {
            b.sortValue().compareTo(a.sortValue())
        }
    }
)