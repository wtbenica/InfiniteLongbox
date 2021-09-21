package com.wtb.comiccollector.fragments

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import com.wtb.comiccollector.R

class CoverDialogFragment(val draw: Drawable) : DialogFragment(R.layout.dialog_fragment_cover) {

    var coverView: ImageView? = null

    override fun onResume() {
        super.onResume()
        updateLayoutParams(MATCH_PARENT, WRAP_CONTENT)
    }

    private fun updateLayoutParams(w: Int, h: Int) {
        val dw = dialog?.window
        dw?.let { window ->
            val layoutParams = window.attributes
            layoutParams.width = w
            layoutParams.height = h
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        view?.setOnClickListener {
            this@CoverDialogFragment.dismiss()
        }

        coverView = view?.findViewById<ImageView>(R.id.cover_view).apply {
            this?.setImageDrawable(draw)
        }

        return view
    }
}