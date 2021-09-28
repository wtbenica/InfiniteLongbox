package com.wtb.comiccollector.fragments_view_models

import android.os.Parcelable
import androidx.lifecycle.*
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.SearchFilter
import com.wtb.comiccollector.database.models.AUTO_ID
import com.wtb.comiccollector.database.models.FullIssue
import com.wtb.comiccollector.database.models.FullSeries
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest

private const val TAG = APP + "IssueListViewModel"

@ExperimentalCoroutinesApi
class IssueListViewModel : ListViewModel<FullIssue>() {

    private val _seriesId = MutableStateFlow(AUTO_ID)
    val seriesId: StateFlow<Int> = _seriesId

    var seriesLiveData: LiveData<FullSeries?> =
        seriesId.flatMapLatest { seriesId -> repository.getSeries(seriesId) }.asLiveData()

    override fun setFilter(filter: SearchFilter) {
        super.setFilter(filter)
        val series = filter.mSeries
        if (series != null) {
            _seriesId.value = series.series.seriesId
        }
    }

    override val itemList: Flow<PagingData<FullIssue>> = filter.switchMap { filter ->
        repository.getIssuesByFilterPaged(filter).asLiveData()
    }.asFlow().cachedIn(viewModelScope)

    fun updateIssueCover(issueId: Int) = repository.updateIssueCover(issueId)

    fun saveIssueListState(instanceState: Parcelable?) {
        repository.saveIssueListState = instanceState
    }

    fun getIssueListState() = repository.saveIssueListState
}