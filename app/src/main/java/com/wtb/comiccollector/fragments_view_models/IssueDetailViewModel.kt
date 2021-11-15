//@file:Suppress("RemoveExplicitTypeArguments")

package com.wtb.comiccollector.fragments_view_models

import android.net.Uri
import android.util.Log
import androidx.lifecycle.*
import com.wtb.comiccollector.*
import com.wtb.comiccollector.database.models.*
import com.wtb.comiccollector.repository.Repository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed

private const val TAG = APP + "IssueDetailViewModel"

@ExperimentalCoroutinesApi
class IssueDetailViewModel : ViewModel() {

    private val repository: Repository = Repository.get()

    private val currentIssue: FullIssue?
        get() = if (variantId.value == AUTO_ID) {
            mIssue.value
        } else {
            variantLiveData.value
        }

    private val currentCover: Cover?
        get() = if (variantId.value == AUTO_ID) {
            primaryCover.value
        } else {
            variantCover.value
        }

    private val currentCollections: List<CollectionItem>?
        get() = if (variantId.value == AUTO_ID) {
            primaryCollections.value
        } else {
            variantCollections.value
        }

    private val _primaryId = MutableStateFlow(AUTO_ID)
    val primaryId: StateFlow<Int>
        get() = _primaryId

    private val mIssue: StateFlow<FullIssue?> = primaryId.flatMapLatest { id ->
        Log.d(TAG, "issueId changed: $id")
        repository.getIssue(id)
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = null
    )

    internal val primaryIssue: LiveData<FullIssue?> = mIssue.asLiveData()

    internal val primaryCover: LiveData<Cover?> = primaryId.mapLatest {
        repository.getCover(it)
    }.asLiveData()

    internal val primaryStoriesLiveData: LiveData<List<Story>> =
        primaryId.flatMapLatest { issueId -> repository.getStoriesByIssue(issueId) }.asLiveData()

    internal val primaryCreditsLiveData: LiveData<List<FullCredit>> =
        primaryId.flatMapLatest { issueId -> repository.getCreditsByIssue(issueId) }.asLiveData()

    internal val primaryAppearancesLiveData =
        primaryId.flatMapLatest { repository.getAppearancesByIssue(it) }.asLiveData()

    internal val primaryCollections =
        primaryId.flatMapLatest { repository.getIssueCollections(it).asFlow() }.asLiveData()

    private val _variantId = MutableStateFlow(AUTO_ID)
    private val variantId: StateFlow<Int>
        get() = _variantId

    // Other parts rely on this possibly being null, which is why it's LiveData, instead of
    // StateFlow like 'issue'
    internal val variantLiveData: LiveData<FullIssue?> =
        variantId.flatMapLatest { id ->
            Log.d(TAG, "variantId changed, updating variantLiveData $id")
            repository.getIssue(id)
        }.asLiveData()

    internal val variantCover: LiveData<Cover?> = variantId.mapLatest {
        repository.getCover(it)
    }.asLiveData()

    internal val variantStoriesLiveData: LiveData<List<Story>> =
        variantId.flatMapLatest { issueId -> repository.getStoriesByIssue(issueId) }.asLiveData()

    internal val variantCreditsLiveData: LiveData<List<FullCredit>> =
        variantId.flatMapLatest { issueId -> repository.getCreditsByIssue(issueId) }.asLiveData()

    internal val variantAppearancesLiveData: LiveData<List<FullAppearance>> =
        variantId.flatMapLatest { issueId -> repository.getAppearancesByIssue(issueId) }
            .asLiveData()

    internal val variantCollections =
        variantId.flatMapLatest { repository.getIssueCollections(it).asFlow() }.asLiveData()

    internal val variantsLiveData: LiveData<List<Issue>> =
        primaryId.flatMapLatest { id -> repository.getVariants(id) }.asLiveData()

    internal val issueList: LiveData<List<FullIssue>> = mIssue.flatMapLatest { fullIssue ->
        fullIssue?.let {
            repository.getIssuesByFilter(
                SearchFilter(
                    series = FullSeries(it.series),
                    myCollection = false,
                    sortType = SortType(
                        ComicCollectorApplication.context!!.getString(R.string.sort_type_issue_number),
                        ComicCollectorApplication.context!!.getString(R.string.column_issue_num),
                        "ie",
                        SortType.SortOrder.ASC
                    )
                )
            )
        } ?: emptyFlow()
    }.asLiveData()

    fun loadIssue(issueId: Int) {
        _primaryId.value = issueId
    }

    fun loadVariant(issueId: Int?) {
        Log.d(TAG, "Loading variant: $issueId")
        _variantId.value = issueId ?: AUTO_ID
    }

    fun clearVariant() {
        _variantId.value = AUTO_ID
    }

    fun addToCollection(collId: Int) {
        Log.d(
            TAG,
            "Adding ${currentIssue?.series?.seriesName} #${currentIssue?.issue?.issueNum} to " +
                    "collection #$collId"
        )
        currentIssue?.let { repository.addToCollection(it, collId) }
        currentCover?.let { repository.markCoverSave(it.coverId) }
    }


    fun removeFromCollection(collId: Int) {
        Log.d(TAG, "REMOVING FROM COLLECTION $currentIssue")
        currentIssue?.let { repository.removeFromCollection(it.issue.issueId, collId) }
        currentCover?.let {
            it.id.let { cid ->
                if (currentCollections?.isEmpty() == true) {
                    repository.markCoverDelete(cid)
                }
            }
        }
    }


    //    /***
//     * This stuff is only used in the issue edit fragment, iow: not used
//     */
//    var allSeriesLiveData: LiveData<List<Series>> = repository.allSeries.asLiveData()
//
//    var allPublishersLiveData: LiveData<List<Publisher>> = repository.allPublishers.asLiveData()
//
//    var allCreatorsLiveData: LiveData<List<Creator>> = repository.allCreators.asLiveData()
//
//    var allRolesLiveData: LiveData<List<Role>> = repository.allRoles.asLiveData()
//
    fun upsert(dataModel: DataModel) {
        when (dataModel) {
            is Series -> repository.saveSeries(dataModel)
            is Creator -> repository.saveCreator(dataModel)
            is Credit -> repository.saveCredit(dataModel)
            is Issue -> repository.saveIssue(dataModel)
            is Role -> repository.saveRole(dataModel)
            else -> Unit
        }
    }

    fun delete(dataModel: DataModel) {
        when (dataModel) {
            is Issue -> repository.deleteIssue(dataModel)
            else -> Unit
        }
    }
}

