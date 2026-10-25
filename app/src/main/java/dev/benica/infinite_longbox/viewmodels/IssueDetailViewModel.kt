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

package dev.benica.infinite_longbox.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dev.benica.infinite_longbox.*
import dev.benica.infinite_longbox.database.daos.IssueStop
import dev.benica.infinite_longbox.database.models.*
import dev.benica.infinite_longbox.repository.Repository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import dev.benica.infinite_longbox.filter.SearchFilter
import dev.benica.infinite_longbox.filter.SortType

private const val TAG = APP + "IssueDetailViewModel"

@ExperimentalCoroutinesApi
class IssueDetailViewModel : ViewModel() {
    /** The page has been given its issue; coming back to it must not give it again. */
    var started = false

    private val repository: Repository = Repository.get()

    private val _primaryId = MutableStateFlow(AUTO_ID)
    val primaryId: StateFlow<Int>
        get() = _primaryId

    private val _variantId = MutableStateFlow(AUTO_ID)
    private val variantId: StateFlow<Int>
        get() = _variantId

    private val mIssue: StateFlow<FullIssue?> = primaryId.flatMapLatest { id ->
        Log.d(TAG, "issueId changed: $id")
        repository.getIssue(id)
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = null
    )

    internal val primaryIssue: LiveData<FullIssue?> = mIssue.asLiveData()

    internal val primaryStoriesLiveData: LiveData<List<Story>> =
        primaryId.flatMapLatest { issueId -> repository.getStoriesByIssue(issueId) }.asLiveData()

    internal val primaryCreditsLiveData: LiveData<List<FullCredit>> =
        primaryId.flatMapLatest { issueId -> repository.getCreditsByIssue(issueId) }.asLiveData()

    internal val primaryAppearancesLiveData =
        primaryId.flatMapLatest { repository.getAppearancesByIssue(it) }.asLiveData()

    // Other parts rely on this possibly being null, which is why it's LiveData, instead of
    // StateFlow like 'issue'
    internal val variantLiveData: LiveData<FullIssue?> =
        variantId.flatMapLatest { id ->
            Log.d(TAG, "variantId changed, updating variantLiveData $id")
            repository.getIssue(id)
        }.asLiveData()

    internal val variantStoriesLiveData: LiveData<List<Story>> =
        variantId.flatMapLatest { issueId -> repository.getStoriesByIssue(issueId) }.asLiveData()

    internal val variantCreditsLiveData: LiveData<List<FullCredit>> =
        variantId.flatMapLatest { issueId -> repository.getCreditsByIssue(issueId) }.asLiveData()

    internal val variantAppearancesLiveData: LiveData<List<FullAppearance>> =
        variantId.flatMapLatest { issueId -> repository.getAppearancesByIssue(issueId) }
            .asLiveData()

    internal val variantsLiveData: LiveData<List<Issue>> =
        primaryId.flatMapLatest { id -> repository.getVariants(id) }.asLiveData()

    /** Which collection the page was opened from, if it was. */
    private val _collectionScope = MutableStateFlow<Int?>(null)

    fun setCollectionScope(collectionId: Int?) {
        _collectionScope.value = collectionId
    }

    /** The filter of the issue list the page was opened from; the jump bar goes through what it gives. */
    private val _listFilter = MutableStateFlow<SearchFilter?>(null)

    fun setListFilter(filter: SearchFilter?) {
        _listFilter.value = filter?.let { SearchFilter(it) }
    }

    /** The collection the gallery is limited to: the list's, or the one the page was opened from; none for an unfiltered list. */
    fun galleryCollectionId(): Int? {
        val filter = _listFilter.value
        return if (filter != null) filter.mCollectionId.takeIf { filter.mMyCollection } else _collectionScope.value
    }

    /** The issues the jump bar goes through, a variant standing for its issue, each with where its cover is. */
    internal val issueList: LiveData<List<IssueStop>> =
        combine(mIssue, _collectionScope, _listFilter) { issue, scope, filter -> Triple(issue, scope, filter) }
            .flatMapLatest { (fullIssue, scope, listFilter) ->
                val filter = listFilter ?: fullIssue?.let { seriesFilter(it, scope) }
                filter?.let { repository.getIssueStopsByFilter(it).map(::issuesOnly) } ?: emptyFlow()
            }.asLiveData()

    private fun seriesFilter(issue: FullIssue, scope: Int?): SearchFilter {
        val context = InfiniteLongboxApplication.context!!
        return SearchFilter(
            series = FullSeries(issue.series),
            myCollection = scope != null,
            collectionId = scope ?: BaseCollection.MY_COLL.id,
            showVariants = true,
            sortType = SortType(
                context.getString(R.string.sort_type_issue_number),
                context.getString(R.string.column_issue_num),
                "ie",
                SortType.SortOrder.ASC,
            )
        )
    }

    private fun issuesOnly(stops: List<IssueStop>): List<IssueStop> {
        val standard = stops.filter { it.variantOf == null }.associateBy { it.issueId }
        return stops.map { it.variantOf ?: it.issueId }.distinct()
            .map { id -> standard[id] ?: IssueStop(id, null, null) }
    }

    fun loadIssue(issueId: Int) {
        _primaryId.value = issueId
    }

    fun loadVariant(issueId: Int?) {
        Log.d(TAG, "Loading variant: $issueId")
        _variantId.value = issueId ?: AUTO_ID
    }

    fun clearVariant() {
        _variantId.value = AUTO_ID
    }

    private val currentIssue: FullIssue?
        get() = if (variantId.value == AUTO_ID) {
            Log.d(TAG, "Getting Current Issue - Primary")
            mIssue.value
        } else {
            Log.d(TAG, "Getting Current Issue - Variant")
            variantLiveData.value
        }


    fun addToCollection(collId: Int) {
        Log.d(
            TAG,
            "Adding ${currentIssue?.series?.seriesName} #${currentIssue?.issue?.issueNum} to " +
                    "collection #$collId"
        )
        currentIssue?.let { repository.addToCollection(it, collId) }
        currentIssue?.let { it.cover?.id?.let { cid -> repository.markCoverSave(cid) } }
    }


    fun removeFromCollection(collId: Int) {
        Log.d(TAG, "REMOVING FROM COLLECTION $currentIssue")
        currentIssue?.let { repository.removeFromCollection(it.issue.issueId, collId) }
        currentIssue?.let {
            it.cover?.id?.let { cid ->
                if (currentIssue?.collectionItems?.isEmpty() == true) {
                    repository.markCoverDelete(cid)
                }
            }
        }
    }


    fun upsert(dataModel: DataModel) {
        when (dataModel) {
            is Series -> repository.saveSeries(dataModel)
            is Creator -> repository.saveCreator(dataModel)
            is Credit -> repository.saveCredit(dataModel)
            is Issue -> repository.saveIssue(dataModel)
            is Role -> repository.saveRole(dataModel)
            else -> Unit
        }
    }

    fun delete(dataModel: DataModel) {
        when (dataModel) {
            is Issue -> repository.deleteIssue(dataModel)
            else -> Unit
        }
    }
}

