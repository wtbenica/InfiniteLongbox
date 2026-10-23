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

import dev.benica.infinite_longbox.database.daos.Count
import dev.benica.infinite_longbox.database.models.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import dev.benica.infinite_longbox.repository.SeedMeta
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import dev.benica.infinite_longbox.repository.MetronCoverOption
import retrofit2.http.Body
import retrofit2.http.POST
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

@ExperimentalCoroutinesApi
interface Webservice {
    // Starter database (see SeedInstaller)
    @GET("/db_query/seed/meta")
    suspend fun getSeedMeta(): SeedMeta

    @Streaming
    @GET("/db_query/seed/db")
    suspend fun getSeedDb(): ResponseBody

    // Covers kept on the server (see CoverStore)
    @GET("/db_query/covers/have")
    suspend fun getServerCoverIds(): List<Int>

    /**
     * Asks the server to fetch the issue's cover from Metron; 404 when Metron has none. For a
     * variant, [parent] is the issue it is a variant of, and either its [name] picks the cover
     * or [choice] (an id from [metronCoverOptions]) does.
     */
    @POST("/db_query/issue/id/{id}/cover/fetch")
    suspend fun fetchCoverFromMetron(
        @Path("id") issueId: Int,
        @Query("parent") parent: Int? = null,
        @Query("name") name: String? = null,
        @Query("choice") choice: String? = null,
    ): Response<ResponseBody>

    /** The covers Metron has for the issue (or for [parent], if the issue is a variant of it). */
    @GET("/db_query/issue/id/{id}/cover/metron/options")
    suspend fun metronCoverOptions(
        @Path("id") issueId: Int,
        @Query("parent") parent: Int? = null,
    ): List<MetronCoverOption>

    @GET("/db_query/issue/id/{id}/cover/metron/preview/{choice}")
    suspend fun metronCoverPreview(
        @Path("id") issueId: Int,
        @Path("choice") choice: String,
        @Query("parent") parent: Int? = null,
    ): ResponseBody

    @POST("/db_query/issue/id/{id}/cover/upload")
    suspend fun uploadCover(
        @Path("id") issueId: Int,
        @Body image: RequestBody,
    ): Response<ResponseBody>

    @POST("/db_query/issue/id/{id}/cover/remove")
    suspend fun removeCover(@Path("id") issueId: Int): Response<ResponseBody>

    /** Everything the server knows about a character, for its page. */
    @GET("/db_query/character/{id}/info")
    suspend fun getCharacterInfo(@Path("id") id: Int): dev.benica.infinite_longbox.repository.CharacterInfo

    /** Everything the server knows about a creator, for their page. */
    @GET("/db_query/creator/{id}/info")
    suspend fun getCreatorInfo(@Path("id") id: Int): dev.benica.infinite_longbox.repository.CreatorInfo

    /** What version each page of the paged models is, so only changed pages are fetched again. */
    @GET("/db_query/manifest")
    suspend fun getManifest(): Map<String, Manifest>

    // GET num pages
    @GET("/db_query/name_detail/page_count")
    suspend fun getNumNameDetailPages(): Count

    @GET("/db_query/series/page_count")
    suspend fun getNumSeriesPages(): Count

    @GET("/db_query/character/page_count")
    suspend fun getNumCharacterPages(): Count

    @GET("/db_query/publisher/page_count")
    suspend fun getNumPublisherPages(): Count

    // GET by page
    @GET("/db_query/publisher/page/{page}")
    suspend fun getPublisherByPage(@Path("page") page: Int): List<Item<GcdPublisher>>

    @GET("/db_query/series/page/{page}")
    suspend fun getSeriesByPage(@Path("page") page: Int): List<Item<GcdSeries>>

    @GET("/db_query/name_detail/page/{page}")
    suspend fun getNameDetailsByPage(@Path("page") page: Int): List<Item<GcdNameDetail>>

    @GET("db_query/character/page/{page}")
    suspend fun getCharactersByPage(@Path("page") page: Int): List<Item<GcdCharacter>>

    // GET By IDs list
    @GET("db_query/publisher/ids/{ids}")
    suspend fun getPublishersByIds(@Path("ids") ids: List<Int>): List<Item<GcdPublisher>>

