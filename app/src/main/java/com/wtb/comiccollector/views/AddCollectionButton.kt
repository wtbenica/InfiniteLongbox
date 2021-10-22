package com.wtb.comiccollector.views

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import androidx.core.content.res.ResourcesCompat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.R

class AddCollectionButton(context: Context, attributeSet: AttributeSet) :
    androidx.appcompat.widget.AppCompatImageButton(
        context,
        attributeSet,
        R.attr.styleAddCollectionButton
    ) {
    var plusToCheck: AnimatedVectorDrawableCompat? = null
    var checkToPlus: AnimatedVectorDrawableCompat? = null
    var showingPlus: Boolean = false

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

    fun showCheck() {
        if (showingPlus) {
            morph()
        }
    }

    fun showPlus() {
        if (!showingPlus) {
            morph()
        }
    }

    fun morph() {
        val drawable = if (showingPlus) plusToCheck else checkToPlus
        setImageDrawable(drawable)
        drawable?.start()
        showingPlus = !showingPlus
    }

    init {
        showingPlus = true
        plusToCheck = AnimatedVectorDrawableCompat.create(context, R.drawable.added_to_collection_anim)
        checkToPlus = AnimatedVectorDrawableCompat.create(context, R.drawable.removed_from_collection_anim)
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