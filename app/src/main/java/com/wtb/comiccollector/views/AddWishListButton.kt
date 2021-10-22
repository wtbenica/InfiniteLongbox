package com.wtb.comiccollector.views

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.widget.Button
import androidx.core.content.res.ResourcesCompat
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.R

class AddWishListButton(context: Context, attributeSet: AttributeSet) :
    androidx.appcompat.widget.AppCompatImageButton(
        context,
        attributeSet,
        R.attr.styleAddCollectionButton
    ) {

    var callback: AddCollectionCallback? = null
    private var inCollection = false
        set(value) {
            field = value
            val draw = ResourcesCompat.getDrawable(
                resources,
                if (field) {
                    R.drawable.remove_wish_list
                } else {
                    R.drawable.add_wish_list
                },
                null
            )
            this.setImageDrawable(draw)

            this.contentDescription = if (field) {
                "Remove from my wish list"
            } else {
                "Add to my wish list"
            }
        }

    init {
        setOnClickListener {
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