@file:Suppress("RemoveExplicitTypeArguments", "RemoveExplicitTypeArguments")

package com.wtb.comiccollector.issue_details.view_models

import android.util.Log
import androidx.lifecycle.*
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.Filter
import com.wtb.comiccollector.database.Daos.Count
import com.wtb.comiccollector.database.models.*
import com.wtb.comiccollector.repository.Repository

private const val TAG = APP + "IssueDetailViewModel"

class IssueDetailViewModel : ViewModel() {

    private val repository: Repository = Repository.get()

    private val issueIdLiveData = MutableLiveData<Int>()
    private val variantIdLiveData = MutableLiveData<Int?>()

    internal val issueLiveData: LiveData<FullIssue?> =
        Transformations.switchMap(issueIdLiveData) { issueId ->
            repository.getIssue(issueId)
        }

    val issueListLiveData = issueLiveData.switchMap { issue: FullIssue? ->
        repository.getIssuesByFilterFlow(Filter(series = issue?.series))
            .asLiveData(viewModelScope.coroutineContext)
    }

//    val issueListLiveData = issueLiveData.switchMap { issue: FullIssue ->
//        liveData(context = viewModelScope.coroutineContext) {
//            emit(issueRepository.getIssuesByFilterFlow(Filter(series = issue.series)))
//        }
//    }
//
//    val issueListLiveData = Transformations.switchMap(issueLiveData) { issue ->
//        issueRepository.getIssuesByFilterLiveData(Filter(series = issue.series)).asLiveData()
//    }
//

    //    lateinit var issueListLiveData: PagingSource<Int, FullIssue>
//
//    init {
//        issueLiveData.observeForever(object : Observer<FullIssue> {
//            override fun onChanged(t: FullIssue?) {
//                issueListLiveData = issueRepository.getIssuesByFilter(Filter(series = t?.series))
//            }
//        })
//    }
//
    val issueStoriesLiveData: LiveData<List<Story>> =
        Transformations.switchMap(issueIdLiveData) { issueId ->
            repository.getStoriesByIssue(issueId)
        }

    val issueCreditsLiveData: LiveData<List<FullCredit>> =
        Transformations.switchMap(issueIdLiveData) { issueId ->
            repository.getCreditsByIssue(issueId)
        }

    val variantLiveData: LiveData<FullIssue?> =
        Transformations.switchMap(variantIdLiveData) { variantId ->
            Log.d(TAG, "Loading variantLD")
            if (variantId != null) {
                Log.d(TAG, "Setting variant")
                variantId.let { repository.getIssue(it) }
            } else {
                Log.d(TAG, "Clearing variant")
                liveData { emit(null as FullIssue?) }
            }
        }

    val variantStoriesLiveData: LiveData<List<Story>> =
        Transformations.switchMap(variantIdLiveData) { variantId ->
            if (variantId != null) {
                repository.getStoriesByIssue(variantId)
            } else {
                liveData { emit(emptyList<Story>()) }
            }
        }

    val variantCreditsLiveData: LiveData<List<FullCredit>> =
        Transformations.switchMap(variantIdLiveData) { variantId ->
            if (variantId != null) {
                repository.getCreditsByIssue(variantId)
            } else {
                liveData { emit(emptyList<FullCredit>()) }
            }
        }

    val variantsLiveData: LiveData<List<Issue>> =
        Transformations.switchMap(issueIdLiveData) { issueId ->
            repository.getVariants(issueId)
        }

    val inCollectionLiveData: LiveData<Count> =
        Transformations.switchMap(issueIdLiveData) { issueId ->
            repository.inCollection(issueId)
        }

    val variantInCollectionLiveData: LiveData<Count> =
        Transformations.switchMap(variantIdLiveData) { variantId ->
            if (variantId != null) {
                repository.inCollection(variantId)
            } else {
                liveData { emit(Count(0)) }
            }
        }

    var allSeriesLiveData: LiveData<List<Series>> = repository.allSeries

    var allPublishersLiveData: LiveData<List<Publisher>> = repository.allPublishers

    var allCreatorsLiveData: LiveData<List<Creator>> = repository.allCreators

    var allRolesLiveData: LiveData<List<Role>> = repository.allRoles

    fun loadIssue(issueId: Int) {
        issueIdLiveData.value = AUTO_ID
        issueIdLiveData.value = issueId
    }

    fun getIssueId() = issueIdLiveData.value

    fun loadVariant(issueId: Int?) {
        variantIdLiveData.value = AUTO_ID
        variantIdLiveData.value = issueId
    }

    fun clearVariant() {
        Log.d(TAG, "Clearing variant")
        variantIdLiveData.value = null
    }

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

    fun upsertCover(cover: Cover) {
        repository.saveCover(cover)
    }

    fun addToCollection() {
        (variantIdLiveData.value
            ?: issueIdLiveData.value)?.let { repository.addToCollection(it) }
    }

    fun removeFromCollection() {
        (variantIdLiveData.value
            ?: issueIdLiveData.value)?.let {
            repository.removeFromCollection(it)
        }
    }
}