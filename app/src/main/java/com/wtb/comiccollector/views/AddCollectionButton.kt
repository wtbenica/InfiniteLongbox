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

package com.wtb.comiccollector.views

import android.content.Context
import android.util.AttributeSet
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.R
import com.wtb.comiccollector.database.models.BaseCollection

class AddCollectionButton(context: Context, attributeSet: AttributeSet) :
    androidx.appcompat.widget.AppCompatImageButton(
        context,
        attributeSet,
        R.attr.styleAddCollectionButton
    ) {
    private var plusToCheck: AnimatedVectorDrawableCompat? = null
    private var checkToPlus: AnimatedVectorDrawableCompat? = null
    private var showingPlus: Boolean = false

    var callback: AddCollectionCallback? = null
    var inCollection = false
        set(value) {
            field = value

            if (field) {
                showCheck()
            } else {
                showPlus()
            }

            this.contentDescription = if (field) {
                "Remove from my collection"
            } else {
                "Add to my collection"
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
            AnimatedVectorDrawableCompat.create(context, R.drawable.issue_added_to_collection)
        checkToPlus =
            AnimatedVectorDrawableCompat.create(context, R.drawable.removed_from_collection_anim)
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

    interface AddCollectionCallback {
        fun addToCollection(collId: Int)
        fun removeFromCollection(collId: Int)
    }


    companion object {
        const val TAG = APP + "AddCollectionButton"

        val collId = BaseCollection.MY_COLL.id
    }
}