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
package dev.benica.infinite_longbox.ui.dialogs

import androidx.activity.ComponentActivity
import dev.benica.infinite_longbox.ui.dialogs.StoragePromptDialog
import dev.benica.infinite_longbox.ui.dialogs.showDialog

/** Asks for a storage place, suggesting the ones already used so the same box isn't spelled two ways. */
object StoragePrompt {
    /**
     * @param current the place to start from
     * @param message what the answer is for
     * @param clearLabel the button that answers "no place"; null for none
     * @param onChosen gets the place typed (never blank), or null for the clear button
     */
    fun show(
        activity: ComponentActivity,
        title: Int,
        message: String?,
        current: String?,
        clearLabel: Int?,
        onChosen: (String?) -> Unit,
    ) = activity.showDialog { dismiss ->
        StoragePromptDialog(title, message, current, clearLabel, onChosen, dismiss)
    }
}
