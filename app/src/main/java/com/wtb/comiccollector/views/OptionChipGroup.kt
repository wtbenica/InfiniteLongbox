package com.wtb.comiccollector.views

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import androidx.core.view.children
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.SearchFilter
import com.wtb.comiccollector.fragments_view_models.FilterViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class OptionChipGroup(context: Context?, attributeSet: AttributeSet) :
    ChipGroup(context, attributeSet), OptionChip.OptionChipCallback {

    interface OptionChipGroupCallback {
        fun checkChanged(action: (FilterViewModel, Boolean) -> Unit, isChecked: Boolean)
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        children.forEach {
            it.isEnabled = enabled
        }
    }

    var callback: OptionChipGroupCallback? = null

    init {
        isSingleLine = true

        val myCollectionChip = OptionChip(
            context,
            "My Collection",
            this,
            FilterViewModel::myCollection
        )
        val variantChip = OptionChip(
            context,
            "Variants",
            this,
            FilterViewModel::showVariants
        )
        addView(myCollectionChip)
        addView(variantChip)
    }

    fun update(filter: SearchFilter) {
        removeAllViews()
        if (filter.mMyCollection) {
            addView(OptionChip(context))
        }
    }

    override fun checkChanged(action: (FilterViewModel, Boolean) -> Unit, isChecked: Boolean) {
        Log.d("${APP}OptionChipGroup", "checkChanged: $isChecked")
        callback?.checkChanged(action, isChecked)
    }
}

@ExperimentalCoroutinesApi
class OptionChip(context: Context?) : Chip(context) {
    private var caller: OptionChipCallback? = null
    internal var action: ((FilterViewModel, Boolean) -> Unit)? = null

    constructor(
        context: Context?,
        text: String,
        caller: OptionChipCallback,
        action: (FilterViewModel, Boolean) -> Unit
    ) : this(context) {
        this.caller = caller
        this.text = text
        this.action = action
    }

    val TAG = APP + "OptionChip"

    init {
        isCloseIconVisible = false
        isCheckedIconVisible = true

        this.setOnClickListener {
            Log.d(TAG, "onClick ${this.isChecked} ${this.isEnabled}")
            action?.let {
                action?.let { a -> caller?.checkChanged(a, this.isChecked) }
            }
        }
    }

    interface OptionChipCallback {
        fun checkChanged(action: (FilterViewModel, Boolean) -> Unit, isChecked: Boolean)
    }
}