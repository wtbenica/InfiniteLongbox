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

package dev.benica.infinite_longbox.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import dev.benica.infinite_longbox.nav.Route
import dev.benica.infinite_longbox.nav.Screen
import dev.benica.infinite_longbox.nav.ScreenHost
import dev.benica.infinite_longbox.ui.dialogs.ChoiceDialog
import dev.benica.infinite_longbox.ui.dialogs.MyCopyDialog
import dev.benica.infinite_longbox.ui.dialogs.showDialog
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import dev.benica.infinite_longbox.APP
import dev.benica.infinite_longbox.R
import dev.benica.infinite_longbox.SearchFilter
import dev.benica.infinite_longbox.database.models.AUTO_ID
import dev.benica.infinite_longbox.database.models.BaseCollection
import dev.benica.infinite_longbox.database.models.Creator
import dev.benica.infinite_longbox.database.models.FullAppearance
import dev.benica.infinite_longbox.database.models.FullCharacter
import dev.benica.infinite_longbox.database.models.FullCredit
import dev.benica.infinite_longbox.database.models.FullIssue
import dev.benica.infinite_longbox.database.models.Issue
import dev.benica.infinite_longbox.database.models.NameDetailAndCreator
import dev.benica.infinite_longbox.database.models.Publisher
import dev.benica.infinite_longbox.database.models.Series
import dev.benica.infinite_longbox.database.models.SeriesAndPublisher
import dev.benica.infinite_longbox.database.models.Story
import dev.benica.infinite_longbox.fragments_view_models.IssueDetailViewModel
import dev.benica.infinite_longbox.repository.MetronCoverOption
import dev.benica.infinite_longbox.repository.MetronCoverResult
import dev.benica.infinite_longbox.repository.Repository
import dev.benica.infinite_longbox.repository.fetchCoverFromMetron
import dev.benica.infinite_longbox.repository.metronCoverOptions
import dev.benica.infinite_longbox.repository.metronCoverPreview
import dev.benica.infinite_longbox.repository.pickedCoverToIssue
import dev.benica.infinite_longbox.repository.removeCover
import dev.benica.infinite_longbox.ui.covers.CoverImages
import dev.benica.infinite_longbox.ui.covers.CoverViewer
import dev.benica.infinite_longbox.ui.covers.IssueCoverState
import dev.benica.infinite_longbox.ui.covers.MetronCoverPicker
import dev.benica.infinite_longbox.ui.covers.ViewerCover
import dev.benica.infinite_longbox.ui.covers.placeholderKey
import dev.benica.infinite_longbox.ui.issue.IssueActionState
import dev.benica.infinite_longbox.ui.issue.IssueDetailActions
import dev.benica.infinite_longbox.ui.issue.IssueDetailScreen
import dev.benica.infinite_longbox.ui.issue.IssueDetailUiState
import dev.benica.infinite_longbox.ui.issue.IssueInfoState
import dev.benica.infinite_longbox.ui.issue.IssueNumberText
import dev.benica.infinite_longbox.ui.issue.JumpBarState
import dev.benica.infinite_longbox.ui.issue.MyCopyState
import dev.benica.infinite_longbox.ui.issue.SignerRef
import dev.benica.infinite_longbox.ui.issue.VariantSelectorState
import dev.benica.infinite_longbox.ui.issue.buildStoryUi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

// Bundle Argument Tags

/** An issue's page: its details, credits and covers, with the jump bar through its series. */
@ExperimentalCoroutinesApi
class IssuePage(host: ScreenHost) : Screen(host) {
    private val listFragmentCallback: ListFragmentCallback get() = host.activity

    private var fullIssue: FullIssue = FullIssue.getEmptyFullIssue()
    private var fullVariant: FullIssue? = null
    /** What the jump bar goes through: the series, or what's in the collection it was opened from. */
    private class Stop(
        val issueId: Int,
        val variantOf: Int?,
        val cover: File?,
        val title: String,
        val variantName: String,
        val placeholderKey: String,
    )

