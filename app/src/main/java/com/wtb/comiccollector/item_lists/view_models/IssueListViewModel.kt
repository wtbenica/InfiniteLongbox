package com.wtb.comiccollector.item_lists.view_models

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.wtb.comiccollector.Filter
import com.wtb.comiccollector.database.Daos.REQUEST_LIMIT
import com.wtb.comiccollector.database.models.FullIssue
import com.wtb.comiccollector.database.models.Issue
import com.wtb.comiccollector.database.models.Series
import com.wtb.comiccollector.repository.Repository
import kotlinx.coroutines.flow.Flow

private const val TAG = "NewIssueListViewModel"

class IssueListViewModel : ViewModel() {
    private val repository: Repository = Repository.get()
    private val seriesIdLiveData = MutableLiveData<Int>()
    private val filterLiveData = MutableLiveData(Filter())

    var seriesLiveData: LiveData<Series?> =
        Transformations.switchMap(filterLiveData) {
            it?.let { filter ->
                filter.mSeries?.seriesId?.let { id -> repository.getSeries(id) }
            }
        }

    fun issueList(): Flow<PagingData<FullIssue>>? {
        val filterValue = this.filterLiveData.value

        return filterValue?.let { filter ->
            Pager(
                config = PagingConfig(
                    pageSize = REQUEST_LIMIT,
                    enablePlaceholders = true,
                    maxSize = 200
                )
            ) {
                repository.getIssuesByFilterPagingSource(filter)
            }.flow
        }
    }

    //    val issueList: LiveData<PagingData<FullIssue>> = Transformations.switchMap(filterLiveData)
//    {
//        it?.let { filter ->
//            Pager(
//                config = PagingConfig(
//                    pageSize = REQUEST_LIMIT,
//                    enablePlaceholders = true,
//                    maxSize = 200
//                )
//            ) {
//                repository.getIssuesByFilterPagingSource(filter)
//            }.liveData
//        }
//    }
//
    fun setFilter(filter: Filter) {
        filterLiveData.value = filter.clone()
    }

    fun addIssue(issue: Issue) {
        Log.d(TAG, "addIssue")
        repository.saveIssue(issue)
    }

    fun updateIssue(issue: FullIssue?) {
        repository.updateIssue(issue)
    }
}