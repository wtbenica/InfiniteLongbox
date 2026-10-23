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

import android.content.Context
import android.text.InputType
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.LifecycleCoroutineScope
import dev.benica.infinite_longbox.R
import dev.benica.infinite_longbox.repository.Repository
import kotlinx.coroutines.launch

/** Asks for a storage place, suggesting the ones already used so the same box isn't spelled two ways. */
object StoragePrompt {
    /**
     * @param current the place to start from
     * @param message what the answer is for
     * @param clearLabel the button that answers "no place"; null for none
     * @param onChosen gets the place typed (never blank), or null for the clear button
     */
    fun show(
        context: Context,
        scope: LifecycleCoroutineScope,
        title: Int,
        message: String?,
        current: String?,
        clearLabel: Int?,
        onChosen: (String?) -> Unit,
    ) {
        val input = AutoCompleteTextView(context).apply {
            setHint(R.string.my_copy_storage)
            setText(current.orEmpty())
            setSelection(text.length)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            setSingleLine()
            threshold = 0
        }
        scope.launch {
            val places = Repository.get().storageLocations()
            input.setAdapter(ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, places))
        }
        val pad = (16 * context.resources.displayMetrics.density).toInt()
        AlertDialog.Builder(context)
            .setTitle(title)
            .apply { message?.let { setMessage(it) } }
            .setView(FrameLayout(context).apply {
                setPadding(pad, 0, pad, 0)
                addView(input)
            })
            .setPositiveButton(android.R.string.ok) { _, _ ->
                input.text.toString().trim().takeIf { it.isNotEmpty() }?.let(onChosen)
            }
            .apply { clearLabel?.let { setNeutralButton(it) { _, _ -> onChosen(null) } } }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
