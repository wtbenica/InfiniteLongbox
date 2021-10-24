package com.wtb.comiccollector.repository

import android.util.Log
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.Webservice
import com.wtb.comiccollector.database.IssueDatabase
import com.wtb.comiccollector.database.daos.BaseDao
import com.wtb.comiccollector.database.models.*
import com.wtb.comiccollector.repository.Updater.Companion.retrieveItemsByArgument
import com.wtb.comiccollector.repository.Updater.PriorityDispatcher.Companion.highPriorityDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import kotlin.reflect.KSuspendFunction1

@ExperimentalCoroutinesApi
class FKeyChecker(val database: IssueDatabase, private val webservice: Webservice) {

    /**
     * Runs [func] on chunks of [chunkSize] items from [models]
     */
    private suspend fun <T : DataModel> checkFKeysChunked(
        models: List<T>,
        func: KSuspendFunction1<List<T>, Unit>,
        chunkSize: Int = 500
    ) {
        var i = 0
        do {
            func(models.subList(i * chunkSize, Integer.min((i + 1) * chunkSize, models.size)))
        } while (++i * chunkSize < models.size)
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
     * @param checkFkFks A function to check the foreign key(s) of the foreign key models
     */
    private suspend fun <M : DataModel, FG : GcdJson<FM>, FM : DataModel>
            checkForMissingForeignKeyModels(
        itemsToCheck: List<M>,
        getForeignKey: (M) -> Int?,
        foreignKeyDao: BaseDao<FM>,
        getFkItems: KSuspendFunction1<List<Int>, List<Item<FG, FM>>>,
        checkFkFks: (suspend (List<FM>) -> Unit)? = null,
    ) {
        val fkIds: List<Int> = itemsToCheck.mapNotNull { getForeignKey(it) }
        val missingIds: List<Int> = fkIds.mapNotNull {
            if (foreignKeyDao.get(it) == null) it else null
        }
        val theTime = LocalDateTime.now()
        Log.d(TAG, "UPDATING $theTime: missing ${missingIds.size} items $getForeignKey")
        val missingItems: MutableList<FM> = mutableListOf()

        missingIds.chunked(20).forEach { ids ->
            retrieveItemsByArgument(ids.toSet().toList(), getFkItems)?.let { items: List<FM> ->
                Log.d(TAG, "UPDATING $theTime: adding ${items.size} to missingItems")
                missingItems.addAll(items)
            }
        }

        Log.d(TAG, "UPDATING $theTime: recovered ${missingItems.size} items $getForeignKey")

        if (missingItems.isNotEmpty()) {
            withContext(CoroutineScope(highPriorityDispatcher).coroutineContext) {
                checkFkFks?.let { it(missingItems) }
            }.let {
                foreignKeyDao.upsert(missingItems)
            }
        }
    }

    internal suspend fun checkFKeysSeries(series: List<Series>) {
        checkFKeysChunked(series, this::checkSeriesFkPublisher)
    }

    private suspend fun checkSeriesFkPublisher(series: List<Series>) =
        checkForMissingForeignKeyModels(
            itemsToCheck = series,
            getForeignKey = Series::publisher,
            foreignKeyDao = database.publisherDao(),
            getFkItems = webservice::getPublishersByIds
        )

    internal suspend fun checkFKeysIssue(issues: List<Issue>) {
        checkFKeysChunked(issues, this::checkIssueFkSeries)
        checkFKeysChunked(issues, this::checkIssueFkVariantOf)
    }

    private suspend fun checkIssueFkSeries(issues: List<Issue>) =
        checkForMissingForeignKeyModels(
            itemsToCheck = issues,
            getForeignKey = Issue::series,
            foreignKeyDao = database.seriesDao(),
            getFkItems = webservice::getSeriesByIds,
            checkFkFks = ::checkFKeysSeries
        )

    private suspend fun checkIssueFkVariantOf(issues: List<Issue>) =
        checkForMissingForeignKeyModels(
            itemsToCheck = issues,
            getForeignKey = Issue::variantOf,
            foreignKeyDao = database.issueDao(),
            getFkItems = webservice::getIssuesByIds,
            checkFkFks = ::checkFKeysIssue
        )

    internal suspend fun checkFKeysStory(stories: List<Story>) {
        checkFKeysChunked(stories, this::checkStoryFkIssue)
    }

    private suspend fun checkStoryFkIssue(stories: List<Story>) =
        checkForMissingForeignKeyModels(
            itemsToCheck = stories,
            getForeignKey = Story::issue,
            foreignKeyDao = database.issueDao(),
            getFkItems = webservice::getIssuesByIds,
            checkFkFks = ::checkFKeysIssue
        )

    internal suspend fun checkFKeysNameDetail(nameDetails: List<NameDetail>) {
        checkFKeysChunked(nameDetails, this::checkNameDetailFkCreator)
    }

    private suspend fun checkNameDetailFkCreator(nameDetails: List<NameDetail>) =
        checkForMissingForeignKeyModels(
            itemsToCheck = nameDetails,
            getForeignKey = NameDetail::creator,
            foreignKeyDao = database.creatorDao(),
            getFkItems = webservice::getCreatorsByIds
        )

    internal suspend fun <T : CreditX> checkFKeysCredit(credits: List<T>) {
//        checkFKeysChunked(credits, this::checkCreditFkSeries)
//        checkFKeysChunked(credits, this::checkCreditFkIssue)
        checkFKeysChunked(credits, this::checkCreditFkStory)
//        checkFKeysChunked(credits, this::checkCreditFkNameDetail)
    }

    private suspend fun <T : CreditX> checkCreditFkSeries(credits: List<T>) =
        checkForMissingForeignKeyModels(
            itemsToCheck = credits,
            getForeignKey = CreditX::series,
            foreignKeyDao = database.seriesDao(),
            getFkItems = webservice::getSeriesByIds,
            checkFkFks = ::checkFKeysSeries
        )

    private suspend fun <T : CreditX> checkCreditFkIssue(credits: List<T>) =
        checkForMissingForeignKeyModels(
            itemsToCheck = credits,
            getForeignKey = CreditX::issue,
            foreignKeyDao = database.issueDao(),
            getFkItems = webservice::getIssuesByIds,
            checkFkFks = ::checkFKeysIssue
        )

    private suspend fun <T : CreditX> checkCreditFkNameDetail(credits: List<T>) =
        checkForMissingForeignKeyModels(
            itemsToCheck = credits,
            getForeignKey = CreditX::nameDetail,
            foreignKeyDao = database.nameDetailDao(),
            getFkItems = webservice::getNameDetailsByIds,
            checkFkFks = ::checkFKeysNameDetail
        )

    private suspend fun <T : CreditX> checkCreditFkStory(credits: List<T>) =
        checkForMissingForeignKeyModels(
            itemsToCheck = credits,
            getForeignKey = CreditX::story,
            foreignKeyDao = database.storyDao(),
            getFkItems = webservice::getStoriesByIds,
            checkFkFks = ::checkFKeysStory
        )

    internal suspend fun checkFKeysSeriesBond(bonds: List<SeriesBond>) {
        checkSeriesBondFkOriginSeries(bonds)
        checkSeriesBondFkOriginIssue(bonds)
        checkSeriesBondFkTargetIssue(bonds)
        checkSeriesBondFkTargetSeries(bonds)
    }

    private suspend fun checkSeriesBondFkOriginIssue(seriesBonds: List<SeriesBond>) =
        checkForMissingForeignKeyModels(
            itemsToCheck = seriesBonds,
            getForeignKey = SeriesBond::originIssue,
            foreignKeyDao = database.issueDao(),
            getFkItems = webservice::getIssuesByIds,
            checkFkFks = ::checkFKeysIssue
        )

    private suspend fun checkSeriesBondFkOriginSeries(seriesBonds: List<SeriesBond>) =
        checkForMissingForeignKeyModels(
            itemsToCheck = seriesBonds,
            getForeignKey = SeriesBond::origin,
            foreignKeyDao = database.seriesDao(),
            getFkItems = webservice::getSeriesByIds,
            checkFkFks = ::checkFKeysSeries
        )

    private suspend fun checkSeriesBondFkTargetSeries(seriesBonds: List<SeriesBond>) =
        checkForMissingForeignKeyModels(
            itemsToCheck = seriesBonds,
            getForeignKey = SeriesBond::target,
            foreignKeyDao = database.seriesDao(),
            getFkItems = webservice::getSeriesByIds,
            checkFkFks = ::checkFKeysSeries
        )

    private suspend fun checkSeriesBondFkTargetIssue(seriesBonds: List<SeriesBond>) =
        checkForMissingForeignKeyModels(
            itemsToCheck = seriesBonds,
            getForeignKey = SeriesBond::targetIssue,
            foreignKeyDao = database.issueDao(),
            getFkItems = webservice::getIssuesByIds,
            checkFkFks = ::checkFKeysIssue
        )

    internal suspend fun checkFKeysAppearance(appearances: List<Appearance>) {
        checkAppearanceFkCharacter(appearances)
        checkAppearanceFkStory(appearances)
    }

    /**
     * Checks appearances foreign keys: character
     */
    private suspend fun checkAppearanceFkCharacter(appearances: List<Appearance>) =
        checkForMissingForeignKeyModels(
            itemsToCheck = appearances,
            getForeignKey = Appearance::character,
            foreignKeyDao = database.characterDao(),
            getFkItems = webservice::getCharactersByIds
        )

    /**
     * Checks appearances foreign keys: stories
     */
    private suspend fun checkAppearanceFkStory(appearances: List<Appearance>) =
        checkForMissingForeignKeyModels(
            itemsToCheck = appearances,
            getForeignKey = Appearance::story,
            foreignKeyDao = database.storyDao(),
            getFkItems = webservice::getStoriesByIds,
            checkFkFks = ::checkFKeysStory
        )

    companion object {
        const val TAG = APP + "FKeyChecker"
    }
}