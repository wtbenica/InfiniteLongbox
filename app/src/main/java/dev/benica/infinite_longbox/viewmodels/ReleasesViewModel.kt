package dev.benica.infinite_longbox.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.benica.infinite_longbox.Notices
import dev.benica.infinite_longbox.repository.LoadTracker
import dev.benica.infinite_longbox.repository.Repository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.YearMonth

/** The release timeline: the month on show, and what came out in it. */
@OptIn(ExperimentalCoroutinesApi::class)
class ReleasesViewModel : ViewModel() {
    private val _month = MutableStateFlow(YearMonth.now())
    val month: StateFlow<YearMonth> = _month

    private val _publishers = MutableStateFlow<Set<Int>>(emptySet())

    /** The publishers the month is narrowed to; empty is all of them. */
    val publishers: StateFlow<Set<Int>> = _publishers

    /** The month's issues, narrowed to the picked publishers; null while they load. */
    val view: Flow<ReleasesView?> = _month.flatMapLatest { month ->
        Repository.get().releasesIn(month).combine(_publishers) { rows, picked -> releasesView(rows, picked) as ReleasesView? }
    }

    fun setPublishers(ids: Set<Int>) {
        _publishers.value = ids
    }

    private val _loading = MutableStateFlow(true)

    /** True while the month's issues are being brought from the server. */
    val loading: StateFlow<Boolean> = _loading

    init {
        viewModelScope.launch {
            _month.collectLatest { month ->
                _loading.value = true
                try {
                    if (!LoadTracker.track { Repository.get().fetchReleases(month) }) Notices.show("Couldn't reach the server; showing what's on the phone")
                } finally {
                    _loading.value = false
                }
            }
        }
    }

    fun setMonth(month: YearMonth) {
        _month.value = month
    }

    fun step(months: Long) {
        _month.value = _month.value.plusMonths(months)
    }
}