    @GET("db_query/series/ids/{seriesIds}")
    suspend fun getSeriesByIds(@Path("seriesIds") seriesIds: List<Int>): List<Item<GcdSeries>>

    @GET("/db_query/issue/upc/{upc}")
    suspend fun getIssuesByUpc(
        @Path("upc") upc: String,
        @Query("number") number: String? = null,
        @Query("limit") limit: Int? = null,
    ): List<Item<GcdIssue>>

    /** For each series, the earliest of its issues that have a cover on the server. */
    @GET("/db_query/series/covers/{seriesIds}")
    suspend fun getSeriesCovers(@Path("seriesIds") seriesIds: List<Int>): Map<String, List<Int>>

    @GET("/db_query/issue/ids/{issueIds}")
    suspend fun getIssuesByIds(@Path("issueIds") issueIds: List<Int>): List<Item<GcdIssue>>

    @GET("/db_query/story/ids/{storyIds}")
    suspend fun getStoriesByIds(@Path("storyIds") storyIds: List<Int>): List<Item<GcdStory>>

    @GET("/db_query/creator/ids/{creatorIds}")
    suspend fun getCreatorsByIds(@Path("creatorIds") creatorIds: List<Int>): List<Item<GcdCreator>>

    @GET("/db_query/name_detail/ids/{nameDetailIds}")
    suspend fun getNameDetailsByIds(@Path("nameDetailIds") nameDetailIds: List<Int>): List<Item<GcdNameDetail>>

    @GET("db_query/character/ids/{ids}")
    suspend fun getCharactersByIds(@Path("ids") ids: List<Int>): List<Item<GcdCharacter>>

    // GET _ by Series
    @GET("/db_query/series/id/{seriesId}/issues")
    suspend fun getIssuesBySeries(@Path("seriesId") seriesId: Int): List<Item<GcdIssue>>

    // GET _ by Issues
    @GET("/db_query/issue/ids/{issueIds}/stories")
    suspend fun getStoriesByIssueIds(@Path("issueIds") issueIds: List<Int>): List<Item<GcdStory>>

    // GET _ by Stories
    @GET("/db_query/story/ids/{storyIds}/credits")
    suspend fun getCreditsByStoryIds(@Path("storyIds") storyId: List<Int>): List<Item<GcdCredit>>

    @GET("/db_query/story/ids/{storyIds}/extracts")
    suspend fun getExCreditsByStoryIds(@Path("storyIds") storyIds: List<Int>): List<Item<GcdExCredit>>

    @GET("db_query/story/ids/{storyIds}/appearances")
    suspend fun getAppearancesByStoryIds(@Path("storyIds") storyIds: List<Int>):
            List<Item<GcdCharacterAppearance>>

    // GET _ by NameDetail
    @GET("/db_query/name_detail/ids/{nameDetailIds}/credits")
    suspend fun getCreditsByNameDetail(@Path("nameDetailIds") nameDetailIds: List<Int>): List<Item<GcdCredit>>

    @GET("/db_query/name_detail/ids/{nameDetailIds}/extracts")
    suspend fun getExCreditsByNameDetail(@Path("nameDetailIds") nameDetailIds: List<Int>): List<Item<GcdExCredit>>

    // GET _ by Characters
    @GET("db_query/character/ids/{characterIds}/appearances")
    suspend fun getAppearancesByCharacterIds(@Path("characterIds") characterId: List<Int>): List<Item<GcdCharacterAppearance>>

    // GET Roles
    @GET("/db_query/role/all")
    suspend fun getRoles(): List<Item<GcdRole>>

    // GET Story Types
    @GET("/db_query/story_type/all")
    suspend fun getStoryTypes(): List<Item<GcdStoryType>>

    // GET Series Bonds
    @GET("db_query/series_bond/all")
    suspend fun getSeriesBonds(): List<Item<GcdSeriesBond>>

    // GET bond types
    @GET("db_query/series_bond_type/all")
    suspend fun getBondTypes(): List<Item<GcdBondType>>

    companion object {
        private const val TAG = "Webservice"
    }
}

/** One paged model's pages, each as a short fingerprint of its content. */
@Serializable
data class Manifest(val pages: Int, val hashes: List<String>)
