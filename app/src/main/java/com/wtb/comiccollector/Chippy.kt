package com.wtb.comiccollector

import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import com.google.android.material.chip.Chip

private const val TAG = APP + "Chippy"

class Chippy(context: Context?) : Chip(context) {

    lateinit var item: Filterable
    lateinit var caller: ChipCallbacks

    constructor(context: Context?, item: Filterable, caller: ChipCallbacks) : this(context) {
        this.item = item
        this.caller = caller
        Log.d(TAG, "Makin Chippy")
        this.width = ViewGroup.LayoutParams.WRAP_CONTENT
        this.height = ViewGroup.LayoutParams.WRAP_CONTENT
        this.closeIcon = context?.let { AppCompatResources.getDrawable(it, R.drawable.ic_close) }
        this.isCloseIconVisible = true

        this.setOnCloseIconClickListener(
            object : OnClickListener {
                override fun onClick(v: View?) {
                    caller.chipClosed(this@Chippy, item)
                }
            }
        )
    }


    interface ChipCallbacks {
        fun chipClosed(view: View, item: Filterable)
    }
}