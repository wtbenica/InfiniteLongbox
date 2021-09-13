package com.wtb.comiccollector.views

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.database.models.FilterItem
import com.wtb.comiccollector.database.models.FilterModel
import com.wtb.comiccollector.database.models.TextFilter
import kotlinx.coroutines.ExperimentalCoroutinesApi

private const val TAG = APP + "SearchAutoCompleteTextView"

@ExperimentalCoroutinesApi
class SearchAutoComplete(context: Context, attributeSet: AttributeSet) :
    MaterialAutoCompleteTextView(context, attributeSet) {

    var callbacks: SearchTextViewCallback? = null
    private var item: FilterModel? = null

    interface SearchTextViewCallback {
        fun addFilterItem(option: FilterItem)
        fun hideKeyboard()
    }

    init {
        isSingleLine = true
        imeOptions = EditorInfo.IME_ACTION_DONE

        setOnEditorActionListener { v, actionId, event ->
            Log.d(TAG, "ACTION ID: $actionId")
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val tempItem = item
                if (tempItem != null) {
                    Log.d(TAG, "ADDING: $tempItem")
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
            val item = parent?.adapter?.getItem(position) as FilterModel?
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
                Log.d(TAG, "TEXT CHANGED!")
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