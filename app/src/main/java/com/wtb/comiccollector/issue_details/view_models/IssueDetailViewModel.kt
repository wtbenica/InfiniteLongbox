package com.wtb.comiccollector.issue_details.view_models

import android.util.Log
import androidx.lifecycle.*
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.Filter
import com.wtb.comiccollector.database.Daos.Count
import com.wtb.comiccollector.database.models.*
import com.wtb.comiccollector.repository.IssueRepository

private const val TAG = APP + "IssueDetailViewModel"

class IssueDetailViewModel : ViewModel() {

    private val issueRepository: IssueRepository = IssueRepository.get()

    private val issueIdLiveData = MutableLiveData<Int>()
    private val variantIdLiveData = MutableLiveData<Int?>()

    val issueLiveData: LiveData<FullIssue> =
        Transformations.switchMap(issueIdLiveData) { issueId ->
            issueRepository.getIssue(issueId)
        }

    val issueListLiveData = issueLiveData.switchMap { issue: FullIssue ->
        issueRepository.getIssuesByFilterFlow(Filter(series = issue.series))
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
            issueRepository.getStoriesByIssue(issueId)
        }

    val issueCreditsLiveData: LiveData<List<FullCredit>> =
        Transformations.switchMap(issueIdLiveData) { issueId ->
            issueRepository.getCreditsByIssue(issueId)
        }

    val variantLiveData: LiveData<FullIssue?> =
        Transformations.switchMap(variantIdLiveData) { variantId ->
            Log.d(TAG, "Loading variantLD")
            if (variantId != null) {
                Log.d(TAG, "Setting variant")
                variantId.let { issueRepository.getIssue(it) }
            } else {
                Log.d(TAG, "Clearing variant")
                liveData { emit(null as FullIssue?) }
            }
        }

    val variantStoriesLiveData: LiveData<List<Story>> =
        Transformations.switchMap(variantIdLiveData) { variantId ->
            if (variantId != null) {
                issueRepository.getStoriesByIssue(variantId)
            } else {
                liveData { emit(emptyList<Story>()) }
            }
        }

    val variantCreditsLiveData: LiveData<List<FullCredit>> =
        Transformations.switchMap(variantIdLiveData) { variantId ->
            if (variantId != null) {
                issueRepository.getCreditsByIssue(variantId)
            } else {
                liveData { emit(emptyList<FullCredit>()) }
            }
        }

    val variantsLiveData: LiveData<List<Issue>> =
        Transformations.switchMap(issueIdLiveData) { issueId ->
            issueRepository.getVariants(issueId)
        }

    val inCollectionLiveData: LiveData<Count> =
        Transformations.switchMap(issueIdLiveData) { issueId ->
            issueRepository.inCollection(issueId)
        }

    val variantInCollectionLiveData: LiveData<Count> =
        Transformations.switchMap(variantIdLiveData) { variantId ->
            if (variantId != null) {
                issueRepository.inCollection(variantId)
            } else {
                liveData { emit(Count(0)) }
            }
        }

    var allSeriesLiveData: LiveData<List<Series>> = issueRepository.allSeries

    var allPublishersLiveData: LiveData<List<Publisher>> = issueRepository.allPublishers

    var allCreatorsLiveData: LiveData<List<Creator>> = issueRepository.allCreators

    var allRolesLiveData: LiveData<List<Role>> = issueRepository.allRoles

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
        issueRepository.saveIssue(issue)
    }

    fun deleteIssue(issue: Issue) {
        issueRepository.deleteIssue(issue)
    }

    fun upsertSeries(series: Series) {
        issueRepository.saveSeries(series)
    }

    fun upsertCreator(creator: Creator) {
        issueRepository.saveCreator(creator)
    }

    fun upsertCredit(credit: Credit) {
        issueRepository.saveCredit(credit)
    }

    fun deleteSeries(series: Series) {
        issueRepository.deleteSeries(series)
    }

    fun deleteCredit(credit: Credit) {
        issueRepository.deleteCredit(credit)
    }

    fun upsertCover(cover: Cover) {
        issueRepository.saveCover(cover)
    }

    fun addToCollection() {
        (variantIdLiveData.value
            ?: issueIdLiveData.value)?.let { issueRepository.addToCollection(it) }
    }

    fun removeFromCollection() {
        (variantIdLiveData.value
            ?: issueIdLiveData.value)?.let {
            issueRepository.removeFromCollection(it)
        }
    }
}