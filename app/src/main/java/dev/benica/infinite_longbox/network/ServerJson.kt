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

import com.google.gson.Gson
import java.lang.reflect.Type
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializerOrNull
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * How the server's answers are read.
 *
 * A type marked @Serializable is read with kotlinx.serialization, which knows Kotlin's null rules
 * and defaults: a missing required field, or a null in a field that can't hold one, fails at once
 * with a message saying which, where Gson would leave a null in the object to fail somewhere else.
 * Everything else (the sync's GcdJson classes) is still read with Gson.
 */
object ServerJson {
    /** The server may send more than the app reads. */
    val json = Json { ignoreUnknownKeys = true }

    private val gson = Gson()

    fun usesKotlinx(type: Type): Boolean = json.serializersModule.serializerOrNull(type) != null

    /** [text] read as [type], the way the app reads the server's answer to a call that returns it. */
    @Suppress("UNCHECKED_CAST")
    fun <T> decode(text: String, type: Type): T {
        val serializer = json.serializersModule.serializerOrNull(type)
        return if (serializer != null) {
            json.decodeFromString(serializer, text) as T
        } else {
            gson.fromJson(text, type)
        }
    }

    /** For Retrofit: takes the types kotlinx.serialization can read and leaves the rest to the next converter. */
    fun converterFactory(): Converter.Factory {
        val inner = json.asConverterFactory("application/json".toMediaType())
        return object : Converter.Factory() {
            override fun responseBodyConverter(
                type: Type,
                annotations: Array<Annotation>,
                retrofit: Retrofit,
            ) = if (usesKotlinx(type)) inner.responseBodyConverter(type, annotations, retrofit) else null

            override fun requestBodyConverter(
                type: Type,
                parameterAnnotations: Array<Annotation>,
                methodAnnotations: Array<Annotation>,
                retrofit: Retrofit,
            ) = if (usesKotlinx(type)) {
                inner.requestBodyConverter(type, parameterAnnotations, methodAnnotations, retrofit)
            } else null
        }
    }
}
