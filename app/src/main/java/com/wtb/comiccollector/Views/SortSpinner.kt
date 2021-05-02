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
    pCompare: (Filterable, Filterable) -> Int
) {
    var compare = { a: Filterable, b: Filterable ->
        Log.d(TAG, "comparing")
        pCompare(a, b)
    }

    override fun toString() = tag
}

class IssueSortOption(tag: String, compare: (a: Filterable, b: Filterable) -> Int) :
    SortOption(tag, compare)

val issueSortOptions: List<SortOption> = listOf(
    IssueSortOption("0-9") { a: Filterable, b: Filterable ->
        a.sortValue().compareTo(b.sortValue())
    },
    IssueSortOption("9-0") { a: Filterable, b: Filterable ->
        b.sortValue().compareTo(a.sortValue())
    },
    IssueSortOption("Date - Earliest") { a: Filterable, b: Filterable ->
        if (a is FullIssue && b is FullIssue && a.issue.releaseDate != null && b.issue.releaseDate != null) {
            a.issue.releaseDate!!.compareTo(b.issue.releaseDate)
        } else {
            a.sortValue().compareTo(b.sortValue())
        }
    },
    IssueSortOption("Date - Most Recent") { a: Filterable, b: Filterable ->
        if (a is FullIssue && b is FullIssue && a.issue.releaseDate != null && b.issue.releaseDate != null) {
            b.issue.releaseDate!!.compareTo(a.issue.releaseDate)
        } else {
            b.sortValue().compareTo(a.sortValue())
        }
    }
)

class SeriesSortOption(tag: String, compare: (a: Filterable, b: Filterable) -> Int) :
    SortOption(tag, compare)

val seriesSortOptions: List<SortOption> = listOf(
    SeriesSortOption("A-Z") { a: Filterable, b: Filterable ->
        a.sortValue().compareTo(b.sortValue())
    },
    SeriesSortOption("Z-A") { a: Filterable, b: Filterable ->
        b.sortValue().compareTo(a.sortValue())
    },
    SeriesSortOption("Date - Earliest") { a: Filterable, b: Filterable ->
        if (a is Series && b is Series && a.startDate != null && b.startDate != null) {
            a.startDate!!.compareTo(b.startDate)
        } else {
            a.sortValue().compareTo(b.sortValue())
        }
    },
    SeriesSortOption("Date - Most Recent") { a: Filterable, b: Filterable ->
        if (a is Series && b is Series && a.startDate != null && b.startDate != null) {
            b.startDate!!.compareTo(a.startDate)
        } else {
            b.sortValue().compareTo(a.sortValue())
        }
    }
)