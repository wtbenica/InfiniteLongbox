package com.wtb.comiccollector.views

import android.content.Context
import android.util.AttributeSet
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.R
import com.wtb.comiccollector.database.models.BaseCollection

class AddWishListButton(context: Context, attributeSet: AttributeSet) :
    androidx.appcompat.widget.AppCompatImageButton(
        context,
        attributeSet,
        R.attr.styleAddCollectionButton
    ) {
    private var plusToCheck: AnimatedVectorDrawableCompat? = null
    private var checkToPlus: AnimatedVectorDrawableCompat? = null
    private var showingPlus: Boolean = false

    var callback: AddCollectionButton.AddCollectionCallback? = null
    var inCollection = false
        set(value) {
            field = value

            if (field) {
                showCheck()
            } else {
                showPlus()
            }

            this.contentDescription = if (field) {
                "Remove from wish list"
            } else {
                "Add to wish list"
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
            AnimatedVectorDrawableCompat.create(context, R.drawable.issue_added_to_wish_list)
        checkToPlus =
            AnimatedVectorDrawableCompat.create(context, R.drawable.removed_from_wish_list_anim)
        setImageDrawable(plusToCheck)

        setOnClickListener {
            morph()
            if (inCollection) {
                callback?.removeFromCollection(collId)
            } else {
                callback?.addToCollection(collId)
            }
        }
    }

    companion object {
        const val TAG = APP + "AddWishListButton"

        val collId = BaseCollection.WISH_LIST.id
    }
}