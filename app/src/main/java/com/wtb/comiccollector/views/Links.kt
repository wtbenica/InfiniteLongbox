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
import android.content.Intent
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatTextView
import com.squareup.picasso.Picasso
import com.wtb.comiccollector.R
import com.wtb.comiccollector.database.models.FullCharacter
import com.wtb.comiccollector.database.models.FullSeries
import com.wtb.comiccollector.database.models.NameDetailAndCreator
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class CreatorLink(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.styleLinkTextView,
) : AppCompatTextView(context, attrs, defStyleAttr), View.OnClickListener {

    internal var callback: CreatorLinkCallback? = null

    internal var creator: NameDetailAndCreator? = null
        set(value) {
            field = value
            val nameDetail = creator?.nameDetail?.name
            val creator = creator?.creator?.name
            val creditName = if (nameDetail != creator) {
                "$creator (as $nameDetail)"
            } else {
                creator
            }
            text = creditName
        }

    init {
        setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        Log.d(TAG, "Creator Clicked!!!!!")
        creator?.let { callback?.creatorClicked(it) }
    }

    companion object {
        private const val TAG = "CreatorLink"
    }

    interface CreatorLinkCallback {
        fun creatorClicked(creator: NameDetailAndCreator)
    }
}

@ExperimentalCoroutinesApi
class CharacterLink @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.styleLinkTextView,
) : AppCompatTextView(context, attrs, defStyleAttr), View.OnClickListener {

    internal var callback: CharacterLinkCallback? = null

    internal var character: FullCharacter? = null
        set(value) {
            field = value
            val name = character?.character?.name
            val alterEgo = character?.character?.alterEgo
            val creditName = if (alterEgo != null) {
                "$name ($alterEgo)"
            } else {
                name
            }
            text = creditName
        }

    init {
        setTextAppearance(R.style.RoleNameText)
        setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        character?.let { callback?.characterClicked(it) }
    }

    companion object {
        private const val TAG = "CreatorLink"
    }

    interface CharacterLinkCallback {
        fun characterClicked(character: FullCharacter)
    }
}

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
        Log.d(TAG, "Creator Clicked!!!!!")
        series?.let { callback?.seriesClicked(it) }
    }

    companion object {
        private const val TAG = "CreatorLink"
    }

    interface SeriesLinkCallback {
        fun seriesClicked(series: FullSeries)
    }
}

class WebLink(
    context: Context,
    attrs: AttributeSet?,
) : AppCompatButton(context, attrs) {
    private val styledAttrs = context.theme.obtainStyledAttributes(attrs, R.styleable.ImageWebLink, 0, 0)
    var url: (() -> String?)? = null

    init {
        setOnClickListener {
            val invokeUrl = url?.invoke() ?: styledAttrs.getString(R.styleable.WebLink_url_weblink)
            if (invokeUrl == null) {
                throw IllegalStateException("Weblink: Missing url")
            } else {
                val intent = Intent().apply {
                    action = Intent.ACTION_VIEW
                    data = Uri.parse(invokeUrl)
                }
                context.startActivity(intent)
            }
        }
    }
}

class ImageWebLink(
    context: Context,
    attrs: AttributeSet?,
) : AppCompatImageButton(context, attrs) {
    private val styledAttrs = context.theme.obtainStyledAttributes(attrs, R.styleable.ImageWebLink, 0, 0)
    private var url: (() -> String?)? = null

    init {
        styledAttrs.getString(R.styleable.AppCompatImageView_android_src) ?: styledAttrs
            .getString(R.styleable.ImageWebLink_img_src).also {
                Picasso.get().load(it).into(this)
            }

        setOnClickListener {
            val invokeUrl =
                url?.invoke() ?: styledAttrs.getString(R.styleable.ImageWebLink_url_imageweblink)
            if (invokeUrl == null) {
                throw IllegalStateException("Weblink: Missing url")
            } else {
                val intent = Intent().apply {
                    action = Intent.ACTION_VIEW
                    data = Uri.parse(invokeUrl)
                }
                context.startActivity(intent)
            }
        }
    }
}