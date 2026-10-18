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
import dev.benica.infinite_longbox.repository.pickedCoverToIssue
import dev.benica.infinite_longbox.repository.removeCover
import dev.benica.infinite_longbox.ui.InfiniteLongboxTheme
import dev.benica.infinite_longbox.ui.IssueActionState
import dev.benica.infinite_longbox.ui.IssueDetailActions
import dev.benica.infinite_longbox.ui.IssueDetailScreen
import dev.benica.infinite_longbox.ui.IssueDetailUiState
import dev.benica.infinite_longbox.ui.IssueCoverState
import dev.benica.infinite_longbox.ui.IssueInfoState
import dev.benica.infinite_longbox.ui.JumpBarState
import dev.benica.infinite_longbox.ui.VariantSelectorState
import dev.benica.infinite_longbox.ui.buildStoryUi
import dev.benica.infinite_longbox.ui.placeholderKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import java.io.File

// Bundle Argument Tags
private const val ARG_ISSUE_ID = "issue_id"
private const val ARG_VARIANT_OF = "variant_of"

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
    private var listFragmentCallback: ListFragment.ListFragmentCallback? = null

    private var fullIssue: FullIssue = FullIssue.getEmptyFullIssue()
    private var fullVariant: FullIssue? = null
    private var issuesInSeries: List<Int> = emptyList()
    private var currentPos: Int = 0
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
            getString(R.string.cover_remove),
        )
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.cover_options_title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> launchCoverPicker()
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
        listFragmentCallback = context as ListFragment.ListFragmentCallback?
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
    ): View? {

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                InfiniteLongboxTheme {
                    IssueDetailScreen(
                        state = uiState,
                        actions = IssueDetailActions(
                            onCoverClick = ::onCoverClicked,
                            onCoverLongClick = ::showCoverOptions,
                            onToggleCollection = {
                                toggleCollection(BaseCollection.MY_COLL.id, uiState.actions.inCollection)
                            },
                            onToggleWishList = {
                                toggleCollection(BaseCollection.WISH_LIST.id, uiState.actions.inWishList)
                            },
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
            val issueIds = issues.map { it.issue.issueId }
            if (issuesInSeries != issueIds) {
                issuesInSeries = issueIds
                this.currentPos = this.issuesInSeries.indexOf(this.fullIssue.issue.issueId)
                updateNavBar()
            }
        }

        issueDetailViewModel.primaryIssue.observe(
            viewLifecycleOwner
        ) {
            it?.let { issue ->
                if (fullIssue != issue) {
                    fullIssue = issue
                    updateUI()
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

    private fun jumpToIssue(skipNum: Int) {
        currentPos += skipNum
        // The selected variant belongs to the issue being left; drop it so the new primary shows
        issueDetailViewModel.clearVariant()
        issueDetailViewModel.loadIssue(this.issuesInSeries[currentPos])
        updateNavBar()
    }

    private fun onVariantSelected(selected: Issue) {
        if (selected.issueId != issueDetailViewModel.primaryId.value) {
            issueDetailViewModel.loadVariant(selected.issueId)
        } else {
            issueDetailViewModel.clearVariant()
        }

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
            listFragmentCallback?.setTitle("$currentIssue")

            val collectionIds: List<Int> = currentIssue.collectionItems.map { it.userCollection }
            uiState = uiState.copy(
                info = IssueInfoState(issue.releaseDate, issue.coverDate, issue.notes),
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
            )

            updateCover()
        }
    }

    private fun updateCover() {
        uiState = uiState.copy(
            cover = IssueCoverState(coverUri?.path?.let { File(it) }, currentIssue.placeholderKey())
        )
    }

    private fun onCoverClicked() {
        val uri = coverUri
        if (uri != null) {
            CoverDialogFragment(uri, currentIssue).show(childFragmentManager, "cover_dialog")
        } else {
            launchCoverPicker()
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
        fun newInstance(
            issueSelectedId: Int? = null,
            variantOf: Int? = null,
        ): IssueDetailFragment =
            IssueDetailFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_ISSUE_ID, issueSelectedId)
                    putSerializable(ARG_VARIANT_OF, variantOf)
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
