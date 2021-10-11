package com.wtb.comiccollector

import com.wtb.comiccollector.database.daos.Count
import com.wtb.comiccollector.database.models.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import retrofit2.http.GET
import retrofit2.http.Path

@ExperimentalCoroutinesApi
interface Webservice {
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
    suspend fun getPublisherByPage(@Path("page") page: Int): List<Item<GcdPublisher, Publisher>>

    @GET("/db_query/series/page/{page}")
    suspend fun getSeriesByPage(@Path("page") page: Int): List<Item<GcdSeries, Series>>

    @GET("/db_query/name_details/page/{page}")
    suspend fun getNameDetailsByPage(@Path("page") page: Int): List<Item<GcdNameDetail, NameDetail>>

    @GET("db_query/character/page/{page}")
    suspend fun getCharactersByPage(@Path("page") page: Int): List<Item<GcdCharacter, Character>>

    // GET By IDs list
    @GET("db_query/publisher/ids/{ids}")
    suspend fun getPublishersByIds(@Path("ids") ids: List<Int>): List<Item<GcdPublisher, Publisher>>

    @GET("db_query/series/ids/{seriesIds}")
    suspend fun getSeriesByIds(@Path("seriesIds") seriesIds: List<Int>): List<Item<GcdSeries, Series>>

    @GET("/db_query/issue/ids/{issueIds}")
    suspend fun getIssuesByIds(@Path("issueIds") issueIds: List<Int>): List<Item<GcdIssue, Issue>>

    @GET("/db_query/story/ids/{storyIds}")
    suspend fun getStoriesByIds(@Path("storyIds") storyIds: List<Int>): List<Item<GcdStory, Story>>

    @GET("/db_query/creator/ids/{creatorIds}")
    suspend fun getCreatorsByIds(@Path("creatorIds") creatorIds: List<Int>): List<Item<GcdCreator, Creator>>

    @GET("/db_query/name_detail/ids/{nameDetailIds}")
    suspend fun getNameDetailsByIds(@Path("nameDetailIds") nameDetailIds: List<Int>): List<Item<GcdNameDetail, NameDetail>>

    @GET("db_query/character/ids/{ids}")
    suspend fun getCharactersByIds(@Path("ids") ids: List<Int>): List<Item<GcdCharacter, Character>>

    // GET _ by Series
    @GET("/db_query/series//id/{seriesId}/issues")
    suspend fun getIssuesBySeries(@Path("seriesId") seriesId: Int): List<Item<GcdIssue, Issue>>

    // GET _ by Issues
    @GET("/db_query/issue/ids/{issueIds}/stories")
    suspend fun getStoriesByIssueIds(@Path("issueIds") issueIds: List<Int>): List<Item<GcdStory, Story>>

    // GET _ by Stories
    @GET("/db_query/story/ids/{storyIds}/credits")
    suspend fun getCreditsByStoryIds(@Path("storyIds") storyId: List<Int>): List<Item<GcdCredit, Credit>>

    @GET("/db_query/story/ids/{storyIds}/extracts")
    suspend fun getExCreditsByStoryIds(@Path("storyIds") storyIds: List<Int>): List<Item<GcdExCredit, ExCredit>>

    @GET("db_query/story/ids/{storyIds}/appearances")
    suspend fun getAppearancesByStoryIds(@Path("storyIds") storyIds: List<Int>):
            List<Item<GcdCharacterAppearance, Appearance>>

    // GET _ by NameDetail
    @GET("/db_query/name_detail/ids/{nameDetailIds}/credits")
    suspend fun getCreditsByNameDetail(@Path("nameDetailIds") nameDetailIds: List<Int>): List<Item<GcdCredit, Credit>>

    @GET("/db_query/name_detail/ids/{nameDetailIds}/extracts")
    suspend fun getExCreditsByNameDetail(@Path("nameDetailIds") nameDetailIds: List<Int>): List<Item<GcdExCredit, ExCredit>>

    // GET _ by Characters
    @GET("db_query/character/id/{characterId}/appearances")
    suspend fun getAppearancesByCharacterIds(@Path("characterId") characterId: Int): List<Item<GcdCharacterAppearance, Appearance>>

    // GET Roles
    @GET("/db_query/role/all")
    suspend fun getRoles(): List<Item<GcdRole, Role>>

    // GET Story Types
    @GET("/db_query/story_type/all")
    suspend fun getStoryTypes(): List<Item<GcdStoryType, StoryType>>

    // GET Series Bonds
    @GET("db_query/series_bond/all")
    suspend fun getSeriesBonds(): List<Item<GcdSeriesBond, SeriesBond>>

    // GET bond types
    @GET("db_query/series_bond_type/all")
    suspend fun getBondTypes(): List<Item<GcdBondType, BondType>>

    companion object {
        private const val TAG = "Webservice"
    }
}

class PageCount(
    val pageCount: Int
)
