package com.wtb.comiccollector.views

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import com.google.android.material.chip.Chip
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.SearchFilter
import com.wtb.comiccollector.fragments_view_models.FilterViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class OptionChipGroup(context: Context?, attributeSet: AttributeSet) :
    FilterCardChipGroup(context, attributeSet), OptionChip.OptionChipCallback {

    var callback: OptionChipGroupCallback? = null

    private val myCollectionChip: OptionChip = OptionChip(
        context,
        "My Collection",
        this,
        { fmv, bool ->
            fmv.myCollection((bool))
        }
    )

    fun update(filter: SearchFilter) {
        removeAllViews()
        addView(myCollectionChip)
        myCollectionChip.isChecked = filter.mMyCollection

        if (filter.returnsIssueList() && !filter.mMyCollection) {
            val variantChip = OptionChip(
                context,
                "Variants",
                this,
                FilterViewModel::showVariants
            ).apply {
                isChecked = filter.mShowVariants
            }

            addView(variantChip)
        }

        if (filter.isNotEmpty() && filter.mSeries == null) {
            val issueChip = OptionChip(
                context,
                "Issues",
                this,
                FilterViewModel::showIssues
            ).apply {
                isChecked = filter.mShowIssues
            }

            addView(issueChip)
        }
    }

    override fun checkChanged(action: (FilterViewModel, Boolean) -> Unit, isChecked: Boolean) {
        callback?.checkChanged(action, isChecked)
    }

    interface OptionChipGroupCallback {
        fun checkChanged(action: (FilterViewModel, Boolean) -> Unit, isChecked: Boolean)
    }
}

@ExperimentalCoroutinesApi
class OptionChip(context: Context?) : Chip(context) {
    private var caller: OptionChipCallback? = null
    private var action: ((FilterViewModel, Boolean) -> Unit)? = null

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

    init {
        isCloseIconVisible = false

        this.setOnClickListener {
            Log.d(TAG, "onClick ${this.isChecked} ${this.isEnabled}")
            action?.let {
                action?.let { a -> caller?.checkChanged(a, this.isChecked) }
            }
        }
    }

    companion object {
        private const val TAG = APP + "OptionChip"
    }

    interface OptionChipCallback {
        fun checkChanged(action: (FilterViewModel, Boolean) -> Unit, isChecked: Boolean)
    }
}