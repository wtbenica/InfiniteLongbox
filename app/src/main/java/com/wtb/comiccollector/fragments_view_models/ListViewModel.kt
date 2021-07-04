package com.wtb.comiccollector.fragments_view_models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.wtb.comiccollector.SearchFilter
import com.wtb.comiccollector.repository.Repository
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
open class ListViewModel : ViewModel() {
    protected val repository: Repository = Repository.get()
    protected val filterLiveData = MutableLiveData(SearchFilter())
    val filter: LiveData<SearchFilter> = filterLiveData
}