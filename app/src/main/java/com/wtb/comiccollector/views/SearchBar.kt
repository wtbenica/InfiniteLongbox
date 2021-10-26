package com.wtb.comiccollector.views

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo.IME_ACTION_DONE
import android.widget.AdapterView
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.R
import com.wtb.comiccollector.database.models.FilterItem
import com.wtb.comiccollector.database.models.FilterModel
import com.wtb.comiccollector.database.models.FilterType
import com.wtb.comiccollector.database.models.TextFilter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.reflect.KClass

private const val TAG = APP + "SearchAutoCompleteTextView"

@ExperimentalCoroutinesApi
class SearchBar(context: Context, attributeSet: AttributeSet) :
    MaterialAutoCompleteTextView(context, attributeSet, R.style.SearchBarStyle) {

    var callbacks: SearchTextViewCallback? = null
    private var item: FilterModel? = null

    init {
        imeOptions = IME_ACTION_DONE
        isSingleLine = true
        // show dropdown and show filter type chipgroup
        setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                this.showDropDown()
                callbacks?.setFilterTypesVisibility(true)
            } else {
                callbacks?.setFilterTypesVisibility(false)
            }
        }

        // on enter
        setOnEditorActionListener { v, actionId, event ->
            Log.d(TAG, "ACTION ID: $actionId")
            if (actionId == IME_ACTION_DONE) {
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

        setOnTouchListener(object : OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                if (event?.action == MotionEvent.ACTION_DOWN) {
                    showDropDown()
                    v?.performClick()
                    return false
                }
                return false
            }

        })

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

    interface SearchTextViewCallback {
        fun addFilterItem(option: FilterItem)
        fun hideKeyboard()
        fun setFilterTypesVisibility(isVisible: Boolean)
    }
}

@ExperimentalCoroutinesApi
class FilterTypeChip<T : FilterType> @JvmOverloads constructor(
    context: Context?,
    attributeSet:
    AttributeSet? = null,
) : Chip(context, attributeSet) {
    var type: KClass<T>? = null

    constructor(context: Context, type: KClass<T>, bgColor: Int, strokeColor: Int) : this(context) {
        this.type = type
        setChipBackgroundColorResource(bgColor)
        setChipStrokeColorResource(strokeColor)
    }
}