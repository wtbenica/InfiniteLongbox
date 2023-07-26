/*
 * Copyright (c) 2023. Wesley T. Benica
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wtb.comiccollector.fragments_view_models

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi

private const val TAG = "SeriesInfoViewModel"

/**
 * Currently unused, but if the ability to edit/enter series info is put back, this is the
 * view model to do it, along with [com.wtb.comiccollector.SeriesInfoDialogFragment]
 */
@ExperimentalCoroutinesApi
class SeriesInfoViewModel : ViewModel() {

//    private val repository: Repository = Repository.get()
//
//    private val seriesIdLiveData = MutableLiveData<Int>()
//
//    var seriesLiveData: LiveData<FullSeries?> = seriesIdLiveData.switchMap { seriesId ->
//        repository.getSeries(seriesId).asLiveData()
//    }
//
//    var allPublishersLiveData: LiveData<List<Publisher>> = repository.allPublishers.asLiveData()
//
//    fun updateSeries(series: Series) {
//        repository.saveSeries(series)
//    }
//
//    fun addSeries(series: Series) {
//        // TODO: Check if series exists
//        repository.saveSeries(series)
//    }
//
//    fun deleteSeries(series: Series) {
//        repository.deleteSeries(series)
//    }
//
//    fun loadSeries(seriesId: Int) {
//        seriesIdLiveData.value = seriesId
//    }
}