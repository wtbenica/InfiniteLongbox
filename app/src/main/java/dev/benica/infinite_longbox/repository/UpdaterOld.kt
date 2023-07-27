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
//import android.util.Log
//import dev.benica.infinite_longbox.APP
//import dev.benica.infinite_longbox.Webservice
//import dev.benica.infinite_longbox.database.IssueDatabase
//import dev.benica.infinite_longbox.database.daos.BaseDao
//import dev.benica.infinite_longbox.database.models.*
//import kotlinx.coroutines.*
//import retrofit2.HttpException
//import java.lang.Integer.min
//import java.net.ConnectException
//import java.net.SocketTimeoutException
//import java.time.LocalDate
//import kotlin.reflect.KSuspendFunction0
//import kotlin.reflect.KSuspendFunction1
//
///**
// * UpdaterOld
// *
// * @constructor Create empty Updater
// */
//@ExperimentalCoroutinesApi
//abstract class UpdaterOld(
//    val webservice: Webservice,
//    val database: IssueDatabase,
//    val prefs: SharedPreferences,
//) {
//
//    internal suspend fun<T: DataModel> checkFKeys(
//        models: List<T>,
//        func: KSuspendFunction1<List<T>, Unit>,
//    ) {
//        var i = 0
//        val listSize = 500
//        do {
//            func(models.subList(i * listSize, min((i + 1) * listSize, models.size)))
//        } while (++i * listSize < models.size)
//    }
//
//    internal suspend fun checkFKeysStory(stories: List<Story>) {
//        checkFKeys(stories, this::checkStoryFkIssue)
//    }
//
//    internal suspend fun checkFKeysSeries(series: List<Series>) {
//        checkFKeys(series, this::checkSeriesFkPublisher)
//    }
//
//    internal suspend fun checkFKeysNameDetail(nameDetails: List<NameDetail>) {
//        checkFKeys(nameDetails, this::checkNameDetailFkCreator)
//    }
//
//    internal suspend fun checkFKeysIssue(issues: List<Issue>) {
//        checkFKeys(issues, this::checkIssueFkSeries)
//        checkFKeys(issues, this::checkIssueFkVariantOf)
//    }
//
//    internal suspend fun checkFKeysSeriesBond(bonds: List<SeriesBond>) {
//        checkSeriesBondFkOriginIssue(bonds)
//        checkSeriesBondFkOriginSeries(bonds)
//        checkSeriesBondFkTargetIssue(bonds)
//        checkSeriesBondFkTargetSeries(bonds)
//    }
//
//    internal suspend fun checkFKeysAppearance(appearances: List<Appearance>) {
//        checkAppearanceFkCharacter(appearances)
//        checkAppearanceFkStory(appearances)
//    }
//
//    internal suspend fun <T : CreditX> checkFKeysCredit(credits: List<T>) {
//        checkFKeys(credits, this::checkCreditFkNameDetail)
//        checkFKeys(credits, this::checkCreditFkStory)
//    }
//
//    internal suspend fun checkSeriesFkPublisher(series: List<Series>) =
//        checkForMissingForeignKeyModels(series,
//            Series::publisher,
//            database.publisherDao(),
//            webservice::getPublishersByIds)
//
//    internal suspend fun checkNameDetailFkCreator(nameDetails: List<NameDetail>) =
//        checkForMissingForeignKeyModels(nameDetails,
//            NameDetail::creator,
//            database.creatorDao(),
//            webservice::getCreatorsByIds)
//
//
//    internal suspend fun checkIssueFkSeries(issues: List<Issue>) =
//        checkForMissingForeignKeyModels(issues,
//            Issue::series,
//            database.seriesDao(),
//            webservice::getSeriesByIds,
//            this@UpdaterOld::checkFKeysSeries)
//
//    internal suspend fun checkIssueFkVariantOf(issues: List<Issue>) =
//        checkForMissingForeignKeyModels(issues,
//            Issue::variantOf,
//            database.issueDao(),
//            webservice::getIssuesByIds,
//            this@UpdaterOld::checkFKeysIssue)
//
//    internal suspend fun checkSeriesBondFkOriginIssue(seriesBonds: List<SeriesBond>) =
//        checkForMissingForeignKeyModels(seriesBonds,
//            SeriesBond::originIssue,
//            database.issueDao(),
//            webservice::getIssuesByIds,
//            this@UpdaterOld::checkFKeysIssue)
//
//    internal suspend fun checkSeriesBondFkOriginSeries(seriesBonds: List<SeriesBond>) =
//        checkForMissingForeignKeyModels(seriesBonds,
//            SeriesBond::origin,
//            database.seriesDao(),
//            webservice::getSeriesByIds,
//            this@UpdaterOld::checkFKeysSeries)
//
//    internal suspend fun checkSeriesBondFkTargetSeries(seriesBonds: List<SeriesBond>) =
//        checkForMissingForeignKeyModels(seriesBonds,
//            SeriesBond::target,
//            database.seriesDao(),
//            webservice::getSeriesByIds,
//            this@UpdaterOld::checkFKeysSeries)
//
//    internal suspend fun checkSeriesBondFkTargetIssue(seriesBonds: List<SeriesBond>) =
//        checkForMissingForeignKeyModels(seriesBonds,
//            SeriesBond::targetIssue,
//            database.issueDao(),
//            webservice::getIssuesByIds,
//            this@UpdaterOld::checkFKeysIssue)
//
//
//    internal suspend fun checkStoryFkIssue(stories: List<Story>) =
//        checkForMissingForeignKeyModels(stories,
//            Story::issue,
//            database.issueDao(),
//            webservice::getIssuesByIds,
//            this@UpdaterOld::checkFKeysIssue)
//
//    internal suspend fun checkAppearanceFkCharacter(appearances: List<Appearance>) =
//        checkForMissingForeignKeyModels(appearances,
//            Appearance::character,
//            database.characterDao(),
//            webservice::getCharactersByIds)
//
//    internal suspend fun checkAppearanceFkStory(appearances: List<Appearance>) =
//        checkForMissingForeignKeyModels(appearances,
//            Appearance::story,
//            database.storyDao(),
//            webservice::getStoriesByIds,
//            this@UpdaterOld::checkFKeysStory)
//
//    internal suspend fun <T : CreditX> checkCreditFkNameDetail(credits: List<T>) =
//        checkForMissingForeignKeyModels(credits,
//            CreditX::nameDetail,
//            database.nameDetailDao(),
//            webservice::getNameDetailsByIds,
//            this@UpdaterOld::checkFKeysNameDetail)
//
//    internal suspend fun <T : CreditX> checkCreditFkStory(credits: List<T>) =
//        checkForMissingForeignKeyModels(credits,
//            CreditX::story,
//            database.storyDao(),
//            webservice::getStoriesByIds,
//            this@UpdaterOld::checkFKeysStory)
//
//    @ExperimentalCoroutinesApi
//    companion object {
//        private const val TAG = APP + "Updater"
//
//        /**
//         * Run safely - runs queryFunction and awaits results. Logs SocketTimeout-, Connect-, and
//         * Http- exceptions. Also avoids making remote calls with empty list.
//         * @param name Function name, shown  in exception log message
//         * @return null on one of the listed exceptions or if it was called with an empty arg list
//         */
//        suspend fun <ResultType : Any, ArgType : Any> runSafely(
//            name: String,
//            arg: ArgType,
//            queryFunction: (ArgType) -> Deferred<ResultType>,
//        ): ResultType? {
//
//            return try {
//                if (arg !is List<*> || arg.isNotEmpty()) {
//                    queryFunction(arg).await()
//                } else {
//                    null
//                }
//            } catch (e: SocketTimeoutException) {
//                Log.d(TAG, "$name $e")
//                null
//            } catch (e: ConnectException) {
//                Log.d(TAG, "$name $e")
//                null
//            } catch (e: HttpException) {
//                Log.d(TAG, "$name $e")
//                null
//            }
//        }
//
//        /**
//         * Run safely
//         *
//         *  runs queryFunction and awaits results. Logs SocketTimeout-, Connect-, and
//         * Http- exceptions. Also avoids making remote calls with empty list.
//         * @param name Function name, shown  in exception log message
//         * @return null on one of the listed exceptions or if it was called with an empty arg list
//         */
//        suspend fun <ResultType : Any> runSafely(
//            name: String,
//            queryFunction: () -> Deferred<ResultType>,
//        ): ResultType? {
//
//            return try {
//                queryFunction().await()
//            } catch (e: SocketTimeoutException) {
//                Log.d(TAG, "$name $e")
//                null
//            } catch (e: ConnectException) {
//                Log.d(TAG, "$name $e")
//                null
//            } catch (e: HttpException) {
//                Log.d(TAG, "$name $e")
//                null
//            }
//        }
//
//        /**
//         * Check if stale
//         */
//        // TODO: This should probably get moved out of SharedPreferences and stored with each record.
//        //  The tradeoff: an extra local db query vs. having a larger prefs which will end up having
//        //  a value for every item in the database.
//        internal fun checkIfStale(
//            prefsKey: String,
//            shelfLife: Long,
//            prefs: SharedPreferences,
//        ): Boolean {
//            val lastUpdated = LocalDate.parse(prefs.getString(prefsKey, "${LocalDate.MIN}"))
//            val isStale = LocalDate.now() > lastUpdated.plusDays(shelfLife)
//            return DEBUG || isStale
//        }
//
//        /**
//         * Get items by argument
//         * @return null on connection error
//         */
//        internal suspend fun <GcdType : GcdJson<ModelType>, ModelType : DataModel, ArgType : Any>
//                getItemsByArgument(
//            arg: ArgType,
//            apiCall: KSuspendFunction1<ArgType, List<Item<GcdType, ModelType>>>,
//        ): List<ModelType>? =
//            supervisorScope {
//                runSafely("getItemsByArgument: ${apiCall.name}", arg) {
//                    async { apiCall(it) }
//                }?.models
//            }
//
//        /**
//         * Refresh all - gets items, performs followup, then saves using dao
//         */
//        internal suspend fun <ModelType : DataModel> updateById(
//            prefs: SharedPreferences,
//            saveTag: String?,
//            getItems: suspend (Int) -> List<ModelType>?,
//            followup: suspend (List<ModelType>) -> Unit = {},
//            dao: BaseDao<ModelType>,
//            id: Int,
//        ): List<ModelType> {
//            return if (saveTag?.let { checkIfStale(it, WEEKLY, prefs) } != false) {
//                coroutineScope {
//                    val items: List<ModelType>? = getItems(id)
//
//                    if (items != null && items.isNotEmpty()) {
//                        followup(items)
//                        dao.upsertSus(items)
//                        saveTag?.let { Repository.saveTime(prefs, it) }
//                    }
//                    return@coroutineScope items ?: emptyList()
//                }
//            } else {
//                emptyList()
//            }
//        }
//
//        /**
//         * Get items - checks if stale and retrieves items
//         * @return null on connection error
//         */
//        internal suspend fun <GcdType : GcdJson<ModelType>, ModelType : DataModel> getItems(
//            prefs: SharedPreferences,
//            apiCall: KSuspendFunction0<List<Item<GcdType, ModelType>>>,
//            saveTag: String,
//        ): List<ModelType>? =
//            supervisorScope {
//                if (checkIfStale(saveTag, WEEKLY, prefs)) {
//                    runSafely("getItems: ${apiCall.name}") {
//                        async { apiCall() }
//                    }?.models
//                } else {
//                    emptyList()
//                }
//            }
//
//        /**
//         * Get all paged - calls getItemsByPage , performs verifyForeignKeys on each page, then
//         * saves each page to dao
//         */
//        internal suspend fun <ModelType : DataModel> refreshAllPaged(
//            prefs: SharedPreferences,
//            savePageTag: String,
//            saveTag: String,
//            getItemsByPage: suspend (Int) -> List<ModelType>?,
//            verifyForeignKeys: suspend (List<ModelType>) -> Unit = {},
//            dao: BaseDao<ModelType>,
//        ) {
//            if (checkIfStale(saveTag, WEEKLY, prefs)) {
//                var page = prefs.getInt(savePageTag, 0)
//                var stop = false
//                var success = true
//
//                do {
//                    coroutineScope {
//                        val itemPage: List<ModelType>? = getItemsByPage(page)
//
//                        if (itemPage != null && itemPage.isNotEmpty()) {
//                            verifyForeignKeys(itemPage)
//                            dao.upsertSus(itemPage)
//                            Repository.savePrefValue(prefs, savePageTag, page)
//                        } else if (itemPage != null) {
//                            stop = true
//                        } else {
//                            stop = true
//                            success = false
//                        }
//                    }
//
//                    page += 1
//                } while (!stop)
//
//                if (success) {
//                    Repository.savePrefValue(prefs, savePageTag, 0)
//                    Repository.saveTime(prefs, saveTag)
//                }
//            }
//        }
//
//        /**
//         * Refresh all - gets items, performs followup, then saves using dao
//         */
//        internal suspend fun <ModelType : DataModel> refreshAll(
//            prefs: SharedPreferences,
//            saveTag: String,
//            getItems: suspend () -> List<ModelType>?,
//            followup: suspend (List<ModelType>) -> Unit = {},
//            dao: BaseDao<ModelType>,
//        ) {
//            if (checkIfStale(saveTag, WEEKLY, prefs)) {
//                coroutineScope {
//                    val items: List<ModelType>? = getItems()
//
//                    if (items != null && items.isNotEmpty()) {
//                        followup(items)
//                        dao.upsertSus(items)
//                        Repository.saveTime(prefs, saveTag)
//                    }
//                }
//            }
//        }
//
//        /**
//         * Get missing foreign key models
//         */
//        internal suspend fun <M : DataModel, FG : GcdJson<FM>, FM : DataModel>
//                checkForMissingForeignKeyModels(
//            itemsToCheck: List<M>,
//            getForeignKey: (M) -> Int?,
//            foreignKeyDao: BaseDao<FM>,
//            getFkItems: KSuspendFunction1<List<Int>, List<Item<FG, FM>>>,
//            fkFollowup: (suspend (List<FM>) -> Unit)? = null,
//        ) {
//            val fkIds: List<Int> = itemsToCheck.mapNotNull { getForeignKey(it) }
//            val missingIds: List<Int> = fkIds.mapNotNull {
//                if (foreignKeyDao.get(it) == null) it else null
//            }
//
//            val missingItems = getItemsByArgument(missingIds, getFkItems)
//
//            if (missingItems != null && missingItems.isNotEmpty()) {
//                fkFollowup?.let { it(missingItems) }
//                foreignKeyDao.upsert(missingItems)
//            }
//        }
//    }
//}
//
