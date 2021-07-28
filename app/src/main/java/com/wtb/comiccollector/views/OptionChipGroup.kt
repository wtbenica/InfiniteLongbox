package com.wtb.comiccollector.views

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import com.google.android.material.chip.Chip
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.SearchFilter
import com.wtb.comiccollector.database.models.*
import com.wtb.comiccollector.fragments_view_models.FilterViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.reflect.KClass

@ExperimentalCoroutinesApi
class OptionChipGroup(context: Context?, attributeSet: AttributeSet) :
    FilterCardChipGroup(context, attributeSet), OptionChip.OptionChipCallback,
    ViewChip.ViewChipCallback {

    var callback: OptionChipGroupCallback? = null

    private val myCollectionChip: OptionChip = OptionChip(
        context,
        "My Collection",
        this
    ) { fmv, bool ->
        fmv.myCollection((bool))
    }

    fun update(filter: SearchFilter) {
        removeAllViews()
        addView(myCollectionChip)
        myCollectionChip.isChecked = filter.mMyCollection

        val viewChip = ViewChip(context, this, filter.viewOptions.second, filter.viewOptions.first)
        addView(viewChip)

        if (filter.viewOption == FullIssue::class && !filter.mMyCollection) {
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
    }

    override fun checkChanged(action: (FilterViewModel, Boolean) -> Unit, isChecked: Boolean) {
        callback?.checkChanged(action, isChecked)
    }

    override fun onClickViewChip() {
        callback?.onClickViewChip()
    }

    interface OptionChipGroupCallback {
        fun checkChanged(action: (FilterViewModel, Boolean) -> Unit, isChecked: Boolean)
        fun onClickViewChip()
    }
}

@ExperimentalCoroutinesApi
class ViewChip @JvmOverloads constructor(
    context: Context?,
    private val caller: ViewChipCallback? = null,
    private var viewTypeIndex: Int = 0,
    viewTypes: List<KClass<out ListItem>>? = null
) : Chip(context) {

    private val mViewTypes = viewTypes ?: listOf(
        FullSeries::class,
        Character::class,
        NameDetailAndCreator::class
    )
    private val viewType: KClass<out ListItem>
        get() = mViewTypes[viewTypeIndex % mViewTypes.size]

    init {
        isCloseIconVisible = false
        isChecked = true
        isCheckable = false
        text = viewType.simpleName
        this.setOnClickListener {
            viewTypeIndex++
            text = viewType.simpleName
            caller?.onClickViewChip()
        }
    }

    @ExperimentalCoroutinesApi
    interface ViewChipCallback {
        fun onClickViewChip()
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