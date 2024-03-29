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
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import dev.benica.infinite_longbox.R
import dev.benica.infinite_longbox.database.models.FullIssue
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class CoverDialogFragment(private val draw: Drawable, private val currentIssue: FullIssue) :
    DialogFragment(R.layout.dialog_fragment_cover) {

    private var coverView: ImageView? = null
    private var coverTextView: TextView? = null
    private var variantNameTextView: TextView? = null

    override fun onResume() {
        super.onResume()
        updateLayoutParams(MATCH_PARENT, MATCH_PARENT)
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

        coverTextView = view?.findViewById<TextView>(R.id.tv_issue).apply {
            this?.text = currentIssue.toString()
        }

        variantNameTextView = view?.findViewById<TextView>(R.id.tv_variant_name).apply {
            val variantName = currentIssue.issue.variantName
            if (variantName.isBlank()) {
                this?.visibility = View.GONE
            } else {
                this?.visibility = View.VISIBLE
                this?.text = variantName
            }
        }

        return view
    }
}