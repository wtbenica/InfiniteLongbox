//@file:Suppress("RemoveExplicitTypeArguments")

package com.wtb.comiccollector.fragments_view_models

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.SearchFilter
import com.wtb.comiccollector.database.daos.Count
import com.wtb.comiccollector.database.models.*
import com.wtb.comiccollector.repository.Repository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

private const val TAG = APP + "IssueDetailViewModel"

@ExperimentalCoroutinesApi
class IssueDetailViewModel : ViewModel() {

    private val repository: Repository = Repository.get()

    private val _issueId = MutableStateFlow(AUTO_ID)
    val issueId: StateFlow<Int>
        get() = _issueId
    private val _variantId = MutableStateFlow(AUTO_ID)
    val variantId: StateFlow<Int>
        get() = _variantId


    val issue: StateFlow<FullIssue?> = issueId.flatMapLatest { id ->
        repository.getIssue(id)
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = null
    )

    val issueList: LiveData<List<FullIssue>> = issue.flatMapLatest { fullIssue ->
        val seriesId = (fullIssue?.series?.seriesId ?: AUTO_ID)
        repository.getIssuesByFilter(SearchFilter(series = FullSeries(Series(seriesId = seriesId)),
                                                  myCollection = false))
    }.asLiveData()

    val issueStoriesLiveData: LiveData<List<Story>> =
        issueId.flatMapLatest { issueId -> repository.getStoriesByIssue(issueId) }.asLiveData()

    val issueCreditsLiveData: LiveData<List<FullCredit>> =
        issueId.flatMapLatest { issueId -> repository.getCreditsByIssue(issueId) }.asLiveData()

    val variantLiveData: LiveData<FullIssue?> =
        variantId.flatMapLatest { id ->
            repository.getIssue(id)
        }.asLiveData()

    val variantStoriesLiveData: LiveData<List<Story>> =
        variantId.flatMapLatest { issueId -> repository.getStoriesByIssue(issueId) }.asLiveData()

    val variantCreditsLiveData: LiveData<List<FullCredit>> =
        variantId.flatMapLatest { issueId -> repository.getCreditsByIssue(issueId) }.asLiveData()

    val variantsLiveData: LiveData<List<Issue>> =
        issueId.flatMapLatest { id -> repository.getVariants(id) }.asLiveData()

    val inCollectionLiveData: LiveData<Count> =
        issueId.flatMapLatest { repository.inCollection(it) }.asLiveData()

    val variantInCollectionLiveData: LiveData<Count> =
        variantId.flatMapLatest { repository.inCollection(it) }.asLiveData()

    fun loadIssue(issueId: Int) {
        _issueId.value = issueId
    }

    fun loadVariant(issueId: Int?) {
        _variantId.value = issueId ?: AUTO_ID
    }

    fun clearVariant() {
        _variantId.value = AUTO_ID
    }

    val currentIssue: FullIssue?
        get() {
            Log.d(TAG, "${variantId.value == AUTO_ID}")

            return if (variantId.value == AUTO_ID) {
                issue.value
            } else {
                variantLiveData.value
            }
        }

    fun addToCollection() {
        currentIssue?.let { repository.addToCollection(it) }
    }


    fun removeFromCollection() {
        val currentIssueId = if (variantId.value == AUTO_ID) {
            issueId.value
        } else {
            variantId.value
        }

        repository.removeFromCollection(currentIssueId)
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
            is Series  -> repository.saveSeries(dataModel)
            is Creator -> repository.saveCreator(dataModel)
            is Credit  -> repository.saveCredit(dataModel)
            is Issue   -> repository.saveIssue(dataModel)
            is Role    -> repository.saveRole(dataModel)
        }
    }

    fun delete(dataModel: DataModel) {
        when (dataModel) {
            is Issue -> repository.deleteIssue(dataModel)
        }
    }
}

