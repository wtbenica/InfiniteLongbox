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
package dev.benica.infinite_longbox.nav

import android.content.Context
import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import dev.benica.infinite_longbox.MainActivity
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * A lifecycle that follows a screen's being on show: it is resumed when the screen appears and
 * destroyed when it goes, so what a screen observes or launches stops with it.
 */
class ScreenLifecycle : LifecycleOwner {
    private val registry = LifecycleRegistry.createUnsafe(this)
    override val lifecycle: Lifecycle get() = registry

    fun resume() = registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

    fun destroy() = registry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
}

/** What a screen is given: the activity it reports to, its place on the stack, and its lifecycle. */
@OptIn(ExperimentalCoroutinesApi::class)
class ScreenHost(val activity: MainActivity, val entry: Entry, val lifecycleOwner: LifecycleOwner)

/**
 * A screen on the stack. [onShown] runs when it comes on show (and again each time it returns to
 * the top), [onHidden] when it goes; [Content] is what it draws.
 */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class Screen(protected val host: ScreenHost) {
    protected val activity: MainActivity get() = host.activity
    protected val context: Context get() = host.activity
    protected val resources: Resources get() = host.activity.resources
    protected val viewLifecycleOwner: LifecycleOwner get() = host.lifecycleOwner

    protected fun requireContext(): Context = host.activity
    protected fun requireActivity(): MainActivity = host.activity
    protected fun getString(@StringRes id: Int, vararg args: Any): String = host.activity.getString(id, *args)

    open fun onShown() {}
    open fun onHidden() {}

    @Composable
    abstract fun Content()
}
