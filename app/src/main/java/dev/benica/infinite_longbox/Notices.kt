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
package dev.benica.infinite_longbox

import androidx.annotation.StringRes
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Short messages for the person, from anywhere in the app; [AppScreen] shows them as snackbars. */
object Notices {
    class Notice(val text: String, val long: Boolean)

    private val queue = MutableSharedFlow<Notice>(extraBufferCapacity = 16)
    val notices: SharedFlow<Notice> = queue.asSharedFlow()

    fun show(text: String, long: Boolean = false) {
        queue.tryEmit(Notice(text, long))
    }

    fun show(@StringRes id: Int, vararg args: Any, long: Boolean = false) {
        val context = InfiniteLongboxApplication.context ?: return
        show(context.getString(id, *args), long)
    }
}
