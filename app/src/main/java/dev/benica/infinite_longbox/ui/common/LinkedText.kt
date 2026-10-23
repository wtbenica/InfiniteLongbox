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

package dev.benica.infinite_longbox.ui.common

import android.text.Spanned
import android.text.style.URLSpan
import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration

/**
 * A string resource with links in it (`<a href=...>`) as text whose links open in the browser: the
 * resource's own markup is read, so the strings stay as they are in strings.xml.
 */
@Composable
fun linkedString(@StringRes id: Int): AnnotatedString {
    val text = LocalContext.current.getText(id)
    val link = TextLinkStyles(
        SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)
    )
    return buildAnnotatedString {
        append(text.toString())
        (text as? Spanned)?.getSpans(0, text.length, URLSpan::class.java)?.forEach {
            addLink(LinkAnnotation.Url(it.url, link), text.getSpanStart(it), text.getSpanEnd(it))
        }
    }
}
