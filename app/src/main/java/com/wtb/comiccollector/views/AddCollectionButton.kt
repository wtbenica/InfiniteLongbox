package com.wtb.comiccollector.views

import android.content.Context
import android.util.AttributeSet
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.R

class AddCollectionButton(context: Context, attributeSet: AttributeSet) :
    androidx.appcompat.widget.AppCompatImageButton(
        context,
        attributeSet,
        R.attr.styleAddCollectionButton
    ) {
    private var plusToCheck: AnimatedVectorDrawableCompat? = null
    private var checkToPlus: AnimatedVectorDrawableCompat? = null
    private var showingPlus: Boolean = false

    var callback: AddCollectionCallback? = null
    var inCollection = false
        set(value) {
            field = value

            if (field) {
                showCheck()
            } else {
                showPlus()
            }

            this.contentDescription = if (field) {
                "Remove from my collection"
            } else {
                "Add to my collection"
            }
        }

    private fun showCheck() {
        if (showingPlus) {
            morph()
        }
    }

    private fun showPlus() {
        if (!showingPlus) {
            morph()
        }
    }

    private fun morph() {
        val drawable = if (showingPlus) plusToCheck else checkToPlus
        setImageDrawable(drawable)
        drawable?.start()
        showingPlus = !showingPlus
    }

    init {
        showingPlus = true
        plusToCheck =
            AnimatedVectorDrawableCompat.create(context, R.drawable.issue_added_to_collection)
        checkToPlus =
            AnimatedVectorDrawableCompat.create(context, R.drawable.removed_from_collection_anim)
        setImageDrawable(plusToCheck)

        setOnClickListener {
            morph()
            if (inCollection) {
                callback?.removeFromCollection()
            } else {
                callback?.addToCollection()
            }
        }
    }

    interface AddCollectionCallback {
        fun addToCollection()
        fun removeFromCollection()
    }


    companion object {
        const val TAG = APP + "AddCollectionButton"
    }
}