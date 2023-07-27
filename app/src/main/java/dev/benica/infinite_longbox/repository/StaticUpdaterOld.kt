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

package dev.benica.infinite_longbox.repository
//
//import android.content.SharedPreferences
//import android.net.Uri
//import android.util.Log
//import dev.benica.infinite_longbox.APP
//import dev.benica.infinite_longbox.ComicCollectorApplication.Companion.context
//import dev.benica.infinite_longbox.Webservice
//import dev.benica.infinite_longbox.database.IssueDatabase
//import dev.benica.infinite_longbox.database.models.*
//import kotlinx.coroutines.*
//import org.jsoup.Jsoup
//import java.net.URL
//
///**
// * Static updater
// *
// * @property webservice
// * @property database
// * @property prefs
// * @constructor Create empty Static updater
// */
//@ExperimentalCoroutinesApi
//class StaticUpdaterOld(
//    webservice: Webservice,
//    database: IssueDatabase,
//    prefs: SharedPreferences,
//) : UpdaterOld(webservice, database, prefs) {
//
//    fun updateSeries(seriesId: Int) = CoroutineScope(Dispatchers.IO).launch {
//        updateSeriesIssues(seriesId)
//    }
//
//    fun updateCharacter(characterId: Int) = CoroutineScope(Dispatchers.IO).launch {
//        updateCharacterAppearances(characterId)
//    }
//
//    fun updateIssue(issueId: Int) = CoroutineScope(Dispatchers.IO).launch {
//        updateIssueStoryDetails(issueId)
//        updateIssueCover(issueId)
//    }
//
//    fun updateCreators(creatorIds: List<Int>) = creatorIds.forEach(this::updateCreator)
//
//    private fun updateCreator(creatorId: Int) = CoroutineScope(Dispatchers.IO).launch {
//        val nameDetails: List<NameDetail> =
//            database.nameDetailDao().getNameDetailsByCreatorId(creatorId)
//        Log.d(TAG, "CRUP: nameDetails: ${nameDetails.size}")
//        nameDetails.forEachIndexed { index, nameDetail ->
//            Log.d(TAG,
//                "CRUP: nameDetail: ${nameDetail.id} ${nameDetail.name} $index of ${nameDetails.size}")
//            updateNameDetailCredits(nameDetail.id)
//            updateNameDetailExCredits(nameDetail.id)
//        }
//    }
//
//    private suspend fun updateIssueStoryDetails(issueId: Int) {
//        val stories: List<Story> = updateIssueStories(issueId)
//        stories.ids.map { updateStoryCredits(it) }
//        stories.ids.map { updateStoryAppearances(it) }
//    }
//
//    private suspend fun updateStoryAppearances(storyId: Int): List<Appearance> =
//        updateById<Appearance>(prefs,
//            null,
//            this@StaticUpdaterOld::getAppearancesByStoryId,
//            this@StaticUpdaterOld::checkFKeysAppearance,
//            database.appearanceDao(),
//            storyId)
//
//    private suspend fun updateStoryCredits(storyId: Int): List<CreditX> =
//        updateById<Credit>(prefs,
//            null,
//            this@StaticUpdaterOld::getCreditsByStoryId,
//            this@StaticUpdaterOld::checkFKeysCredit,
//            database.creditDao(),
//            storyId) +
//                updateById<ExCredit>(prefs,
//                    null,
//                    this@StaticUpdaterOld::getExCreditsByStoryId,
//                    this@StaticUpdaterOld::checkFKeysCredit,
//                    database.exCreditDao(),
//                    storyId)
//
//    private suspend fun updateIssueStories(issueId: Int): List<Story> =
//        updateById<Story>(prefs,
//            null,
//            this@StaticUpdaterOld::getStoriesByIssueId,
//            this@StaticUpdaterOld::checkFKeysStory,
//            database.storyDao(),
//            issueId)
//
//    private suspend fun updateSeriesIssues(seriesId: Int) =
//        updateById<Issue>(prefs,
//            seriesTag(seriesId),
//            this@StaticUpdaterOld::getIssuesBySeriesId,
//            this@StaticUpdaterOld::checkFKeysIssue,
//            database.issueDao(),
//            seriesId)
//
//    private suspend fun updateCharacterAppearances(characterId: Int) =
//        updateById<Appearance>(prefs,
//            characterTag(characterId),
//            this@StaticUpdaterOld::getAppearancesByCharacterId,
//            this@StaticUpdaterOld::checkFKeysAppearance,
//            database.appearanceDao(),
//            characterId)
//
//    private suspend fun updateNameDetailCredits(nameDetailId: Int) {
//        Log.d(TAG, "CRUP: $nameDetailId")
//        updateById<Credit>(
//            prefs,
//            null,
//            this@StaticUpdaterOld::getCreditsByNameDetailId,
//            this@StaticUpdaterOld::checkFKeysCredit,
//            database.creditDao(),
//            nameDetailId
//        )
//    }
//
//    private suspend fun updateNameDetailExCredits(nameDetailId: Int) =
//        updateById<ExCredit>(
//            prefs,
//            null,
//            this@StaticUpdaterOld::getExCreditsByNameDetailId,
//            this@StaticUpdaterOld::checkFKeysCredit,
//            database.exCreditDao(),
//            nameDetailId
//        )
//
//    private suspend fun getAppearancesByStoryId(storyId: Int): List<Appearance>? =
//        getItemsByArgument(storyId, webservice::getAppearancesByStoryId)
//
//    private suspend fun getCreditsByStoryId(storyId: Int): List<Credit>? =
//        getItemsByArgument(storyId, webservice::getCreditsByStoryId)
//
//    private suspend fun getExCreditsByStoryId(storyId: Int): List<ExCredit>? =
//        getItemsByArgument(storyId, webservice::getExCreditsByStoryId)
//
//    private suspend fun getStoriesByIssueId(issueId: Int): List<Story>? =
//        getItemsByArgument(issueId, webservice::getStoriesByIssue)
//
//    private suspend fun getIssuesBySeriesId(seriesId: Int): List<Issue>? =
//        getItemsByArgument(seriesId, webservice::getIssuesBySeries)
//
//    private suspend fun getAppearancesByCharacterId(characterId: Int): List<Appearance>? =
//        getItemsByArgument(characterId, webservice::getAppearances)
//
//    private suspend fun getCreditsByNameDetailId(nameDetailId: Int): List<Credit>? =
//        getItemsByArgument(listOf(nameDetailId), webservice::getCreditsByNameDetail)
//
//    private suspend fun getExCreditsByNameDetailId(nameDetailId: Int): List<ExCredit>? =
//        getItemsByArgument(listOf(nameDetailId), webservice::getExCreditsByNameDetail)
//
//    /**
//     *  UpdateAsync - Updates publisher, series, role, and storytype tables
//     */
//    @ExperimentalCoroutinesApi
//    internal suspend fun updateAsync() {
//        database.transactionDao().upsertStatic(
//            publishers = getPublishers(),
//            roles = getRoles(),
//            storyTypes = getStoryTypes(),
//            bondTypes = getBondTypes(),
//        )
//
//        getAllSeries()
//        getAllCreators()
//        getAllNameDetails()
//        getAllCharacters()
//        getAllSeriesBonds()
//    }
//
//    private suspend fun getAllSeriesBonds() {
//        refreshAll<SeriesBond>(
//            prefs = prefs,
//            saveTag = UPDATED_BONDS,
//            getItems = this::getSeriesBonds,
//            followup = this::checkFKeysSeriesBond,
//            dao = database.seriesBondDao()
//        )
//    }
//
//    private suspend fun getAllCharacters() {
//        refreshAllPaged<Character>(
//            prefs = prefs,
//            savePageTag = UPDATED_CHARACTERS_PAGE,
//            saveTag = UPDATED_CHARACTERS,
//            getItemsByPage = this::getCharactersByPage,
//            dao = database.characterDao()
//        )
//    }
//
//    private suspend fun getAllNameDetails() {
//        refreshAllPaged<NameDetail>(
//            prefs = prefs,
//            savePageTag = UPDATED_NAME_DETAILS_PAGE,
//            saveTag = UPDATED_NAME_DETAILS,
//            getItemsByPage = this::getNameDetailsByPage,
//            verifyForeignKeys = this::checkFKeysNameDetail,
//            dao = database.nameDetailDao()
//        )
//    }
//
//    private suspend fun getAllCreators() {
//        refreshAllPaged<Creator>(
//            prefs = prefs,
//            savePageTag = UPDATED_CREATORS_PAGE,
//            saveTag = UPDATED_CREATORS,
//            getItemsByPage = this::getCreatorsByPage,
//            dao = database.creatorDao()
//        )
//    }
//
//    private suspend fun getAllSeries() {
//        refreshAllPaged<Series>(
//            prefs = prefs,
//            savePageTag = UPDATED_SERIES_PAGE,
//            saveTag = UPDATED_SERIES,
//            getItemsByPage = this::getSeriesByPage,
//            verifyForeignKeys = this::checkFKeysSeries,
//            dao = database.seriesDao()
//        )
//    }
//
//    private suspend fun getPublishers(): List<Publisher>? =
//        getItems(prefs, webservice::getPublishers, UPDATED_PUBLISHERS)
//
//    private suspend fun getRoles(): List<Role>? =
//        getItems(prefs, webservice::getRoles, UPDATED_ROLES)
//
//    private suspend fun getStoryTypes(): List<StoryType>? =
//        getItems(prefs, webservice::getStoryTypes, UPDATED_STORY_TYPES)
//
//    private suspend fun getBondTypes(): List<BondType>? =
//        getItems(prefs, webservice::getBondTypes, UPDATED_BOND_TYPE)
//
//    private suspend fun getSeriesBonds(): List<SeriesBond>? =
//        getItems(prefs, webservice::getSeriesBonds, UPDATED_BONDS)
//
//    private suspend fun getCharactersByPage(page: Int): List<Character>? =
//        getItemsByArgument(page, webservice::getCharactersByPage)
//
//    private suspend fun getSeriesByPage(page: Int): List<Series>? =
//        getItemsByArgument(page, webservice::getSeriesByPage)
//
//    private suspend fun getCreatorsByPage(page: Int): List<Creator>? =
//        getItemsByArgument(page, webservice::getCreatorsByPage)
//
//    private suspend fun getNameDetailsByPage(page: Int): List<NameDetail>? =
//        getItemsByArgument(page, webservice::getNameDetailsByPage)
//
//    private fun updateIssueCover(issueId: Int) {
//        if (checkIfStale(ISSUE_TAG(issueId), ISSUE_LIFETIME, prefs)) {
//            CoroutineScope(Dispatchers.IO).launch {
//                database.issueDao().getIssueSus(issueId)?.let { issue ->
//                    if (issue.coverUri == null) {
//                        kotlin.runCatching {
//                            val doc = Jsoup.connect(issue.issue.url).get()
//
//                            val noCover = doc.getElementsByClass("no_cover").size == 1
//
//                            val coverImgElements = doc.getElementsByClass("cover_img")
//                            val wraparoundElements =
//                                doc.getElementsByClass("wraparound_cover_img")
//
//                            val elements = when {
//                                coverImgElements.size > 0   -> coverImgElements
//                                wraparoundElements.size > 0 -> wraparoundElements
//                                else                        -> null
//                            }
//
//                            val src = elements?.get(0)?.attr("src")
//
//                            val url = src?.let { URL(it) }
//
//                            if (!noCover && url != null) {
//                                val image = CoroutineScope(Dispatchers.IO).async {
//                                    url.toBitmap()
//                                }
//
//                                CoroutineScope(Dispatchers.Default).launch {
//                                    val bitmap = image.await()
//
//                                    bitmap?.let {
//                                        val savedUri: Uri? =
//                                            it.saveToInternalStorage(
//                                                context!!,
//                                                issue.issue.coverFileName
//                                            )
//
//                                        val cover =
//                                            Cover(issue = issueId, coverUri = savedUri)
//                                        database.coverDao().upsertSus(listOf(cover))
//                                    }
//                                }
//                            } else if (noCover) {
//                                val cover = Cover(issue = issueId, coverUri = null)
//                                database.coverDao().upsertSus(cover)
//                            } else {
//                                Log.d(TAG, "COVER UPDATER No Cover Found")
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    companion object {
//        internal const val TAG = APP + "UpdateStatic"
//    }
//}
//
