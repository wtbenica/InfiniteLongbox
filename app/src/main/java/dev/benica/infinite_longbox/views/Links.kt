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
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import dev.benica.infinite_longbox.R
import dev.benica.infinite_longbox.database.models.FullSeries
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class SeriesLink(
    context: Context,
    attrs: AttributeSet,
) : AppCompatTextView(context, attrs, R.attr.styleLinkTextView), View.OnClickListener {

    internal var callback: SeriesLinkCallback? = null

    internal var series: FullSeries? = null
        set(value) {
            field = value
            text = series?.series?.fullDescription
        }

    init {
        setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        series?.let { callback?.seriesClicked(it) }
    }

    companion object {
        @Suppress("unused")
        private const val TAG = "CreatorLink"
    }

    interface SeriesLinkCallback {
        fun seriesClicked(series: FullSeries)
    }
}
