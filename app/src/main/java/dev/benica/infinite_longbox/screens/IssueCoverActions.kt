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

package dev.benica.infinite_longbox.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import dev.benica.infinite_longbox.Notices
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.benica.infinite_longbox.MainActivity
import dev.benica.infinite_longbox.R
import dev.benica.infinite_longbox.database.models.Issue
import dev.benica.infinite_longbox.repository.MetronCoverOption
import dev.benica.infinite_longbox.repository.MetronCoverResult
import dev.benica.infinite_longbox.repository.fetchCoverFromMetron
import dev.benica.infinite_longbox.repository.metronCoverOptions
import dev.benica.infinite_longbox.repository.metronCoverPreview
import dev.benica.infinite_longbox.repository.pickedCoverToIssue
import dev.benica.infinite_longbox.repository.removeCover
import dev.benica.infinite_longbox.ui.covers.MetronCoverPicker
import dev.benica.infinite_longbox.ui.dialogs.ChoiceDialog
import dev.benica.infinite_longbox.ui.dialogs.showDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

/** Adding, replacing and removing the cover of the issue [current] gives: from Metron, or from this device. */
@OptIn(ExperimentalCoroutinesApi::class)
class IssueCoverActions(
    private val activity: MainActivity,
    private val scope: CoroutineScope,
    private val current: () -> Issue,
) {
    /** Set when composed: it needs the activity result registry. */
    private var pickCoverImage: ManagedActivityResultLauncher<Intent, ActivityResult>? = null

    private fun launchPicker() {
        val request = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        pickCoverImage?.launch(Intent.createChooser(request, activity.getString(R.string.cover_choose_title)))
    }

    /** The cover button on an issue with none: from Metron (through the server), or from this device. */
    fun addOptions() {
        val options = arrayOf(
            activity.getString(R.string.cover_from_metron),
            activity.getString(R.string.cover_from_device),
        )
        activity.showDialog { dismiss ->
            ChoiceDialog(
                R.string.cover_add_title, options.toList(), null,
                onChoose = { which ->
                    when (which) {
                        null -> Unit
                        0 -> fromMetron()
                        else -> launchPicker()
                    }
                },
                dismiss = dismiss,
            )
        }
    }

    private var picker by mutableStateOf<List<MetronCoverOption>?>(null)

    /** Fetches the cover from Metron; for a variant, falls back to the covers Metron offers to pick from. */
    private fun fromMetron() {
        val issue = current()
        val context = activity.applicationContext
        Notices.show(R.string.cover_metron_working)
        scope.launch {
            val result = fetchCoverFromMetron(context, issue.issueId, issue.variantOf, issue.variantName)
            if ((result == MetronCoverResult.NONE || result == MetronCoverResult.AMBIGUOUS) && issue.variantOf != null) {
                chooseFromMetron()
            } else {
                Notices.show(result.message)
            }
        }
    }

    private suspend fun chooseFromMetron() {
        val issue = current()
        val options = metronCoverOptions(issue.issueId, issue.variantOf)
        when {
            options.isNullOrEmpty() ->
                Notices.show(R.string.cover_metron_none)
            // One cover to choose from is no choice: it is the one asked for, and can be replaced
            options.size == 1 -> metronPicked(options.first())
            else -> picker = options
        }
    }

    private fun metronPicked(option: MetronCoverOption) {
        picker = null
        val issue = current()
        val context = activity.applicationContext
        scope.launch {
            val result = fetchCoverFromMetron(context, issue.issueId, issue.variantOf, choice = option.id)
            Notices.show(result.message)
        }
    }

    private fun picked(uri: Uri?) {
        uri ?: return
        val issueId = current().issueId
        scope.launch {
            val message = pickedCoverToIssue(activity.applicationContext, issueId, uri)
            Notices.show(message)
        }
    }

    /** Long-press on an existing cover: replace it, or remove it. */
    fun options() {
        val issueId = current().issueId
        val options = arrayOf(
            activity.getString(R.string.cover_replace),
            activity.getString(R.string.cover_replace_from_metron),
            activity.getString(R.string.cover_remove),
        )
        activity.showDialog { dismiss ->
            ChoiceDialog(
                R.string.cover_options_title, options.toList(), null,
                onChoose = { which ->
                    when (which) {
                        null -> Unit
                        0 -> launchPicker()
                        1 -> fromMetron()
                        else -> scope.launch {
                            val message = removeCover(activity.applicationContext, issueId)
                            Notices.show(message)
                        }
                    }
                },
                dismiss = dismiss,
            )
        }
    }

    /** What the actions put on screen: the Metron picker, and the file picker's launcher. */
    @Composable
    fun Dialogs() {
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) picked(result.data?.data)
        }
        SideEffect { pickCoverImage = launcher }
        picker?.let { options ->
            val shown = current()
            MetronCoverPicker(
                options = options,
                loadPreview = { metronCoverPreview(shown.issueId, shown.variantOf, it.id) },
                onPick = ::metronPicked,
                onDismiss = { picker = null },
            )
        }
    }
}
