package com.wtb.comiccollector

import android.content.Context
import android.util.AttributeSet
import android.widget.ArrayAdapter
import androidx.appcompat.widget.AppCompatSpinner
import com.wtb.comiccollector.database.models.Series

class SortSpinner(context: Context, attributeSet: AttributeSet?) :
    AppCompatSpinner(context, attributeSet) {
    init {
        this.adapter = ArrayAdapter(
            context,
            androidx.appcompat.R.layout.support_simple_spinner_dropdown_item,
            SortOption.values()
        )
    }
}

enum class SortOption(val tag: String, val compare: (a: Series, b: Series) -> Int) {
    ALPHA("A-Z", { a: Series, b: Series ->
        a.seriesName.compareTo(b.seriesName)
    }),
    ALPHA_REVERSE("Z-A", { a: Series, b: Series ->
        -a.seriesName.compareTo(b.seriesName)
    }),
    DATE("Date - Earliest", { a: Series, b: Series ->
        when {
            a.startDate != null && b.startDate != null -> a.startDate!!.compareTo(b.startDate)
            a.startDate != null -> 1
            b.startDate != null -> -1
            else -> 0
        }
    }),
    DATE_REVERS("Date - Most Recent", { a: Series, b: Series ->
        when {
            a.startDate != null && b.startDate != null -> -a.startDate!!.compareTo(b.startDate)
            a.startDate != null -> -1
            b.startDate != null -> 1
            else -> 0
        }
    });

    override fun toString() = tag
}