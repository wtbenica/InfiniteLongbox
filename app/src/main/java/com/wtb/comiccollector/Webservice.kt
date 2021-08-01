package com.wtb.comiccollector

import android.util.Log
import com.wtb.comiccollector.database.models.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import retrofit2.HttpException
import retrofit2.http.GET
import retrofit2.http.Path
import java.net.ConnectException
import java.net.SocketTimeoutException

@ExperimentalCoroutinesApi
interface Webservice {
    // GET By ID list
    @GET("db_query/series/{seriesIds}")
    suspend fun getSeriesByIds(@Path("seriesIds") seriesIds: List<Int>): List<Item<GcdSeries,
            Series>>

    @GET("/db_query/issues/{issueIds}")
    suspend fun getIssuesByIds(@Path("issueIds") issueIds: List<Int>): List<Item<GcdIssue, Issue>>

    @GET("/db_query/story/{storyIds}")
    suspend fun getStoriesByIds(@Path("storyIds") storyIds: List<Int>): List<Item<GcdStory, Story>>

    @GET("/db_query/creators/{creatorIds}")
    suspend fun getCreatorsByIds(@Path("creatorIds") creatorIds: List<Int>): List<Item<GcdCreator, Creator>>

    @GET("db_query/characters/{page}")
    suspend fun getCharactersByIds(@Path("ids") ids: List<Int>): List<Item<GcdCharacter, Character>>

    @GET("/db_query/name_detail/{nameDetailIds}")
    suspend fun getNameDetailsByIds(@Path("nameDetailIds") nameDetailIds: List<Int>):
            List<Item<GcdNameDetail, NameDetail>>


    // GET Issues
    @GET("/db_query/issues_list/{page}")
    suspend fun getIssuesByPage(@Path("page") page: Int): List<Item<GcdIssue, Issue>>

    @GET("/db_query/series/{seriesId}/issues")
    suspend fun getIssuesBySeries(@Path("seriesId") seriesId: Int): List<Item<GcdIssue, Issue>>

    // GET Credits
    @GET("/db_query/name_detail/{nameDetailIds}/credits")
    suspend fun getCreditsByNameDetail(@Path("nameDetailIds") nameDetailIds: List<Int>): List<Item<GcdCredit, Credit>>

    @GET("/db_query/name_detail/{nameDetailIds}/extracts")
    suspend fun getExtractedCreditsByNameDetail(@Path("nameDetailIds") nameDetailIds: List<Int>):
            List<Item<GcdExCredit, ExCredit>>

    @GET("/db_query/stories/{storyIds}/credits")
    suspend fun getCreditsByStoryIds(@Path("storyIds") storyIds: List<Int>): List<Item<GcdCredit, Credit>>

    @GET("/db_query/credits_list/{page}")
    suspend fun getCreditsByPage(@Path("page") page: Int): List<Item<GcdCredit, Credit>>

    @GET("/db_query/excredits_list/{page}")
    suspend fun getExCreditsByPage(@Path("page") page: Int): List<Item<GcdExCredit, ExCredit>>

    @GET("/db_query/stories/{storyIds}/extracts")
    suspend fun getExtractedCreditsByStories(@Path("storyIds") storyIds: List<Int>): List<Item<GcdExCredit, ExCredit>>

    @GET("/db_query/name_details_list/{page}")
    suspend fun getNameDetailsByPage(@Path("page") page: Int): List<Item<GcdNameDetail, NameDetail>>

    // GET Stories
    @GET("/db_query/stories_list/{page}")
    suspend fun getStoriesByPage(@Path("page") page: Int): List<Item<GcdStory, Story>>

    @GET("/db_query/issue/{issueId}/stories")
    suspend fun getStoriesByIssue(@Path("issueId") issueId: Int): List<Item<GcdStory, Story>>

    @GET("/db_query/issues/{issueIds}/stories")
    suspend fun getStoriesByIssues(@Path("issueIds") issueIds: List<Int>): List<Item<GcdStory, Story>>

    // GET Creator
    @GET("/db_query/creators/all/{page}")
    suspend fun getCreatorsByPage(@Path("page") page: Int): List<Item<GcdCreator, Creator>>

    // GET NameDetails
    @GET("/db_query/name_details/creator_ids/{creatorIds}")
    suspend fun getNameDetailsByCreatorIds(@Path("creatorIds") creatorIds: List<Int>):
            List<Item<GcdNameDetail, NameDetail>>

    @GET("/db_query/name_detail/name/{name}")
    suspend fun getNameDetailByName(@Path("name") name: String): List<Item<GcdNameDetail,
            NameDetail>>

    // GET Publishers, Roles, Series, StoryTypes, Stories
    @GET("/db_query/publisher")
    suspend fun getPublishers(): List<Item<GcdPublisher, Publisher>>

    @GET("/db_query/role")
    suspend fun getRoles(): List<Item<GcdRole, Role>>

    @GET("/db_query/story_types")
    suspend fun getStoryTypes(): List<Item<GcdStoryType, StoryType>>

    @GET("/db_query/series_list/{page}")
    suspend fun getSeriesByPage(@Path("page") page: Int): List<Item<GcdSeries, Series>>

    @GET("db_query/series_bonds")
    suspend fun getSeriesBonds(): List<Item<GcdSeriesBond, SeriesBond>>

    @GET("db_query/series_bond_types")
    suspend fun getBondTypes(): List<Item<GcdBondType, BondType>>

    @GET("db_query/characters/{page}")
    suspend fun getCharactersByPage(@Path("page") page: Int): List<Item<GcdCharacter, Character>>

    @GET("db_query/story/{storyIds}/characters")
    suspend fun getAppearancesByStory(@Path("storyIds") storyIds: List<Int>):
            List<Item<GcdCharacterAppearance, Appearance>>

    @GET("db_query/appearances_list/{page}")
    suspend fun getAppearancesByPage(@Path("page") page: Int): List<Item<GcdCharacterAppearance, Appearance>>

    @GET("db_query/character/{characterId}/appearances")
    suspend fun getAppearances(@Path("characterId") characterId: Int): List<Item<GcdCharacterAppearance, Appearance>>

    @GET("db_query/publishers/ids/{ids}")
    suspend fun getPublishersByIds(ids: List<Int>): List<Item<GcdPublisher, Publisher>>

    companion object {
        private const val TAG = "Webservice"

        fun runSafely(lambda: () -> Any) {
            try {
                lambda()
            } catch (e: SocketTimeoutException) {
                Log.d(TAG, "$lambda $e")
            } catch (e: ConnectException) {
                Log.d(TAG, "$lambda $e")
            } catch (e: HttpException) {
                Log.d(TAG, "$lambda $e")
            }
        }
    }
}

