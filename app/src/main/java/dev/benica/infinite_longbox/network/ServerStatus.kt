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
package dev.benica.infinite_longbox.network

import dev.benica.infinite_longbox.Notices
import dev.benica.infinite_longbox.repository.BASE_URL
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** Whether the server answers; after it fails, requests to it fail at once for a while instead of each waiting out a timeout. */
class ServerStatus(
    private val now: () -> Long = System::currentTimeMillis,
    private val coolOffMs: Long = COOL_OFF_MS,
    private val onDown: () -> Unit = {},
) {
    private var downUntil = 0L
    private var down = false

    @Synchronized
    fun allowed(): Boolean = now() >= downUntil

    @Synchronized
    fun failed() {
        downUntil = now() + coolOffMs
        if (!down) {
            down = true
            onDown()
        }
    }

    @Synchronized
    fun answered() {
        down = false
        downUntil = 0L
    }

    /** Whether [e] says the server can't be reached, rather than that it answered badly. */
    fun isUnreachable(e: IOException) =
        e is ConnectException || e is SocketTimeoutException || e is UnknownHostException

    companion object {
        const val COOL_OFF_MS = 30_000L
        const val CONNECT_TIMEOUT_S = 10L

        /** The app's own server's status; the notice comes once per outage. */
        val server = ServerStatus(onDown = {
            Notices.show("Can't reach the server. Will keep trying in the background.", long = true)
        })

        private val serverHost: String by lazy { BASE_URL.toHttpUrl().host }
    }

    /** Fails requests to the app's server at once while it is cooling off, and notes how each one went. */
    class Guard(private val status: ServerStatus = server) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            if (request.url.host != serverHost) return chain.proceed(request)
            if (!status.allowed()) throw ConnectException("The server was unreachable a moment ago")
            return try {
                chain.proceed(request).also { status.answered() }
            } catch (e: IOException) {
                if (status.isUnreachable(e)) status.failed()
                throw e
            }
        }
    }
}
