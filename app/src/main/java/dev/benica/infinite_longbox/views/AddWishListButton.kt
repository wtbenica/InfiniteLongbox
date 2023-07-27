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

import android.content.Context
import android.util.AttributeSet
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import dev.benica.infinite_longbox.APP
import dev.benica.infinite_longbox.R
import dev.benica.infinite_longbox.database.models.BaseCollection

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