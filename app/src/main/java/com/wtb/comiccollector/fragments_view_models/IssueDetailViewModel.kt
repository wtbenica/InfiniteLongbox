@file:Suppress("RemoveExplicitTypeArguments", "RemoveExplicitTypeArguments")

package com.wtb.comiccollector.fragments_view_models

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.SearchFilter
import com.wtb.comiccollector.database.Daos.Count
import com.wtb.comiccollector.database.models.*
import com.wtb.comiccollector.repository.DUMMY_ID
import com.wtb.comiccollector.repository.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.launch

private const val TAG = APP + "IssueDetailViewModel"

@ExperimentalCoroutinesApi
class IssueDetailViewModel : ViewModel() {

    private val repository: Repository = Repository.get()

    private val _issueId = MutableStateFlow<Int>(DUMMY_ID)
    private val _variantId = MutableStateFlow<Int>(DUMMY_ID)

    val issueId: StateFlow<Int> = _issueId
    val variantId: StateFlow<Int> = _variantId

    val issue: StateFlow<FullIssue?> = issueId.flatMapLatest { id ->
        Log.d(TAG, "issueId changed: $id")
        repository.getIssue(id)
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = null
    )

    val variant: StateFlow<FullIssue?> =
        variantId.flatMapLatest { id -> repository.getIssue(id) }.stateIn(
            scope = viewModelScope,
            started = WhileSubscribed(5000),
            initialValue = null
        )

    val issueList: LiveData<List<FullIssue>> = issue.flatMapLatest { fullIssue ->
        repository.getIssuesByFilter(SearchFilter(series = fullIssue?.series, myCollection = false))
    }.asLiveData()

    val issueStoriesLiveData: LiveData<List<Story>> =
        issueId.flatMapLatest { issueId ->
            Log.d(TAG, "getting issue stories $issueId")
            val a = repository.getStoriesByIssue(issueId)
            CoroutineScope(Dispatchers.IO).launch {
                Log.d(TAG, "HASBTGL ${a.collect { it.size }}")
            }
            a
        }.asLiveData()

    val issueCreditsLiveData: LiveData<List<FullCredit>> =
        issueId.flatMapLatest { issueId -> repository.getCreditsByIssue(issueId) }.asLiveData()

    val variantStoriesLiveData: LiveData<List<Story>> =
        variantId.flatMapLatest { issueId -> repository.getStoriesByIssue(issueId) }.asLiveData()

    val variantCreditsLiveData: LiveData<List<FullCredit>> =
        variantId.flatMapLatest { issueId -> repository.getCreditsByIssue(issueId) }
            .asLiveData()

    val variantsLiveData: LiveData<List<Issue>> =
        issueId.flatMapLatest { id -> repository.getVariants(id) }.asLiveData()

    val inCollectionLiveData: LiveData<Count> =
        issueId.flatMapLatest { repository.inCollection(it) }.asLiveData()

    val variantInCollectionLiveData: LiveData<Count> =
        variantId.flatMapLatest { repository.inCollection(it) }.asLiveData()

    fun loadIssue(issueId: Int) {
        Log.d(TAG, "loadIssue: $issueId")
        _issueId.value = issueId
    }

    fun getIssueId() = issueId.value

    fun loadVariant(issueId: Int?) {
        _variantId.value = issueId ?: DUMMY_ID
    }

    fun clearVariant() {
        Log.d(TAG, "Clearing variant")
        _variantId.value = DUMMY_ID
    }

    fun addToCollection() {
        val currentIssueId = if (variantId.value == DUMMY_ID) {
            issueId.value
        } else {
            variantId.value
        }
        repository.addToCollection(currentIssueId)
    }


    fun removeFromCollection() {
        val currentIssueId = if (variantId.value == DUMMY_ID) {
            issueId.value
        } else {
            variantId.value
        }
        repository.removeFromCollection(currentIssueId)
    }


    /***
     * This stuff is only used in the issue edit fragment, iow: not used
     */
    var allSeriesLiveData: LiveData<List<Series>> = repository.allSeries.asLiveData()

    var allPublishersLiveData: LiveData<List<Publisher>> = repository.allPublishers.asLiveData()

    var allCreatorsLiveData: LiveData<List<Creator>> = repository.allCreators.asLiveData()

    var allRolesLiveData: LiveData<List<Role>> = repository.allRoles.asLiveData()

    fun updateIssue(issue: Issue) {
        repository.saveIssue(issue)
    }

    fun deleteIssue(issue: Issue) {
        repository.deleteIssue(issue)
    }

    fun upsertSeries(series: Series) {
        repository.saveSeries(series)
    }

    fun upsertCreator(creator: Creator) {
        repository.saveCreator(creator)
    }

    fun upsertCredit(credit: Credit) {
        repository.saveCredit(credit)
    }

    fun deleteSeries(series: Series) {
        repository.deleteSeries(series)
    }

    fun deleteCredit(credit: Credit) {
        repository.deleteCredit(credit)
    }
}
