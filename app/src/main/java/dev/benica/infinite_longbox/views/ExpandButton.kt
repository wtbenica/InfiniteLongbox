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

package dev.benica.infinite_longbox.views

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.animation.DecelerateInterpolator
import dev.benica.infinite_longbox.APP

class ExpandButton(context: Context, attributeSet: AttributeSet) :
    androidx.appcompat.widget.AppCompatImageButton(context, attributeSet) {

    private var isExpanded = false

    fun initExpanded(isExpanded: Boolean) {
        this.isExpanded = isExpanded
        rotation = if (isExpanded) {
            180f
        } else {
            0f
        }
    }

    fun toggleExpand() {
        val currRotation = rotation
        val destRotation = if (isExpanded) 0f else 180f
        val rotateAnimation =
            ObjectAnimator.ofFloat(this, "rotation", currRotation, destRotation)
                .apply {
                    interpolator = DecelerateInterpolator()
                }

        AnimatorSet().apply {
            play(rotateAnimation)
            start()
        }

        isExpanded = !isExpanded
        Log.d(TAG, "The box is expanded: $isExpanded")
    }

    companion object {
        const val TAG = APP + "ImageButton"
    }
}