package com.wtb.comiccollector

import androidx.lifecycle.*

private const val TAG = "IssueDetailViewModel"

class IssueDetailViewModel : ViewModel() {

    private val issueRepository: IssueRepository = IssueRepository.get()
    private val issueIdLiveData = MutableLiveData<Int>()
    private val variantIdLiveData = MutableLiveData<Int?>()

    var fullIssueLiveData: LiveData<IssueAndSeries> =
        Transformations.switchMap(issueIdLiveData) { issueId ->
            issueRepository.getFullIssue(issueId)
        }

    var issueStoriesLiveData: LiveData<List<Story>> =
        Transformations.switchMap(issueIdLiveData) { issueId ->
            issueRepository.getStoriesByIssue(issueId)
        }

    var issueCreditsLiveData: LiveData<List<FullCredit>> =
        Transformations.switchMap(issueIdLiveData) { issueId ->
            issueRepository.getIssueCredits(issueId)
        }

    var variantStoriesLiveData: LiveData<List<Story>> =
        Transformations.switchMap(variantIdLiveData) { issueId ->
            if (issueId != null) {
                issueRepository.getStoriesByIssue(issueId)
            } else {
                liveData { emit(emptyList<Story>()) }
            }
        }

    var variantCreditsLiveData: LiveData<List<FullCredit>> =
        Transformations.switchMap(variantIdLiveData) { issueId ->
            if (issueId != null) {
                issueRepository.getIssueCredits(issueId)
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

    fun getIssue() = issueIdLiveData.value

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