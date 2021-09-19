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
import com.wtb.comiccollector.repository.Updater.PriorityDispatcher.Companion.lowPriorityDispatcher
import kotlinx.coroutines.*
import kotlinx.coroutines.android.asCoroutineDispatcher
import retrofit2.HttpException
import java.lang.Integer.min
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.time.Instant
import java.time.LocalDate
import kotlin.reflect.KSuspendFunction0
import kotlin.reflect.KSuspendFunction1

val BooleanArray.allTrue
    get() = this.all { it }

/**
 * Updater
 *
 * @constructor Create empty Updater
 */
@ExperimentalCoroutinesApi
abstract class Updater(
    val webservice: Webservice,
    val prefs: SharedPreferences,
) {
    protected val database: IssueDatabase
        get() = IssueDatabase.getInstance(context!!)

    private suspend fun <T : DataModel> checkFKeys(
        models: List<T>,
        func: KSuspendFunction1<List<T>, Unit>,
    ) {
        var i = 0
        val listSize = 500
        do {
            func(models.subList(i * listSize, min((i + 1) * listSize, models.size)))
        } while (++i * listSize < models.size)
    }

    internal suspend fun checkFKeysStory(stories: List<Story>) {
        checkFKeys(stories, this::checkStoryFkIssue)
    }

    internal suspend fun checkFKeysSeries(series: List<Series>) {
        checkFKeys(series, this::checkSeriesFkPublisher)
    }

    internal suspend fun checkFKeysNameDetail(nameDetails: List<NameDetail>) {
        checkFKeys(nameDetails, this::checkNameDetailFkCreator)
    }

    internal suspend fun checkFKeysIssue(issues: List<Issue>) {
        checkFKeys(issues, this::checkIssueFkSeries)
        checkFKeys(issues, this::checkIssueFkVariantOf)
    }

    internal suspend fun checkFKeysSeriesBond(bonds: List<SeriesBond>) {
        checkSeriesBondFkOriginIssue(bonds)
        checkSeriesBondFkOriginSeries(bonds)
        checkSeriesBondFkTargetIssue(bonds)
        checkSeriesBondFkTargetSeries(bonds)
    }

    internal suspend fun checkFKeysAppearance(appearances: List<Appearance>) {
        checkAppearanceFkCharacter(appearances)
        checkAppearanceFkStory(appearances)
    }

    internal suspend fun <T : CreditX> checkFKeysCredit(credits: List<T>) {
        checkFKeys(credits, this::checkCreditFkNameDetail)
        checkFKeys(credits, this::checkCreditFkStory)
    }

    private suspend fun checkSeriesFkPublisher(series: List<Series>) =
        checkForMissingForeignKeyModels(series,
                                        Series::publisher,
                                        database.publisherDao(),
                                        webservice::getPublishersByIds)

    private suspend fun checkNameDetailFkCreator(nameDetails: List<NameDetail>) =
        checkForMissingForeignKeyModels(nameDetails,
                                        NameDetail::creator,
                                        database.creatorDao(),
                                        webservice::getCreatorsByIds)


    private suspend fun checkIssueFkSeries(issues: List<Issue>) =
        checkForMissingForeignKeyModels(issues,
                                        Issue::series,
                                        database.seriesDao(),
                                        webservice::getSeriesByIds,
                                        ::checkFKeysSeries)

    private suspend fun checkIssueFkVariantOf(issues: List<Issue>) =
        checkForMissingForeignKeyModels(issues,
                                        Issue::variantOf,
                                        database.issueDao(),
                                        webservice::getIssuesByIds,
                                        ::checkFKeysIssue)

    private suspend fun checkSeriesBondFkOriginIssue(seriesBonds: List<SeriesBond>) =
        checkForMissingForeignKeyModels(seriesBonds,
                                        SeriesBond::originIssue,
                                        database.issueDao(),
                                        webservice::getIssuesByIds,
                                        ::checkFKeysIssue)

    private suspend fun checkSeriesBondFkOriginSeries(seriesBonds: List<SeriesBond>) =
        checkForMissingForeignKeyModels(seriesBonds,
                                        SeriesBond::origin,
                                        database.seriesDao(),
                                        webservice::getSeriesByIds,
                                        ::checkFKeysSeries)

    private suspend fun checkSeriesBondFkTargetSeries(seriesBonds: List<SeriesBond>) =
        checkForMissingForeignKeyModels(seriesBonds,
                                        SeriesBond::target,
                                        database.seriesDao(),
                                        webservice::getSeriesByIds,
                                        ::checkFKeysSeries)

    private suspend fun checkSeriesBondFkTargetIssue(seriesBonds: List<SeriesBond>) =
        checkForMissingForeignKeyModels(seriesBonds,
                                        SeriesBond::targetIssue,
                                        database.issueDao(),
                                        webservice::getIssuesByIds,
                                        ::checkFKeysIssue)


    private suspend fun checkStoryFkIssue(stories: List<Story>) =
        checkForMissingForeignKeyModels(stories,
                                        Story::issue,
                                        database.issueDao(),
                                        webservice::getIssuesByIds,
                                        ::checkFKeysIssue)

    private suspend fun checkAppearanceFkCharacter(appearances: List<Appearance>) =
        checkForMissingForeignKeyModels(appearances,
                                        Appearance::character,
                                        database.characterDao(),
                                        webservice::getCharactersByIds)

    private suspend fun checkAppearanceFkStory(appearances: List<Appearance>) =
        checkForMissingForeignKeyModels(appearances,
                                        Appearance::story,
                                        database.storyDao(),
                                        webservice::getStoriesByIds,
                                        ::checkFKeysStory)

    private suspend fun <T : CreditX> checkCreditFkNameDetail(credits: List<T>) =
        checkForMissingForeignKeyModels(credits,
                                        CreditX::nameDetail,
                                        database.nameDetailDao(),
                                        webservice::getNameDetailsByIds,
                                        ::checkFKeysNameDetail)

    private suspend fun <T : CreditX> checkCreditFkStory(credits: List<T>) =
        checkForMissingForeignKeyModels(credits,
                                        CreditX::story,
                                        database.storyDao(),
                                        webservice::getStoriesByIds,
                                        ::checkFKeysStory)

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
            var timeStarted: Long
            timeStarted = try {
                prefs.getLong("${prefsKey}_STARTED_TIME", Instant.MIN.epochSecond)
            } catch (e: ClassCastException) {
                Instant.MIN.epochSecond
            }
            val isExpired = timeStarted + 3 < Instant.now().epochSecond

            return DEBUG || (isStale && (!isStarted || isExpired))
        }

        /**
         * Get items by argument
         * @return null on connection error
         */
        internal suspend fun <GcdType : GcdJson<ModelType>, ModelType : DataModel, ArgType : Any>
                getItemsByArgument(
            arg: ArgType,
            apiCall: KSuspendFunction1<ArgType, List<Item<GcdType, ModelType>>>,
        ): List<ModelType>? =
            supervisorScope {
                runSafely("getItemsByArgument: ${apiCall.name}", arg, {
                    async { apiCall(it) }
                })?.models
            }

        internal suspend fun <GcdType : GcdJson<ModelType>, ModelType : DataModel, ArgType : Any>
                getItemsByList(
            arg: List<ArgType>,
            apiCall: KSuspendFunction1<List<ArgType>, List<Item<GcdType, ModelType>>>,
        ): List<ModelType> {
            val lists = arg.chunked(20)
            val res = mutableListOf<ModelType>()
            for (elem in lists) {
                getItemsByArgument(elem, apiCall)?.let { res.addAll(it) }
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
                    followup(items)
                    collector.collect(items)
                }
                return@coroutineScope items ?: emptyList()
            }
        }

        private const val BUFFER_SIZE = 5000
        private const val UPSERT_WAIT: Long = 2

        abstract class Collector<ModelType : DataModel>(private val dao: BaseDao<ModelType>) {
            private var count: Int = 0
            private var startTime: Instant? = null
            private var itemList = mutableListOf<ModelType>()

            @Synchronized
            fun collect(result: List<ModelType>?) {
                if (startTime == null) {
                    startTime = Instant.now()
                }
                result?.let { itemList.addAll(it) }

                val currentTime = Instant.now()

                if (++count == BUFFER_SIZE) {
                    saveList()
                }

                CoroutineScope(lowPriorityDispatcher).launch {
                    delay(3000)
                    if (currentTime > (startTime?.plusSeconds(UPSERT_WAIT) ?: Instant.MIN) &&
                        itemList.isNotEmpty()) {
                        saveList()
                    }
                }
            }

            @Synchronized
            private fun saveList() {
                dao.upsert(itemList)
                itemList.clear()
                count = 0
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

    interface UpdateByPageCallback {
        fun setTotal(total: Int)
        fun setNumComplete(numComplete: Int)
        fun setProgress()
    }

    /**
     * Get all paged - calls getItemsByPage , performs verifyForeignKeys on each page, then
     * saves each page to dao
     */
    internal suspend fun <ModelType : DataModel> refreshAllPaged(
        prefs: SharedPreferences,
        savePageTag: String,
        saveTag: String,
        getItemsByPage: suspend (Int) -> List<ModelType>?,
        verifyForeignKeys: suspend (List<ModelType>) -> Unit = {},
        dao: BaseDao<ModelType>,
        getNumPages: suspend () -> Count,
        updateProgress: ((Int) -> Unit)? = null
    ) {
        // If it's been a week, mark all pages as stale
        if (checkIfStale(saveTag, WEEKLY, prefs)) {
            Log.d(TAG, "Pages are stale, updating")
            Repository.savePrefValue(prefs, savePageTag, "")
        } else {
            updateProgress?.invoke(100)
        }

        // make an array for whether each page has been updated
        val numPages: Int = getNumPages().count
        val pagesComplete = BooleanArray(numPages)

        // savePageTag's value is a string of whether each page has been updated, e.g. "tfffttttftt"
        val pagesCompleteSaved: String = prefs.getString(savePageTag, "") ?: ""

        // if the number of pages match, then it's the same data, so use PCS to update PC,
        // otherwise, this has the effect of marking all pages as unupdated
        if (numPages == pagesCompleteSaved.length) {
            pagesCompleteSaved.forEachIndexed { index, c ->
                pagesComplete[index] = (c == 't')
            }
        }

        var numComplete = pagesComplete.count { it }
        var progress: Int

        fun pageFinished() {
            synchronized(this) {
                numComplete++
                progress = ((numComplete.toFloat()) / numPages * 100).toInt()
                Log.d(TAG, "Page Complete $progress")
                updateProgress?.invoke(progress)
            }
        }

        // for each page in PC, if not complete, then try to update again
        // if request is successful, save and mark as updated
        withContext(CoroutineScope(Dispatchers.IO).coroutineContext) {
            pagesComplete.forEachIndexed { currPage, isComplete ->
                if (!isComplete) {
                    launch {
                        Log.d(TAG, "WORKING ON $saveTag PAGE $currPage")
                        val itemPage: List<ModelType>? = getItemsByPage(currPage)

                        // if request is successful, save and mark as updated
                        if (itemPage != null && itemPage.isNotEmpty()) {
                            verifyForeignKeys(itemPage)
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
        saveTag: String,
        getItems: suspend () -> List<ModelType>?,
        followup: suspend (List<ModelType>) -> Unit = {},
        dao: BaseDao<ModelType>,
    ) {
        if (checkIfStale(saveTag, WEEKLY, prefs)) {
            coroutineScope {
                val items: List<ModelType>? = getItems()

                if (items != null && items.isNotEmpty()) {
                    followup(items)
                    if (dao.upsertSus(items)) {
                        Repository.saveTime(prefs, saveTag)
                    }
                }
            }
        }
    }

    /**
     * Get missing foreign key models
     *
     * @param M The [DataModel] of the objects to be checked
     * @param FG The [GcdJson] retrieved by [getFkItems]
     * @param FM The [DataModel] of the foreign keys
     * @param itemsToCheck The list of [M] whose foreign keys will be checked
     * @param getForeignKey A lambda to access the foreign key id on a model
     * @param foreignKeyDao The dao of the foreign key objects' [GcdJson]
     * @param getFkItems The function to retrieve the foreign key objects remotely
     * @param fkFollowup A function that to check a foreign key of the foreign key models
     */
    private suspend fun <M : DataModel, FG : GcdJson<FM>, FM : DataModel>
            checkForMissingForeignKeyModels(
        itemsToCheck: List<M>,
        getForeignKey: (M) -> Int?,
        foreignKeyDao: BaseDao<FM>,
        getFkItems: KSuspendFunction1<List<Int>, List<Item<FG, FM>>>,
        fkFollowup: (suspend (List<FM>) -> Unit)? = null,
    ) {
        val fkIds: List<Int> = itemsToCheck.mapNotNull { getForeignKey(it) }
        val missingIds: List<Int> = fkIds.mapNotNull {
            if (foreignKeyDao.get(it) == null) it else null
        }

        val missingItems: MutableList<FM> = mutableListOf()
//            getItemsByArgument(missingIds.toSet().toList(), getFkItems)
        missingIds.chunked(20).forEach { ids ->
            getItemsByArgument(ids.toSet().toList(), getFkItems)?.let { items ->
                missingItems.addAll(items)
            }
        }

        if (missingItems.isNotEmpty()) {
            fkFollowup?.let { it(missingItems) }
            foreignKeyDao.upsert(missingItems)
        }
    }

    class PriorityDispatcher {
        companion object {
            private var NOW_INSTANCE: CoroutineDispatcher? = null
            private var HP_INSTANCE: CoroutineDispatcher? = null
            private var LP_INSTANCE: CoroutineDispatcher? = null

            val nowDispatcher: CoroutineDispatcher
                get() {
                    val thread = HandlerThread("nowThread",
                                               Process.THREAD_PRIORITY_DISPLAY).also {
                        it.start()
                    }
                    val res = NOW_INSTANCE
                        ?: Handler(thread.looper).asCoroutineDispatcher("nowDispatcher")
                    if (NOW_INSTANCE == null) {
                        NOW_INSTANCE = res
                    }
                    return NOW_INSTANCE!!
                }

            val highPriorityDispatcher: CoroutineDispatcher
                get() {
                    val thread = HandlerThread("highPriorityThread",
                                               Process.THREAD_PRIORITY_MORE_FAVORABLE).also {
                        it.start()
                    }
                    val res = HP_INSTANCE
                        ?: Handler(thread.looper).asCoroutineDispatcher("highPriorityDisptcher")
                    if (HP_INSTANCE == null) {
                        HP_INSTANCE = res
                    }
                    return HP_INSTANCE!!
                }

            val lowPriorityDispatcher: CoroutineDispatcher
                get() {
                    val thread = HandlerThread("lowPriorityThread",
                                               Process.THREAD_PRIORITY_BACKGROUND).also {
                        it.start()
                    }
                    val res = LP_INSTANCE
                        ?: Handler(thread.looper).asCoroutineDispatcher("lowPriorityDisptcher")
                    if (LP_INSTANCE == null) {
                        LP_INSTANCE = res
                    }
                    return LP_INSTANCE!!
                }
        }
    }
}


