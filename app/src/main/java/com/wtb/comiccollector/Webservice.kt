package com.wtb.comiccollector

import com.wtb.comiccollector.database.daos.Count
import com.wtb.comiccollector.database.models.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import retrofit2.http.GET
import retrofit2.http.Path

@ExperimentalCoroutinesApi
interface Webservice {
    // GET By IDs list
    @GET("db_query/series/{seriesIds}")
    suspend fun getSeriesByIds(@Path("seriesIds") seriesIds: List<Int>): List<Item<GcdSeries, Series>>

    @GET("/db_query/issues/{issueIds}")
    suspend fun getIssuesByIds(@Path("issueIds") issueIds: List<Int>): List<Item<GcdIssue, Issue>>

    @GET("/db_query/story/{storyIds}")
    suspend fun getStoriesByIds(@Path("storyIds") storyIds: List<Int>): List<Item<GcdStory, Story>>

    @GET("/db_query/creator_list/{creatorIds}")
    suspend fun getCreatorsByIds(@Path("creatorIds") creatorIds: List<Int>): List<Item<GcdCreator, Creator>>

    @GET("db_query/characters/ids/{ids}")
    suspend fun getCharactersByIds(@Path("ids") ids: List<Int>): List<Item<GcdCharacter, Character>>

    @GET("/db_query/name_detail/{nameDetailIds}")
    suspend fun getNameDetailsByIds(@Path("nameDetailIds") nameDetailIds: List<Int>): List<Item<GcdNameDetail, NameDetail>>

    @GET("db_query/publishers/ids/{ids}")
    suspend fun getPublishersByIds(@Path("ids") ids: List<Int>): List<Item<GcdPublisher, Publisher>>

    @GET("/db_query/series/{seriesId}/issues")
    suspend fun getIssuesBySeries(@Path("seriesId") seriesId: Int): List<Item<GcdIssue, Issue>>

    // GET by page
    @GET("/db_query/creators/all/{page}")
    suspend fun getCreatorsByPage(@Path("page") page: Int): List<Item<GcdCreator, Creator>>

    @GET("/db_query/name_details_list/{page}")
    suspend fun getNameDetailsByPage(@Path("page") page: Int): List<Item<GcdNameDetail, NameDetail>>

    @GET("/db_query/series_list/{page}")
    suspend fun getSeriesByPage(@Path("page") page: Int): List<Item<GcdSeries, Series>>

    @GET("db_query/characters/{page}")
    suspend fun getCharactersByPage(@Path("page") page: Int): List<Item<GcdCharacter, Character>>

    @GET("/db_query/publisher_list/{page}")
    suspend fun getPublisherByPage(@Path("page") page: Int): List<Item<GcdPublisher, Publisher>>

    // GET num pages
    @GET("/db_query/creator_page_count")
    suspend fun getNumCreatorPages(): Count

    @GET("/db_query/name_detail_page_count")
    suspend fun getNumNameDetailPages(): Count

    @GET("/db_query/series_page_count")
    suspend fun getNumSeriesPages(): Count

    @GET("/db_query/character_page_count")
    suspend fun getNumCharacterPages(): Count

    @GET("/db_query/publisher_page_count")
    suspend fun getNumPublisherPages(): Count

    // GET Credits
    @GET("/db_query/name_detail/{nameDetailIds}/credits")
    suspend fun getCreditsByNameDetail(@Path("nameDetailIds") nameDetailIds: List<Int>): List<Item<GcdCredit, Credit>>

    @GET("/db_query/name_detail/{nameDetailIds}/extracts")
    suspend fun getExCreditsByNameDetail(@Path("nameDetailIds") nameDetailIds: List<Int>): List<Item<GcdExCredit, ExCredit>>

    @GET("/db_query/stories/{storyIds}/credits")
    suspend fun getCreditsByStoryIds(@Path("storyIds") storyId: List<Int>): List<Item<GcdCredit, Credit>>

    @GET("/db_query/stories/{storyIds}/extracts")
    suspend fun getExCreditsByStoryIds(@Path("storyIds") storyIds: List<Int>): List<Item<GcdExCredit, ExCredit>>

    // GET Stories
    @GET("/db_query/issues/{issueIds}/stories")
    suspend fun getStoriesByIssueIds(@Path("issueIds") issueIds: List<Int>): List<Item<GcdStory, Story>>

    // GET Roles
    @GET("/db_query/role")
    suspend fun getRoles(): List<Item<GcdRole, Role>>

    // GET Story Types
    @GET("/db_query/story_types")
    suspend fun getStoryTypes(): List<Item<GcdStoryType, StoryType>>

    // GET Series Bonds
    @GET("db_query/series_bonds")
    suspend fun getSeriesBonds(): List<Item<GcdSeriesBond, SeriesBond>>

    // GET bond types
    @GET("db_query/series_bond_types")
    suspend fun getBondTypes(): List<Item<GcdBondType, BondType>>

    // GET Characters
    @GET("db_query/story/{storyIds}/characters")
    suspend fun getAppearancesByStoryIds(@Path("storyIds") storyIds: List<Int>):
            List<Item<GcdCharacterAppearance, Appearance>>

    // GET Appearances
    @GET("db_query/character/{characterId}/appearances")
    suspend fun getAppearancesByCharacterIds(@Path("characterId") characterId: Int): List<Item<GcdCharacterAppearance, Appearance>>

    companion object {
        private const val TAG = "Webservice"
    }
}

class PageCount(
    val pageCount: Int
)
