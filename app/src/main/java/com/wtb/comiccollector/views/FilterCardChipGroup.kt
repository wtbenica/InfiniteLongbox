package com.wtb.comiccollector.views

import android.content.Context
import android.util.AttributeSet
import androidx.core.view.children
import com.google.android.material.chip.ChipGroup
import com.wtb.comiccollector.R

abstract class FilterCardChipGroup(context: Context?, attributeSet: AttributeSet) :
    ChipGroup(context, attributeSet, R.style.filterCardChipGroup) {

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        children.forEach {
            it.isEnabled = enabled
        }
    }
}