package com.wtb.comiccollector.views

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.TextFilter
import com.wtb.comiccollector.database.models.FilterOption

private const val TAG = APP + "SearchAutoCompleteTextView"

class SearchAutoComplete(context: Context, attributeSet: AttributeSet) :
    androidx.appcompat.widget.AppCompatAutoCompleteTextView(context, attributeSet) {

    var callbacks: SearchTextViewCallback? = null
    private var item: FilterOption? = null

    interface SearchTextViewCallback {
        fun addFilterItem(option: FilterOption)
        fun hideKeyboard()
    }

    init {
        isSingleLine = true

        setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val tempItem = item
                if (tempItem != null) {
                    callbacks?.addFilterItem(tempItem)
                } else {
                    callbacks?.addFilterItem(TextFilter(v.text.toString()))
                }
                text.clear()
                true
            } else {
                false
            }
        }

        onItemClickListener = AdapterView.OnItemClickListener { parent, v, position, id ->
            Log.d(TAG, "filterTextView item clicked")
            val item = parent?.adapter?.getItem(position) as FilterOption?
            this.item = item
            this.text.clear()
            callbacks?.hideKeyboard()
            item?.let { callbacks?.addFilterItem(it) }
        }

        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Do Nothing
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.toString() != item?.compareValue) {
                    item = null
                }
            }

            override fun afterTextChanged(s: Editable?) {
                // Do Nothing
            }
        })
    }
}