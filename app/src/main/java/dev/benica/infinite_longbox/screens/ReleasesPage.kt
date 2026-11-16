package dev.benica.infinite_longbox.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.benica.infinite_longbox.R
import dev.benica.infinite_longbox.nav.Screen
import dev.benica.infinite_longbox.nav.ScreenHost
import dev.benica.infinite_longbox.ui.releases.ReleasesScreen
import dev.benica.infinite_longbox.viewmodels.ReleasesViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi

/** The releases page: a month at a time, its issues under the weeks they came out in. */
@ExperimentalCoroutinesApi
class ReleasesPage(host: ScreenHost) : Screen(host) {
    private val viewModel: ReleasesViewModel by lazy { host.entry.viewModel<ReleasesViewModel>() }

    private val callback: ReleasesCallback get() = host.activity

    @Composable
    override fun Content() {
        val month by viewModel.month.collectAsState()
        val loading by viewModel.loading.collectAsState()
        val sections by viewModel.sections.collectAsState(null)
        ReleasesScreen(
            month = month,
            sections = sections,
            loading = loading,
            onStep = viewModel::step,
            onPick = viewModel::setMonth,
            onIssue = { callback.openIssue(it) },
        )
    }

    override fun onShown() {
        activity.setTitle(getString(R.string.releases_title))
    }

    interface ReleasesCallback {
        fun openIssue(issueId: Int)
    }
}
