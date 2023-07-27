/*
 * Copyright (c) 2023. Wesley T. Benica
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.benica.infinite_longbox.fragments

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import dev.benica.infinite_longbox.R

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