    private var issuesInSeries: List<Stop> = emptyList()
    private var currentPos: Int = 0

    /** The stop being jumped to, until the page shows it. */
    private var jumpTarget: Int? = null
    private var issueCredits: List<FullCredit> = emptyList()
    private var issueStories: List<Story> = emptyList()
    private var issueAppearances: List<FullAppearance> = emptyList()
    private var variantCredits: List<FullCredit> = emptyList()
    private var variantStories: List<Story> = emptyList()
    private var variantAppearances: List<FullAppearance> = emptyList()
    private var issueVariants: List<Issue> = emptyList()

    private var uiState by mutableStateOf(IssueDetailUiState())
    private var isVariant: Boolean = false

    private val currentIssue: FullIssue
        get() = fullVariant ?: fullIssue

    private val coverUri: Uri?
        get() = currentIssue.coverUri

    private val issueDetailViewModel: IssueDetailViewModel by lazy { host.entry.viewModel<IssueDetailViewModel>() }

    /** Set when the page is composed: it needs the activity result registry. */
    private var pickCoverImage: ManagedActivityResultLauncher<Intent, ActivityResult>? = null

    private fun launchCoverPicker() {
        val request = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        pickCoverImage?.launch(Intent.createChooser(request, getString(R.string.cover_choose_title)))
    }

    /** The cover button on an issue with none: from Metron (through the server), or from this device. */
    private fun showAddCoverOptions() {
        val options = arrayOf(
            getString(R.string.cover_from_metron),
            getString(R.string.cover_from_device),
        )
        activity.showDialog { dismiss ->
            ChoiceDialog(
                R.string.cover_add_title, options.toList(), null,
                onChoose = { which ->
                    when (which) {
                        null -> Unit
                        0 -> fetchCoverFromMetron()
                        else -> launchCoverPicker()
                    }
                },
                dismiss = dismiss,
            )
        }
    }

    private var metronPicker by mutableStateOf<List<MetronCoverOption>?>(null)

