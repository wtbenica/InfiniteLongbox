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

package dev.benica.infinite_longbox.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.benica.infinite_longbox.R

/** Where the current issue sits in its series; [position] is -1 until it's known. */
data class JumpBarState(val position: Int = -1, val count: Int = 0) {
    private val known get() = position >= 0 && count > 0
    val canGoBack get() = known && position > 0
    val canSkipBack get() = known && position >= 10
    val canGoForward get() = known && position < count - 1
    val canSkipForward get() = known && position <= count - 11
}

/** First / -10 / -1 / +1 / +10 / Last. [onJump] gets how many issues to move. */
@Composable
fun IssueJumpBar(state: JumpBarState, onJump: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        JumpButton(R.string.goto_start, state.canGoBack, Modifier) { onJump(-state.position) }
        JumpButton(R.string.goto_skip_back, state.canSkipBack, Modifier.weight(1f)) { onJump(-10) }
        JumpButton(R.string.goto_one_back, state.canGoBack, Modifier.weight(1f)) { onJump(-1) }
        JumpButton(R.string.goto_one_forward, state.canGoForward, Modifier.weight(1f)) { onJump(1) }
        JumpButton(R.string.goto_skip_forward, state.canSkipForward, Modifier.weight(1f)) {
            onJump(
                10
            )
        }
        JumpButton(R.string.goto_end, state.canGoForward, Modifier) {
            onJump(state.count - state.position - 1)
        }
    }
}

@Composable
private fun JumpButton(
    label: Int,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    TextButton(onClick = onClick, enabled = enabled, modifier = modifier) {
        Text(stringResource(label))
    }
}

@Preview(showBackground = true)
@Composable
private fun IssueJumpBarPreview() {
    InfiniteLongboxTheme {
        IssueJumpBar(JumpBarState(position = 12, count = 40), onJump = {})
    }
}
