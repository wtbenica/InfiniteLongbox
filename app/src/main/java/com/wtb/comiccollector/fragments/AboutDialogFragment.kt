package com.wtb.comiccollector.fragments

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.wtb.comiccollector.R

class AboutDialogFragment : DialogFragment(R.layout.dialog_fragment_about) {
    private var credit: TextView? = null

    override fun onResume() {
        super.onResume()
        updateLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
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
            this@AboutDialogFragment.dismiss()
        }

        credit = view?.findViewById<TextView>(R.id.cc_credit)?.apply {
            movementMethod = LinkMovementMethod.getInstance()
        }

        return view
    }


}