package com.wtb.comiccollector.views

import android.content.Context
import android.util.AttributeSet
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.R

class AddCollectionButton(context: Context, attributeSet: AttributeSet) :
    androidx.appcompat.widget.AppCompatImageButton(
        context,
        attributeSet,
        R.attr.styleAddCollectionButton
    ) {

    var callback: AddCollectionCallback? = null
    var inCollection = false
        set(value) {
            field = value
            this.setImageResource(
                if (field) {
                    R.drawable.remove_collection
                } else {
                    R.drawable.add_collection
                }
            )
            this.contentDescription = if (field) {
                "Remove from collection"
            } else {
                "Add to collection"
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