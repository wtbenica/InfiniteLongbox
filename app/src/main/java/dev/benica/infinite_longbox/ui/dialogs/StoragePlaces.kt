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

import dev.benica.infinite_longbox.Notices
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import dev.benica.infinite_longbox.R
import dev.benica.infinite_longbox.repository.Repository
import kotlinx.coroutines.launch

/** Every storage place with its copy count, and how many copies are in none; a tap on a place renames it (to a name already used merges the two). */
fun ComponentActivity.showStoragePlaces() {
    lifecycleScope.launch {
        val places = Repository.get().storagePlaces()
        val unstored = Repository.get().unstoredCopies()
        if (places.isEmpty() && unstored == 0) {
            Notices.show(R.string.storage_places_none)
            return@launch
        }
        showDialog { dismiss ->
            ChoiceDialog(
                R.string.storage_places_title,
                places.map { "${it.name} (${it.copies})" } + "${getString(R.string.filter_not_stored)} ($unstored)",
                R.string.scan_done,
                // The last line is the copies in no place: nothing to rename
                onChoose = { which -> which?.let { places.getOrNull(it) }?.let { renameStoragePlace(it.name) } },
                dismiss = dismiss,
            )
        }
    }
}

private fun ComponentActivity.renameStoragePlace(name: String) {
    StoragePrompt.show(
        this, R.string.storage_rename_title,
        getString(R.string.storage_rename_message, name), name, R.string.storage_rename_clear,
    ) { newName ->
        lifecycleScope.launch {
            val copies = Repository.get().renameStorage(name, newName)
            Notices.show(
                if (newName == null) getString(R.string.storage_cleared, copies, name)
                else getString(R.string.storage_renamed, copies, newName)
            )
        }
    }
}
