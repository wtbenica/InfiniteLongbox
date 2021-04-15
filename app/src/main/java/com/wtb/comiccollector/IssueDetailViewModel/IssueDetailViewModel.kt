package com.wtb.comiccollector.IssueDetailViewModel

import androidx.lifecycle.*
import com.wtb.comiccollector.AUTO_ID
import com.wtb.comiccollector.Issue
import com.wtb.comiccollector.IssueAndSeries
import com.wtb.comiccollector.IssueRepository
import com.wtb.comiccollector.database.models.*

private const val TAG = "IssueDetailViewModel"

class IssueDetailViewModel : ViewModel() {

    private val issueRepository: IssueRepository = IssueRepository.get()
    private val issueIdLiveData = MutableLiveData<Int>()
    private val variantIdLiveData = MutableLiveData<Int?>()

    val issueLiveData: LiveData<IssueAndSeries> =
        Transformations.switchMap(issueIdLiveData) { issueId ->
            issueRepository.getIssue(issueId)
        }

    val issueStoriesLiveData: LiveData<List<Story>> =
        Transformations.switchMap(issueIdLiveData) { issueId ->
            issueRepository.getStoriesByIssue(issueId)
        }

    val issueCreditsLiveData: LiveData<List<FullCredit>> =
        Transformations.switchMap(issueIdLiveData) { issueId ->
            issueRepository.getCreditsByIssue(issueId)
        }

    val variantLiveData: LiveData<IssueAndSeries?> =
        Transformations.switchMap(variantIdLiveData) { issueId ->
            issueId?.let { issueRepository.getIssue(it) }
        }

    val variantStoriesLiveData: LiveData<List<Story>> =
        Transformations.switchMap(variantIdLiveData) { issueId ->
            if (issueId != null) {
                issueRepository.getStoriesByIssue(issueId)
            } else {
                liveData { emit(emptyList<Story>()) }
            }
        }

    val variantCreditsLiveData: LiveData<List<FullCredit>> =
        Transformations.switchMap(variantIdLiveData) { issueId ->
            if (issueId != null) {
                issueRepository.getCreditsByIssue(issueId)
            } else {
                liveData { emit(emptyList<FullCredit>()) }
            }
        }

    val variantsLiveData: LiveData<List<Issue>> =
        Transformations.switchMap(issueIdLiveData) { issueId ->
            issueRepository.getVariants(issueId)
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
}