    /**
     * Fetches the cover from Metron. For a variant it tries the variant Metron names the same way
     * and, failing that, shows what Metron has to pick from.
     */
    private fun fetchCoverFromMetron() {
        val issue = currentIssue.issue
        val context = context.applicationContext
        Toast.makeText(context, R.string.cover_metron_working, Toast.LENGTH_SHORT).show()
        viewLifecycleOwner.lifecycleScope.launch {
            val result = fetchCoverFromMetron(context, issue.issueId, issue.variantOf, issue.variantName)
            if ((result == MetronCoverResult.NONE || result == MetronCoverResult.AMBIGUOUS) && issue.variantOf != null) {
                chooseMetronCover()
            } else {
                Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun chooseMetronCover() {
        val issue = currentIssue.issue
        val options = metronCoverOptions(issue.issueId, issue.variantOf)
        when {
            options.isNullOrEmpty() ->
                Toast.makeText(context, R.string.cover_metron_none, Toast.LENGTH_SHORT).show()
            // One cover to choose from is no choice: it is the one asked for, and can be replaced
            options.size == 1 -> metronCoverPicked(options.first())
            else -> metronPicker = options
        }
    }

    private fun metronCoverPicked(option: MetronCoverOption) {
        metronPicker = null
        val issue = currentIssue.issue
        val context = context.applicationContext
        viewLifecycleOwner.lifecycleScope.launch {
            val result = fetchCoverFromMetron(context, issue.issueId, issue.variantOf, choice = option.id)
            Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun coverPicked(uri: Uri?) {
        uri ?: return
        val issueId = currentIssue.issue.issueId
        viewLifecycleOwner.lifecycleScope.launch {
            val message = pickedCoverToIssue(context.applicationContext, issueId, uri)
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    /** Long-press on an existing cover: replace it, or remove it. */
    private fun showCoverOptions() {
        val issueId = currentIssue.issue.issueId
        val options = arrayOf(
            getString(R.string.cover_replace),
            getString(R.string.cover_replace_from_metron),
            getString(R.string.cover_remove),
        )
        activity.showDialog { dismiss ->
            ChoiceDialog(
                R.string.cover_options_title, options.toList(), null,
                onChoose = { which ->
                    when (which) {
                        null -> Unit
                        0 -> launchCoverPicker()
                        1 -> fetchCoverFromMetron()
                        else -> viewLifecycleOwner.lifecycleScope.launch {
                            val message = removeCover(context.applicationContext, issueId)
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                dismiss = dismiss,
            )
        }
    }

    init {
        fullIssue =
            FullIssue(Issue(issueNumRaw = null), SeriesAndPublisher(Series(), Publisher()), null)
        issueCredits = emptyList()
        issueStories = emptyList()
        issueAppearances = emptyList()
        variantCredits = emptyList()
        variantStories = emptyList()

        val route = host.entry.route as Route.Issue
        val issueId = route.issueId
        val variantOf = route.variantOf

        // Once per page: coming back to it (from a character's page, say) finds it as it was left
        if (!issueDetailViewModel.started) {
            issueDetailViewModel.started = true
            issueDetailViewModel.setCollectionScope(route.collectionId)

            if (variantOf == null) {
                issueDetailViewModel.loadIssue(issueId)
                issueDetailViewModel.loadVariant(null)
            } else {
                issueDetailViewModel.showVariants()
                issueDetailViewModel.loadVariant(issueId)
                issueDetailViewModel.loadIssue(variantOf)
            }
        }
    }

    init {
        observeModel()
    }

    @Composable
    override fun Content() {
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) coverPicked(result.data?.data)
        }
        SideEffect { pickCoverImage = launcher }
        coverViewer?.let { (covers, start) ->
            Dialog(
                onDismissRequest = { closeCoverViewer(viewerPage) },
                properties = DialogProperties(usePlatformDefaultWidth = false),
            ) {
                CoverViewer(covers, start, onDismiss = ::closeCoverViewer, onPageChanged = { viewerPage = it })
            }
        }
        metronPicker?.let { options ->
            val issue = currentIssue.issue
            MetronCoverPicker(
                options = options,
                loadPreview = { metronCoverPreview(issue.issueId, issue.variantOf, it.id) },
                onPick = ::metronCoverPicked,
                onDismiss = { metronPicker = null },
            )
        }
        IssueDetailScreen(
            state = uiState,
            actions = IssueDetailActions(
                onCoverClick = ::onCoverClicked,
                onCoverLongClick = ::showCoverOptions,
                onToggleCollection = {
                    toggleCollection(
                        BaseCollection.MY_COLL.id,
                        uiState.actions.inCollection
                    )
                },
                onToggleWishList = {
                    toggleCollection(
                        BaseCollection.WISH_LIST.id,
                        uiState.actions.inWishList
                    )
                },
                onEditCopy = ::editMyCopy,
                onSignerClick = ::signerClicked,
                onSearchEbay = ::searchEbay,
                onVariantSelected = ::onVariantSelected,
                onCharacterClick = ::characterClicked,
                onCreatorClick = ::creatorClicked,
                onJump = ::jumpToIssue,
            ),
        )
    }

    private fun observeModel() {
        issueDetailViewModel.issueList.observe(
            viewLifecycleOwner
        ) { issues ->
            val stops = issues.map {
                Stop(
                    it.issue.issueId, it.issue.variantOf, it.coverUri?.path?.let { path -> File(path) },
                    it.toString(), it.issue.variantName, it.placeholderKey(),
                )
            }
            // A cover arriving changes a stop without changing which stops there are: the viewer
            // and the neighbours' prefetch read the covers from here, so it is kept up to date too
            val samePlaces = issuesInSeries.map { it.issueId } == stops.map { it.issueId }
            if (!samePlaces || issuesInSeries.map { it.cover?.path } != stops.map { it.cover?.path }) {
                issuesInSeries = stops
                if (!samePlaces) refreshPosition() else prefetchNeighbours()
            }
        }

        issueDetailViewModel.primaryIssue.observe(
            viewLifecycleOwner
        ) {
            it?.let { issue ->
                if (fullIssue != issue) {
                    fullIssue = issue
                    updateUI()
                    refreshPosition()
                }
            }
        }

        issueDetailViewModel.primaryCreditsLiveData.observe(
            viewLifecycleOwner
        ) { credits: List<FullCredit>? ->
            credits?.let {
                if (issueCredits != it) {
                    issueCredits = it
                    updateUI()
                }
            }
        }

        issueDetailViewModel.primaryStoriesLiveData.observe(
            viewLifecycleOwner
        ) { stories: List<Story>? ->
            stories?.let {
                if (issueStories != it) {
                    issueStories = it
                    updateUI()
                }
            }
        }

        issueDetailViewModel.primaryAppearancesLiveData.observe(
            viewLifecycleOwner
        ) { appearances: List<FullAppearance>? ->
            appearances?.let {
                if (issueAppearances != it) {
                    issueAppearances = it
                    updateUI()
                }
            }
        }

        issueDetailViewModel.variantLiveData.observe(
            viewLifecycleOwner
        ) {
            it.let { variant ->
                if (fullVariant != variant) {
                    fullVariant = variant
                    isVariant = (variant?.issue?.issueId ?: AUTO_ID) != AUTO_ID
                    updateUI()
                    refreshPosition()
                }
            }
        }

        issueDetailViewModel.variantCreditsLiveData.observe(
            viewLifecycleOwner
        ) { credits: List<FullCredit>? ->
            credits?.let {
                if (variantCredits != it) {
                    this.variantCredits = it
                    updateUI()
                }
            }
        }

        issueDetailViewModel.variantStoriesLiveData.observe(
            viewLifecycleOwner
        ) { stories: List<Story> ->
            stories.let {
                if (variantStories != it) {
                    this.variantStories = it
                    updateUI()
                }
            }
        }

        issueDetailViewModel.variantAppearancesLiveData.observe(
            viewLifecycleOwner
        ) { appearances: List<FullAppearance>? ->
            appearances?.let {
                if (variantAppearances != it) {
                    this.variantAppearances = it
                    updateUI()
                }
            }
        }

        issueDetailViewModel.variantsLiveData.observe(
            viewLifecycleOwner
        ) { issues: List<Issue>? ->
            issues?.let {
                if (issueVariants != it) {
                    this.issueVariants = it
                    updateUI()
                }
            }
        }

//        issueDetailViewModel.inCollectionLiveData.observe(
//            viewLifecycleOwner,
//
//        issueDetailViewModel.variantInCollectionLiveData.observe(
//            viewLifecycleOwner,
    }

    private fun updateNavBar() {
        uiState = uiState.copy(jumpBar = JumpBarState(currentPos, issuesInSeries.size))
    }

    /** Where the page's issue is among [issuesInSeries]: the cover shown, else its standard issue. */
    private fun positionOfCurrent(): Int {
        // Part-way through a jump the page still shows the issue being left: go by where it is going
        jumpTarget?.let { target ->
            if (currentIssue.issue.issueId == target) {
                jumpTarget = null
            } else {
                val at = issuesInSeries.indexOfFirst { it.issueId == target }
                if (at >= 0) return at
            }
        }
        val shown = issuesInSeries.indexOfFirst { it.issueId == currentIssue.issue.issueId }
        return if (shown >= 0) shown
        else issuesInSeries.indexOfFirst { it.issueId == fullIssue.issue.issueId }
    }

    /**
     * The issue (and cover) on the page arrive separately from the list the jump bar goes through,
     * in either order, so the position is worked out again whenever one of them does.
     */
    private fun refreshPosition() {
        currentPos = positionOfCurrent()
        updateNavBar()
        prefetchNeighbours()
    }

    /** The covers of the stops either side of this one are loaded before they're jumped to. */
    private fun prefetchNeighbours() {
        val context = context.applicationContext
        if (currentPos < 0) return
        for (i in (currentPos - 2)..(currentPos + 3)) {
            issuesInSeries.getOrNull(i)?.cover?.let { CoverImages.prefetchDetail(context, it) }
        }
    }

    private fun jumpToIssue(skipNum: Int) {
        currentPos = (currentPos + skipNum).coerceIn(0, issuesInSeries.lastIndex)
        val stop = issuesInSeries[currentPos]
        jumpTarget = stop.issueId
        if (stop.variantOf == null) {
            // The selected variant belongs to the issue being left; drop it so the new primary shows
            issueDetailViewModel.clearVariant()
            issueDetailViewModel.loadIssue(stop.issueId)
        } else {
            // A variant in the collection: its standard issue is the page, the variant is the cover
            issueDetailViewModel.loadVariant(stop.issueId)
            issueDetailViewModel.loadIssue(stop.variantOf)
        }
        updateNavBar()
    }

    private fun onVariantSelected(selected: Issue) {
        if (selected.issueId != issueDetailViewModel.primaryId.value) {
            // With a variant on the page its cover has to be among the ones the viewer goes through
            issueDetailViewModel.showVariants()
            issueDetailViewModel.loadVariant(selected.issueId)
        } else {
            issueDetailViewModel.clearVariant()
        }

        // The cover chosen may be one of the stops: the jump bar follows it
        issuesInSeries.indexOfFirst { it.issueId == selected.issueId }
            .takeIf { it >= 0 }?.let { currentPos = it; updateNavBar() }

        updateCover()
    }

    private fun searchEbay() {
        val category = 259104
        val url =
            "https://www.ebay.com/sch/?_sacat=$category&_nkw=${
                currentIssue.series.seriesName.replace(' ', '+')
            }+${currentIssue.issue.issueNumRaw}+${currentIssue.issue.variantName}+${
                currentIssue.series.startDate?.year ?: ""
            }"

        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    }

    private fun toggleCollection(collId: Int, isIn: Boolean) {
        if (isIn) removeFromCollection(collId) else addToCollection(collId)
    }

    private fun updateUI() {
        val issue: Issue = currentIssue.issue
        if (issue.issueId != AUTO_ID) {
            val series = currentIssue.series
            listFragmentCallback.setTitle(
                series.seriesName,
                IssueNumberText.subtitle(
                    issue.issueNumRaw, series.publishingFormat, series.issueCount, series.endDate != null
                ),
            )

            val collectionIds: List<Int> = currentIssue.collectionItems.map { it.userCollection }
            uiState = uiState.copy(
                info = IssueInfoState(
                    publisher = currentIssue.seriesAndPublisher.publisher.publisher,
                    releaseDate = issue.releaseDate,
                    coverDate = issue.coverDate,
                    notes = issue.notes
                ),
                actions = IssueActionState(
                    issueId = issue.issueId,
                    inCollection = collectionIds.contains(BaseCollection.MY_COLL.id),
                    inWishList = collectionIds.contains(BaseCollection.WISH_LIST.id),
                ),
                stories = buildStoryUi(
                    issueStories, variantStories, issueCredits, variantCredits,
                    issueAppearances, variantAppearances
                ),
                gcdIssueId = issue.issueId,
                variants = VariantSelectorState(issueVariants, issue.issueId),
                myCopy = myCopyState(),
            )
            followSigners(issue.issueId)

            updateCover()
        }
    }

    /** Who signed the copy of the issue on the page, kept up to date while the page is up. */
    private var signers: List<Creator> = emptyList()
    private var signersJob: Job? = null
    private var signersIssueId = -1

    private fun myCopyState(): MyCopyState? = currentIssue.collectionItems
        .firstOrNull { it.userCollection == BaseCollection.MY_COLL.id }
        ?.let { item ->
            MyCopyState(
                signers.map { SignerRef(it.creatorId, it.name) },
                item.storageLocation, item.condition, item.notes,
            )
        }

    private fun followSigners(issueId: Int) {
        if (issueId == signersIssueId) return
        signersIssueId = issueId
        signers = emptyList()
        signersJob?.cancel()
        signersJob = viewLifecycleOwner.lifecycleScope.launch {
            Repository.get().signers(issueId).collect {
                signers = it
                uiState = uiState.copy(myCopy = myCopyState())
            }
        }
    }

    private fun signerClicked(creatorId: Int) {
        val creator = signers.firstOrNull { it.creatorId == creatorId } ?: return
        activity.onCreatorInfo(creator)
    }

    /** Asks for what is kept about the copy in My Collection: who signed it, where it is, and so on. */
    private fun editMyCopy() {
        val current = uiState.myCopy ?: return
        val issueId = currentIssue.issue.issueId
        activity.showDialog { dismiss -> MyCopyDialog(issueId, current, dismiss) }
    }

    private fun updateCover() {
        uiState = uiState.copy(
            cover = IssueCoverState(coverUri?.path?.let { File(it) }, currentIssue.placeholderKey())
        )
    }

    /** The covers of the stops the jump bar goes through (or just this one), open at this one. */
    private var coverViewer by mutableStateOf<Pair<List<ViewerCover>, Int>?>(null)
    private var viewerPage = 0

    private fun openCoverViewer() {
        val here = currentPos
        viewerPage = here.coerceAtLeast(0)
        coverViewer = if (here >= 0 && issuesInSeries.isNotEmpty()) {
            // The page's own cover is the freshest word on this issue's
            val shown = currentIssue.issue.issueId
            val shownCover = coverUri?.path?.let { File(it) }
            issuesInSeries.map {
                val file = if (it.issueId == shown) shownCover else it.cover
                ViewerCover(it.issueId, it.title, it.variantName, file?.takeIf { f -> f.exists() }, it.placeholderKey)
            } to here
        } else {
            val issue = currentIssue
            listOf(
                ViewerCover(
                    issue.issue.issueId, issue.toString(), issue.issue.variantName,
                    coverUri?.path?.let { File(it) }?.takeIf { it.exists() }, issue.placeholderKey(),
                )
            ) to 0
        }
    }

    /** The viewer closed on cover [shown]: the page behind goes to that issue. */
    private fun closeCoverViewer(shown: Int) {
        coverViewer = null
        if (issuesInSeries.isNotEmpty() && currentPos >= 0 && shown != currentPos) jumpToIssue(shown - currentPos)
    }

    private fun onCoverClicked() {
        val uri = coverUri
        if (uri != null) {
            openCoverViewer()
        } else {
            showAddCoverOptions()
        }
    }

    private fun characterClicked(character: FullCharacter) {
        val filter = SearchFilter(
            character = character.character, myCollection = false,
            showVariants = true
        )
        listFragmentCallback.updateFilter(filter)
    }

    private fun creatorClicked(creator: NameDetailAndCreator) {
        val filter = SearchFilter(
            creators = setOf(creator.creator), myCollection = false,
            showVariants = true
        )
        listFragmentCallback.updateFilter(filter)
    }

    companion object {
        private const val TAG = APP + "IssuePage"
    }

    private fun addToCollection(collId: Int) = issueDetailViewModel.addToCollection(collId)

    private fun removeFromCollection(collId: Int) {
        Log.d(TAG, "REMOVING FROM COLLECTION $fullIssue $fullVariant")
        issueDetailViewModel.removeFromCollection(collId)
    }

}
