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

import android.content.SharedPreferences
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.util.Log
import dev.benica.infinite_longbox.APP
import dev.benica.infinite_longbox.InfiniteLongboxApplication.Companion.context
import dev.benica.infinite_longbox.Webservice
import dev.benica.infinite_longbox.database.IssueDatabase
import dev.benica.infinite_longbox.database.daos.BaseDao
import dev.benica.infinite_longbox.database.daos.Count
import dev.benica.infinite_longbox.database.models.*
import dev.benica.infinite_longbox.ui.update.UpdateProgress.ProgressWrapper
import kotlinx.coroutines.*
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.time.Instant
import java.time.LocalDate
import kotlin.reflect.KSuspendFunction0
import kotlin.reflect.KSuspendFunction1
import kotlin.time.Duration.Companion.milliseconds

val BooleanArray.allTrue
    get() = this.all { it }

/**
 * Abstract class for updating local database from remote database. Contains helper functions
 * for updating and retrieving data. Also contains helper functions for updating progress bars.
 *
 * @param webservice
 * @param prefs
 */
@ExperimentalCoroutinesApi
abstract class Updater(
    val webservice: Webservice,
    val prefs: SharedPreferences,
) {
    protected val database: IssueDatabase
        get() = IssueDatabase.getInstance(context!!)

    val fKeyChecker
        get() = FKeyChecker(database, webservice)

    @ExperimentalCoroutinesApi
    companion object {
        private const val TAG = APP + "Updater"

        /**
         * Run safely - runs queryFunction and awaits results. Logs SocketTimeout-, Connect-, and
         * Http- exceptions. Also avoids making remote calls with empty list.
         * @param name Function name, shown  in exception log message
         * @return null on one of the listed exceptions or if it was called with an empty arg list
         */
        private suspend fun <ResultType : Any, ArgType : Any> runSafely(
            name: String,
            arg: ArgType,
            queryFunction: (ArgType) -> Deferred<ResultType>,
            doRetry: Boolean = false,
        ): ResultType? {

            return try {
                if (arg !is List<*> || arg.isNotEmpty()) {
                    LoadTracker.track { queryFunction(arg).await() }
                } else {
                    null
                }
            } catch (e: SocketTimeoutException) {
                Log.d(TAG, "$name $e")
                if (doRetry)
                    retrySafely(name, arg, queryFunction)
                null
            } catch (e: ConnectException) {
                if (doRetry)
                    retrySafely(name, arg, queryFunction)
                Log.d(TAG, "$name $e")
                null
            } catch (e: HttpException) {
                Log.d(TAG, "$name $e")
                null
            } catch (e: IOException) {
                // Any other dropped or refused connection, e.g. the server closed it mid-response
                if (doRetry)
                    retrySafely(name, arg, queryFunction)
                Log.d(TAG, "$name $e")
                null
            }
        }

        private suspend fun <ArgType : Any, ResultType : Any> retrySafely(
            name: String,
            arg: ArgType,
            queryFunction: (ArgType) -> Deferred<ResultType>,
        ) {
            withContext(Dispatchers.IO) {
                launch {
                    delay(5000.milliseconds)
                    runSafely(name, arg, queryFunction, true)
                }
            }
        }

        /**
         * Run safely
         *
         *  runs queryFunction and awaits results. Logs SocketTimeout-, Connect-, and
         * Http- exceptions. Also avoids making remote calls with empty list.
         * @param name Function name, shown  in exception log message
         * @return null on one of the listed exceptions or if it was called with an empty arg list
         */
        suspend fun <ResultType : Any> runSafely(
            name: String,
            queryFunction: () -> Deferred<ResultType>,
        ): ResultType? {

            return try {
                LoadTracker.track { queryFunction().await() }
            } catch (e: SocketTimeoutException) {
                Log.d(TAG, "$name $e")
                null
            } catch (e: ConnectException) {
                Log.d(TAG, "$name $e")
                null
            } catch (e: HttpException) {
                Log.d(TAG, "$name $e")
                null
            } catch (e: IOException) {
                Log.d(TAG, "$name $e")
                null
            } catch (e: kotlinx.serialization.SerializationException) {
                Log.w(TAG, "$name: the server's answer was not what was expected: $e")
                null
            }
        }

        /**
         * @return true if DEBUG is true or if item needs to be updated and it hasn't already
         * been started or it's been more than 3 secs
         */
        // TODO: This should probably get moved out of SharedPreferences and stored with each record.
        //  The tradeoff: an extra local db query vs. having a larger prefs which will end up having
        //  a value for every item in the database.
        internal fun checkIfStale(
            prefsKey: String,
            shelfLife: Long,
            prefs: SharedPreferences,
        ): Boolean {
            val lastUpdated = LocalDate.parse(prefs.getString(prefsKey, "${LocalDate.MIN}"))
            val isStale = LocalDate.now() > lastUpdated.plusDays(shelfLife)
            val isStarted = prefs.getBoolean("${prefsKey}_STARTED", false)
            val timeStarted: Long = try {
                prefs.getLong("${prefsKey}_STARTED_TIME", Instant.MIN.epochSecond)
            } catch (e: ClassCastException) {
                Instant.MIN.epochSecond
            }
            val isExpired = timeStarted + 3 < Instant.now().epochSecond

            return DEBUG || (isStale && (!isStarted || isExpired))
        }

        /**
         * wrapper function to safely retrieve models from webservice
         * @return always returns a list (empty or non-empty). Does not report connection errors
         */
        internal suspend fun <GcdType : GcdJson<ModelType>, ModelType : DataModel, ArgType : Any>
                retrieveItemsByArgument(
            arg: ArgType,
            apiCall: KSuspendFunction1<ArgType, List<Item<GcdType>>>,
        ): List<ModelType>? =
            supervisorScope {
                runSafely(
                    name = "getItemsByArgument: ${apiCall.name}",
                    arg = arg,
                    queryFunction = {
                        async { apiCall(it) }
                    })?.models
            }

        /**
         * Get items by list
         * @return always returns a list (empty or non-empty). Does not report connection errors
         */
        suspend fun <GcdType : GcdJson<ModelType>, ModelType : DataModel, ArgType : Any>
                retrieveItemsByList(
            argList: List<ArgType>,
            apiCall: KSuspendFunction1<List<ArgType>, List<Item<GcdType>>>,
        ): List<ModelType> {
            // Fewer, bigger requests, a few at a time: each request costs a round trip, which
            // is what dominates on a phone, while the server answers in milliseconds.
            val gate = Semaphore(PARALLEL_REQUESTS)
            val res = coroutineScope {
                argList.chunked(LIST_CHUNK_SIZE).map { elem ->
                    async { gate.withPermit { retrieveItemsByArgument(elem, apiCall) } }
                }.awaitAll()
            }.filterNotNull().flatten()
            return res
        }

        private const val LIST_CHUNK_SIZE = 200
        private const val PARALLEL_REQUESTS = 4

        /**
         * Gets items, performs followup, then saves using dao
         * usually: [getItems] for [id], checks foreign keys for [followup], saves items to
         * [dao]
         * then saves update time to [saveTag] in [prefs]
         */
        internal suspend fun <ModelType : DataModel, T : Any> updateById(
            prefs: SharedPreferences,
            saveTag: ((Int) -> String)?,
            getItems: suspend (T) -> List<ModelType>?,
            id: T,
            followup: suspend (List<ModelType>) -> Unit = {},
            dao: BaseDao<ModelType>,
        ): List<ModelType> {
            return coroutineScope {
                val items: List<ModelType>? = getItems(id)

                if (!items.isNullOrEmpty()) {
                    async {
                        followup(items)
                    }.await().let {
                        dao.upsertSus(items)
                    }
                }
                return@coroutineScope items ?: emptyList()
            }
        }
    }

    /**
     * Get items - checks if stale and retrieves items
     * @return null on connection error
     */
    internal suspend fun <GcdType : GcdJson<ModelType>, ModelType : DataModel> getItems(
        prefs: SharedPreferences,
        apiCall: KSuspendFunction0<List<Item<GcdType>>>,
        saveTag: String,
    ): List<ModelType>? =
        supervisorScope {
            if (checkIfStale(saveTag, WEEKLY, prefs)) {
                runSafely("getItems: ${apiCall.name}") {
                    async { apiCall() }
                }?.models
            } else {
                emptyList()
            }
        }

    /**
     * Checks if stale, then retrieves items by page, verifies foreign keys and retrieves missing,
     * then saves to dao. Also updates progress bar.
     *
     * @param prefs SharedPreferences to save progress to
     * @param savePageTag key to use to save page progress
     * @param saveTag key to use to save last update time
     * @param getItemsByPage Function to retrieve items by page
     * @param verifyForeignKeys Function to verify foreign keys
     * @param dao Dao to save items to
     * @param getNumPages Function to retrieve number of pages
     * @param progressWrapper says which part this is and how far along it is
     */
    internal suspend fun <ModelType : DataModel> refreshAllPaged(
        prefs: SharedPreferences,
        savePageTag: String,
        saveTag: String,
        getItemsByPage: suspend (Int) -> List<ModelType>?,
        verifyForeignKeys: suspend (List<ModelType>) -> Deferred<Unit>? = { null },
        dao: BaseDao<ModelType>,
        getNumPages: suspend () -> Count,
        progressWrapper: ProgressWrapper? = null,
        manifestKey: String? = null,
    ) {
        progressWrapper?.start()

        // savePageTag's value is a string of whether each page has been updated, e.g. "tfffttttftt";
        // hashTag's is the version (fingerprint) of each page the phone has
        val hashTag = "${savePageTag}_hashes"
        var pagesCompleteSaved: String = prefs.getString(savePageTag, "") ?: ""
        var manifest: List<String>? = null

        if (!pagesCompleteSaved.contains('f') && checkIfStale(saveTag, WEEKLY, prefs)) {
            // It's been a week. Ask which pages changed and fetch only those; with no answer, fetch
            // everything again next time, as before.
            manifest = manifestKey?.let { fetchManifest(it) }
            if (manifest != null) {
                pagesCompleteSaved = PageDiff.stalePages(
                    pagesCompleteSaved, PageDiff.parse(prefs.getString(hashTag, "")), manifest
                )
            } else {
                pagesCompleteSaved = ""
            }
            Repository.savePrefValue(prefs, savePageTag, pagesCompleteSaved)
        }

        // make an array for whether each page has been updated
        val numPages: Int = getNumPages().count
        val pagesComplete = BooleanArray(numPages)

        if (numPages == pagesCompleteSaved.length) {
            pagesCompleteSaved.forEachIndexed { index: Int, c: Char ->
                pagesComplete[index] = (c == 't')
            }
        }

        var numComplete = pagesComplete.count { it }
        // What this run has to fetch, and so what its progress is measured against
        val alreadyComplete = numComplete
        val toFetch = numPages - numComplete
        if (toFetch > 0) progressWrapper?.report(0, toFetch)

        // Pages are about to be fetched: say what version each is, to remember with it
        if (manifest == null && manifestKey != null && numComplete < numPages) {
            manifest = fetchManifest(manifestKey)
        }
        val fetchedVersions = java.util.concurrent.ConcurrentHashMap<Int, String>()
        val versions = manifest?.takeIf { it.size == numPages }

        /**
         * Saves boolean array to prefs as a string. 't' for true, 'f' for false
         */
        fun BooleanArray.saveToPrefs(tag: String) {
            val tsAndFs = java.lang.StringBuilder()
            forEach { tsAndFs.append(if (it) 't' else 'f') }
            Repository.savePrefValue(prefs, tag, tsAndFs.toString())
        }

        /**
         * Increments numComplete, saves to prefs, and reports the progress
         */
        fun pageFinished() {
            synchronized(this) {
                numComplete++
                pagesComplete.saveToPrefs(savePageTag)
                progressWrapper?.report(numComplete - alreadyComplete, toFetch)
            }
        }

        // for each page in PC, if not complete, then try to update again
        // if request is successful, save and mark as updated
        withContext(Dispatchers.IO) {
            pagesComplete.forEachIndexed { currPage: Int, isComplete: Boolean ->
                if (!isComplete) {
                    val itemPage: List<ModelType>? = getItemsByPage(currPage)

                    // if request is successful, save and mark as updated
                    if (!itemPage.isNullOrEmpty()) {
                        // The FK check hands back a Deferred; it must be awaited so the
                        // foreign keys exist before the page is saved.
                        verifyForeignKeys(itemPage)?.await()
                        if (dao.upsertSus(itemPage)) {
                            pagesComplete[currPage] = true
                            versions?.let { fetchedVersions[currPage] = it[currPage] }
                            pageFinished()
                        }
                    }
                }
            }
        }.let {
            // if every page has been updated, change saveTag time
            if (pagesComplete.allTrue) {
                Repository.saveTime(prefs, saveTag)
            }

            // write new savePageTag
            val sb = StringBuilder()
            pagesComplete.forEach {
                sb.append(if (it) "t" else "f")
            }

            Repository.savePrefValue(prefs, savePageTag, sb.toString())

            if (versions != null) {
                Repository.savePrefValue(
                    prefs, hashTag,
                    PageDiff.format(PageDiff.remember(PageDiff.parse(prefs.getString(hashTag, "")), numPages, fetchedVersions))
                )
            }
        }

        // Up to date: say so for long enough to be read, rather than flashing past
        if (toFetch == 0) progressWrapper?.hold()
    }

    /** The version of each page of [key]'s model, or null if the server can't say. */
    private suspend fun fetchManifest(key: String): List<String>? =
        runSafely("getManifest") { CoroutineScope(Dispatchers.IO).async { webservice.getManifest() } }
            ?.get(key)?.hashes


    /**
     * Refresh all - gets items, performs followup, then saves using dao
     */
    internal suspend fun <ModelType : DataModel> refreshAll(
        prefs: SharedPreferences,
        saveTag: String?,
        getItems: suspend () -> List<ModelType>?,
        followup: suspend (List<ModelType>) -> Unit = {},
        dao: BaseDao<ModelType>,
    ) {
        if (saveTag?.let { checkIfStale(it, WEEKLY, prefs) } != false) {
            coroutineScope {
                val items: List<ModelType>? = getItems()

                if (!items.isNullOrEmpty()) {
                    followup(items)
                    if (dao.upsertSus(items)) {
                        saveTag?.let { Repository.saveTime(prefs, it) }
                    }
                }
            }
        }
    }

    class PriorityDispatcher {
        companion object {
            // One thread each, started on first use
            private fun dispatcher(name: String, priority: Int): CoroutineDispatcher {
                val thread = HandlerThread(name, priority).also { it.start() }
                return Handler(thread.looper).asCoroutineDispatcher(name)
            }

            val nowDispatcher: CoroutineDispatcher by lazy {
                dispatcher("nowThread", Process.THREAD_PRIORITY_DISPLAY)
            }

            val highPriorityDispatcher: CoroutineDispatcher by lazy {
                dispatcher("highPriorityThread", Process.THREAD_PRIORITY_MORE_FAVORABLE)
            }

            val lowPriorityDispatcher: CoroutineDispatcher by lazy {
                dispatcher("lowPriorityThread", Process.THREAD_PRIORITY_BACKGROUND)
            }
        }
    }
}


