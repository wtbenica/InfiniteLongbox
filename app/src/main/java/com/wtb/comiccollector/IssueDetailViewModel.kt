package com.wtb.comiccollector

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import java.util.*

private const val TAG = "IssueDetailViewModel"

class IssueDetailViewModel : ViewModel() {

    private val issueRepository: IssueRepository = IssueRepository.get()
    private val issueIdLiveData = MutableLiveData<UUID>()

    var fullIssueLiveData: LiveData<IssueAndSeries?> =
        Transformations.switchMap(issueIdLiveData) { issueId ->
            issueRepository.getNewFullIssue(issueId)
        }

    var issueCreditsLiveData: LiveData<List<FullCredit>> =
        Transformations.switchMap(issueIdLiveData) { issueId ->
            issueRepository.getNewIssueCredits(issueId)
        }

    var allSeriesLiveData: LiveData<List<Series>> = issueRepository.allSeries

    var allPublishersLiveData: LiveData<List<Publisher>> = issueRepository.allPublishers

    var allCreatorsLiveData: LiveData<List<Creator>> = issueRepository.allCreators

    var allWritersLiveData: LiveData<List<Creator>> = issueRepository.allWriters

    var allRolesLiveData: LiveData<List<Role>> = issueRepository.allRoles

    fun loadIssue(issueId: UUID) {
        issueIdLiveData.value = NEW_SERIES_ID
        issueIdLiveData.value = issueId
    }

    fun updateIssue(issue: Issue) {
        issueRepository.updateIssue(issue)
    }

    fun deleteIssue(issue: Issue) {
        issueRepository.deleteIssue(issue)
    }

    fun updateSeries(series: Series) {
        issueRepository.updateSeries(series)
    }

    fun addSeries(series: Series) {
        // TODO: Check if series exists
        issueRepository.addSeries(series)
    }

    fun addCreator(creator: Creator) {
        issueRepository.addCreator(creator)
    }

    fun deleteSeries(series: Series) {
        issueRepository.deleteSeries(series)
    }

    fun addIssue(issue: Issue) {
        issueRepository.addIssue(issue)
    }

    fun updateCredit(credit: Credit) {
        issueRepository.updateCredit(credit)
    }

    fun addCredit(credit: Credit) {
        issueRepository.addCredit(credit)
    }
}