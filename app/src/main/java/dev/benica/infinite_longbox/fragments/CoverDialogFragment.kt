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
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.DialogFragment
import dev.benica.infinite_longbox.database.models.FullIssue
import dev.benica.infinite_longbox.ui.covers.CoverViewer
import dev.benica.infinite_longbox.ui.theme.InfiniteLongboxTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.io.File

/** An issue's cover as large as the screen allows. */
@ExperimentalCoroutinesApi
class CoverDialogFragment(private val coverUri: Uri, private val currentIssue: FullIssue) :
    DialogFragment() {

    override fun onResume() {
        super.onResume()
        dialog?.window?.let { window ->
            window.setLayout(MATCH_PARENT, MATCH_PARENT)
            window.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            InfiniteLongboxTheme {
                CoverViewer(
                    file = File(coverUri.path.orEmpty()),
                    title = currentIssue.toString(),
                    variantName = currentIssue.issue.variantName,
                    onDismiss = ::dismiss,
                )
            }
        }
    }
}
