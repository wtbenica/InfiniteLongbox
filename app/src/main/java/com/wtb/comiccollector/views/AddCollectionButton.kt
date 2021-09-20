package com.wtb.comiccollector.views

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.R

class AddCollectionButton(context: Context, attributeSet: AttributeSet) :
    androidx.appcompat.widget.AppCompatImageButton(context, attributeSet) {

    var callback: AddCollectionCallback? = null
    var inCollection = false
        set(value) {
            field = value
            this.setImageResource(
                if (field) {
                    Log.d(TAG, "In collection, so setting to remove collection image.")
                    R.drawable.remove_collection
                } else {
                    Log.d(TAG, "Not in collection, so setting to add collection image.")
                    R.drawable.add_collection
                }
            )
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