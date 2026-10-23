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

package dev.benica.infinite_longbox.fragments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.AppBarLayout
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
import dev.benica.infinite_longbox.ui.theme.InfiniteLongboxTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File

// Bundle Argument Tags
private const val ARG_ISSUE_ID = "issue_id"
private const val ARG_VARIANT_OF = "variant_of"
private const val ARG_COLLECTION_ID = "collection_id"
private const val NO_COLLECTION = -1

internal const val RESULT_DATE_PICKER_END = "result_date_picker_start"
internal const val RESULT_DATE_PICKER_START = "result_date_picker_end"
internal const val DIALOG_DATE = "DialogDate"

/**
 * A simple [Fragment] subclass.
 * Use the [IssueDetailFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
@ExperimentalCoroutinesApi
class IssueDetailFragment : Fragment() {
    private var listFragmentCallback: ListFragmentCallback? = null

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

    private val issueDetailViewModel: IssueDetailViewModel by viewModels()

    private val pickCoverImage =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) coverPicked(result.data?.data)
        }

    private fun launchCoverPicker() {
        val request = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        pickCoverImage.launch(Intent.createChooser(request, getString(R.string.cover_choose_title)))
    }

    /** The cover button on an issue with none: from Metron (through the server), or from this device. */
    private fun showAddCoverOptions() {
        val options = arrayOf(
            getString(R.string.cover_from_metron),
            getString(R.string.cover_from_device),
        )
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.cover_add_title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> fetchCoverFromMetron()
                    else -> launchCoverPicker()
                }
            }
            .show()
    }

    private var metronPicker by mutableStateOf<List<MetronCoverOption>?>(null)

    /**
     * Fetches the cover from Metron. For a variant it tries the variant Metron names the same way
     * and, failing that, shows what Metron has to pick from.
     */
    private fun fetchCoverFromMetron() {
        val issue = currentIssue.issue
        val context = requireContext().applicationContext
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
                Toast.makeText(requireContext(), R.string.cover_metron_none, Toast.LENGTH_SHORT).show()
            // One cover to choose from is no choice: it is the one asked for, and can be replaced
            options.size == 1 -> metronCoverPicked(options.first())
            else -> metronPicker = options
        }
    }

    private fun metronCoverPicked(option: MetronCoverOption) {
        metronPicker = null
        val issue = currentIssue.issue
        val context = requireContext().applicationContext
        viewLifecycleOwner.lifecycleScope.launch {
            val result = fetchCoverFromMetron(context, issue.issueId, issue.variantOf, choice = option.id)
            Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun coverPicked(uri: Uri?) {
        uri ?: return
        val issueId = currentIssue.issue.issueId
        viewLifecycleOwner.lifecycleScope.launch {
            val message = pickedCoverToIssue(requireContext().applicationContext, issueId, uri)
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
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
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.cover_options_title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> launchCoverPicker()
                    1 -> fetchCoverFromMetron()
                    else -> viewLifecycleOwner.lifecycleScope.launch {
                        val message = removeCover(requireContext().applicationContext, issueId)
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listFragmentCallback = context as ListFragmentCallback?
    }

    override fun onResume() {
        super.onResume()
        listFragmentCallback?.setToolbarScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_NO_SCROLL)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fullIssue =
            FullIssue(Issue(issueNumRaw = null), SeriesAndPublisher(Series(), Publisher()), null)
        issueCredits = emptyList()
        issueStories = emptyList()
        issueAppearances = emptyList()
        variantCredits = emptyList()
        variantStories = emptyList()

        val issueId = if (VERSION.SDK_INT >= TIRAMISU) {
            arguments?.getSerializable(ARG_ISSUE_ID, Int::class.javaObjectType)!!
        } else {
            @Suppress("DEPRECATION")
            arguments?.getSerializable(ARG_ISSUE_ID) as Int
        }

        val variantOf = if (VERSION.SDK_INT >= TIRAMISU) {
            arguments?.getSerializable(ARG_VARIANT_OF, Int::class.javaObjectType)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getSerializable(ARG_VARIANT_OF) as Int?
        }

        issueDetailViewModel.setCollectionScope(
            arguments?.getInt(ARG_COLLECTION_ID, NO_COLLECTION)?.takeIf { it != NO_COLLECTION }
        )

        if (variantOf == null) {
            issueDetailViewModel.loadIssue(issueId)
            issueDetailViewModel.loadVariant(null)
        } else {
            issueDetailViewModel.loadVariant(issueId)
            issueDetailViewModel.loadIssue(variantOf)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                InfiniteLongboxTheme {
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
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        issueDetailViewModel.issueList.observe(
            viewLifecycleOwner
        ) { issues ->
            val stops = issues.map {
                Stop(
                    it.issue.issueId, it.issue.variantOf, it.coverUri?.path?.let { path -> File(path) },
                    it.toString(), it.issue.variantName, it.placeholderKey(),
                )
            }
            if (issuesInSeries.map { it.issueId } != stops.map { it.issueId }) {
                issuesInSeries = stops
                refreshPosition()
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

    override fun onDetach() {
        super.onDetach()

        listFragmentCallback = null
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
        val context = context?.applicationContext ?: return
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

        context?.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    }

    private fun toggleCollection(collId: Int, isIn: Boolean) {
        if (isIn) removeFromCollection(collId) else addToCollection(collId)
    }

    private fun updateUI() {
        val issue: Issue = currentIssue.issue
        if (issue.issueId != AUTO_ID) {
            val series = currentIssue.series
            listFragmentCallback?.setTitle(
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
        (activity as? CreatorListFragment.CreatorListCallback)?.onCreatorInfo(creator)
    }

    /** Asks for what is kept about the copy in My Collection: who signed it, where it is, and so on. */
    private fun editMyCopy() {
        val current = uiState.myCopy ?: return
        val issueId = currentIssue.issue.issueId
        val context = requireContext()
        val pad = (16 * resources.displayMetrics.density).toInt()

        fun field(hint: Int, value: String?, multiline: Boolean = false) = android.widget.AutoCompleteTextView(context).apply {
            setHint(hint)
            setText(value.orEmpty())
            if (multiline) {
                inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
                minLines = 2
            } else {
                inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS
                setSingleLine()
            }
        }

        // Who signed it: creators, chosen from the same kind of dropdown as the filter's
        val chosen = signers.toMutableList()
        val chips = com.google.android.material.chip.ChipGroup(context)
        fun showChips() {
            chips.removeAllViews()
            chosen.forEach { creator ->
                chips.addView(com.google.android.material.chip.Chip(context).apply {
                    text = creator.name
                    isCloseIconVisible = true
                    setOnCloseIconClickListener { chosen.remove(creator); showChips() }
                })
            }
        }
        showChips()
        val signerSearch = android.widget.AutoCompleteTextView(context).apply {
            setHint(R.string.my_copy_signed_by)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS
            setSingleLine()
            setAdapter(FilterOptionsAdapter(context) { text ->
                runBlocking(Dispatchers.IO) { Repository.get().findCreators(text) }
            })
            setOnItemClickListener { parent, _, position, _ ->
                (parent.getItemAtPosition(position) as? Creator)?.let { creator ->
                    if (chosen.none { it.creatorId == creator.creatorId }) chosen.add(creator)
                    showChips()
                }
                setText("")
            }
        }
        val storage = field(R.string.my_copy_storage, current.storageLocation)
        val condition = field(R.string.my_copy_condition, current.condition)
        val notes = field(R.string.my_copy_notes, current.notes, multiline = true)

        // Places already used are suggested, so the same box isn't spelled two ways
        viewLifecycleOwner.lifecycleScope.launch {
            val places = Repository.get().storageLocations()
            storage.setAdapter(android.widget.ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, places))
            storage.threshold = 0
        }

        val form = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(pad, pad / 2, pad, 0)
            addView(chips)
            listOf(signerSearch, storage, condition, notes).forEach { addView(it) }
        }
        AlertDialog.Builder(context)
            .setTitle(R.string.my_copy_title)
            .setView(android.widget.ScrollView(context).apply { addView(form) })
            .setPositiveButton(R.string.my_copy_save) { _, _ ->
                Repository.get().setCopyInfo(
                    issueId, BaseCollection.MY_COLL.id,
                    storage.text.toString(), condition.text.toString(), notes.text.toString(),
                    chosen.map { it.creatorId },
                )
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
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
            issuesInSeries.map { ViewerCover(it.issueId, it.title, it.variantName, it.cover?.takeIf { f -> f.exists() }, it.placeholderKey) } to here
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
        listFragmentCallback?.updateFilter(filter)
    }

    private fun creatorClicked(creator: NameDetailAndCreator) {
        val filter = SearchFilter(
            creators = setOf(creator.creator), myCollection = false,
            showVariants = true
        )
        listFragmentCallback?.updateFilter(filter)
    }

    companion object {
        @JvmStatic
        /** [collectionId]: the collection the issue was opened from, so the jump bar stays in it. */
        fun newInstance(
            issueSelectedId: Int? = null,
            variantOf: Int? = null,
            collectionId: Int? = null,
        ): IssueDetailFragment =
            IssueDetailFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_ISSUE_ID, issueSelectedId)
                    putSerializable(ARG_VARIANT_OF, variantOf)
                    putInt(ARG_COLLECTION_ID, collectionId ?: NO_COLLECTION)
                }
            }

        private const val TAG = APP + "IssueDetailFragment"
    }

    private fun addToCollection(collId: Int) = issueDetailViewModel.addToCollection(collId)

    private fun removeFromCollection(collId: Int) {
        Log.d(TAG, "REMOVING FROM COLLECTION $fullIssue $fullVariant")
        issueDetailViewModel.removeFromCollection(collId)
    }

}
