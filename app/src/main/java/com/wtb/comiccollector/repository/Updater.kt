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

package com.wtb.comiccollector.repository

import android.content.SharedPreferences
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.util.Log
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.ComicCollectorApplication.Companion.context
import com.wtb.comiccollector.Webservice
import com.wtb.comiccollector.database.IssueDatabase
import com.wtb.comiccollector.database.daos.BaseDao
import com.wtb.comiccollector.database.daos.Count
import com.wtb.comiccollector.database.models.*
import com.wtb.comiccollector.views.ProgressUpdateCard.ProgressWrapper
import kotlinx.coroutines.*
import kotlinx.coroutines.android.asCoroutineDispatcher
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.time.Instant
import java.time.LocalDate
import kotlin.reflect.KSuspendFunction0
import kotlin.reflect.KSuspendFunction1

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

    init {
        Collector.initialize(database)
    }

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
                    queryFunction(arg).await()
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
                // do i need to check for others?
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
                    delay(5000)
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
                queryFunction().await()
            } catch (e: SocketTimeoutException) {
                Log.d(TAG, "$name $e")
                null
            } catch (e: ConnectException) {
                Log.d(TAG, "$name $e")
                null
            } catch (e: HttpException) {
                Log.d(TAG, "$name $e")
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
            apiCall: KSuspendFunction1<ArgType, List<Item<GcdType, ModelType>>>,
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
            apiCall: KSuspendFunction1<List<ArgType>, List<Item<GcdType, ModelType>>>,
        ): List<ModelType> {
            val lists = argList.chunked(20)
            val res = mutableListOf<ModelType>()
            for (elem in lists) {
                retrieveItemsByArgument(elem, apiCall)?.let { res.addAll(it) }
            }
            return res
        }

        /**
         * Gets items, performs followup, then saves using dao
         * usually: [getItems] for [id], checks foreign keys for [followup], saves items to
         * [collector]
         * then saves update time to [saveTag] in [prefs]
         */
        internal suspend fun <ModelType : DataModel, T : Any> updateById(
            prefs: SharedPreferences,
            saveTag: ((Int) -> String)?,
            getItems: suspend (T) -> List<ModelType>?,
            id: T,
            followup: suspend (List<ModelType>) -> Unit = {},
            collector: Collector<ModelType>,
        ): List<ModelType> {
            return coroutineScope {
                val items: List<ModelType>? = getItems(id)

                if (items != null && items.isNotEmpty()) {
                    async {
                        followup(items)
                    }.await().let {
                        collector.dao.upsertSus(items)
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
        apiCall: KSuspendFunction0<List<Item<GcdType, ModelType>>>,
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
     * @param progressWrapper ProgressWrapper to update progress bar
     */
    internal suspend fun <ModelType : DataModel> refreshAllPaged(
        prefs: SharedPreferences,
        savePageTag: String,
        saveTag: String,
        getItemsByPage: suspend (Int) -> List<ModelType>?,
        verifyForeignKeys: suspend (List<ModelType>) -> Unit = {},
        dao: BaseDao<ModelType>,
        getNumPages: suspend () -> Count,
        progressWrapper: ProgressWrapper? = null
    ) {
        // savePageTag's value is a string of whether each page has been updated, e.g. "tfffttttftt"
        val pagesCompleteSaved: String = prefs.getString(savePageTag, "") ?: ""

        if (!pagesCompleteSaved.contains('f')) {
            // If it's been a week, mark all pages as stale
            if (checkIfStale(saveTag, WEEKLY, prefs)) {
                progressWrapper?.isComplete(false)
                Repository.savePrefValue(prefs, savePageTag, "")
            } else {
                progressWrapper?.setProgress(100)
            }
        }

        // make an array for whether each page has been updated
        val numPages: Int = getNumPages().count
        val pagesComplete = BooleanArray(numPages)
        progressWrapper?.setMax(numPages)

        if (numPages == pagesCompleteSaved.length) {
            pagesCompleteSaved.forEachIndexed { index: Int, c: Char ->
                pagesComplete[index] = (c == 't')
            }
        }

        var numComplete = pagesComplete.count { it }
        progressWrapper?.setProgress(numComplete)

        /**
         * Saves boolean array to prefs as a string. 't' for true, 'f' for false
         */
        fun BooleanArray.saveToPrefs(tag: String) {
            val tsAndFs = java.lang.StringBuilder()
            forEach { tsAndFs.append(if (it) 't' else 'f') }
            Repository.savePrefValue(prefs, tag, tsAndFs.toString())
        }

        /**
         * Increments numComplete, saves to prefs, and increments progress bar
         */
        fun pageFinished() {
            synchronized(this) {
                numComplete++
                pagesComplete.saveToPrefs(savePageTag)
                progressWrapper?.incrementProgress()
            }
        }

        // for each page in PC, if not complete, then try to update again
        // if request is successful, save and mark as updated
        withContext(CoroutineScope(Dispatchers.IO).coroutineContext) {
            pagesComplete.forEachIndexed { currPage: Int, isComplete: Boolean ->
                if (!isComplete) {
                    val itemPage: List<ModelType>? = getItemsByPage(currPage)

                    // if request is successful, save and mark as updated
                    if (itemPage != null && itemPage.isNotEmpty()) {
                        async {
                            verifyForeignKeys(itemPage)
                        }.await().let {
                            if (dao.upsertSus(itemPage)) {
                                pagesComplete[currPage] = true
                                pageFinished()
                            }
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
        }
    }


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

                if (items != null && items.isNotEmpty()) {
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
            @Volatile
            private var NOW_INSTANCE: CoroutineDispatcher? = null

            @Volatile
            private var HP_INSTANCE: CoroutineDispatcher? = null

            @Volatile
            private var LP_INSTANCE: CoroutineDispatcher? = null

            val nowDispatcher: CoroutineDispatcher
                get() {
                    synchronized(this) {
                        val thread = HandlerThread(
                            "nowThread",
                            Process.THREAD_PRIORITY_DISPLAY
                        ).also {
                            it.start()
                        }
                        val res = NOW_INSTANCE
                            ?: Handler(thread.looper).asCoroutineDispatcher("nowDispatcher")
                        if (NOW_INSTANCE == null) {
                            NOW_INSTANCE = res
                        }
                        return NOW_INSTANCE!!
                    }
                }

            val highPriorityDispatcher: CoroutineDispatcher
                get() {
                    synchronized(this)
                    {
                        val thread = HandlerThread(
                            "highPriorityThread",
                            Process.THREAD_PRIORITY_MORE_FAVORABLE
                        ).also {
                            it.start()
                        }
                        val res = HP_INSTANCE
                            ?: Handler(thread.looper).asCoroutineDispatcher("highPriorityDispatcher")
                        if (HP_INSTANCE == null) {
                            HP_INSTANCE = res
                        }
                        return HP_INSTANCE!!
                    }
                }

            val lowPriorityDispatcher: CoroutineDispatcher
                get() {
                    synchronized(this) {
                        val thread = HandlerThread(
                            "lowPriorityThread",
                            Process.THREAD_PRIORITY_BACKGROUND
                        ).also {
                            it.start()
                        }
                        val res = LP_INSTANCE
                            ?: Handler(thread.looper).asCoroutineDispatcher("lowPriorityDispatcher")
                        if (LP_INSTANCE == null) {
                            LP_INSTANCE = res
                        }
                        return LP_INSTANCE!!
                    }
                }
        }
    }
}


