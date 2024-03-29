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

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TableRow
import androidx.appcompat.widget.AppCompatImageButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
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
import dev.benica.infinite_longbox.views.AddCollectionButton
import dev.benica.infinite_longbox.views.AddWishListButton
import dev.benica.infinite_longbox.views.CreditsBox
import dev.benica.infinite_longbox.views.IssueInfoBox
import dev.benica.infinite_longbox.views.WebLink
import kotlinx.coroutines.ExperimentalCoroutinesApi

// Bundle Argument Tags
private const val ARG_ISSUE_ID = "issue_id"
private const val ARG_EDITABLE = "open_as_editable"
private const val ARG_VARIANT_OF = "variant_of"

// onActivityResult Request Codes
private const val PICK_COVER_IMAGE = 0
internal const val RESULT_DATE_PICKER_END = "result_date_picker_start"
internal const val RESULT_DATE_PICKER_START = "result_date_picker_end"
private const val RESULT_SERIES_DETAIL = 108
private const val RESULT_NEW_WRITER = 109
private const val RESULT_NEW_PENCILLER = 110
private const val RESULT_NEW_INKER = 111

// Fragment Tags
private const val DIALOG_SERIES_DETAIL = "DialogNewSeries"
private const val DIALOG_NEW_CREATOR = "DialogNewCreator"
internal const val DIALOG_DATE = "DialogDate"

private const val ADD_SERIES_ID = -2

/**
 * A simple [Fragment] subclass.
 * Use the [IssueDetailFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
// TODO: Do I need a separate fragment for editing vs viewing or can I do it all in this one?
@ExperimentalCoroutinesApi
class IssueDetailFragment : Fragment(), CreditsBox.CreditsBoxCallback,
    AddCollectionButton.AddCollectionCallback {

    private var numUpdates = 0
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

    private lateinit var coverImageView: ImageButton
    private lateinit var ebayButton: AppCompatImageButton
    private lateinit var collectionButton: AddCollectionButton
    private lateinit var wishListButton: AddWishListButton
    private lateinit var variantSpinnerHolder: LinearLayout
    private lateinit var variantSpinner: Spinner
    private var isVariant: Boolean = false

    private lateinit var issueCreditsFrame: FrameLayout
    private lateinit var creditsBox: CreditsBox

    private lateinit var infoBox: IssueInfoBox

    private lateinit var gotoStartButton: Button
    private lateinit var gotoSkipBackButton: Button
    private lateinit var gotoPreviousButton: Button
    private lateinit var gotoNextButton: Button
    private lateinit var gotoSkipForwardButton: Button
    private lateinit var gotoEndButton: Button

    private lateinit var gcdLinkButton: WebLink

    private val currentIssue: FullIssue
        get() = fullVariant ?: fullIssue

    private val coverUri: Uri?
        get() = currentIssue.coverUri

    private var saveIssue = true

    private val issueDetailViewModel: IssueDetailViewModel by viewModels()

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
        setHasOptionsMenu(true)

        fullIssue =
            FullIssue(Issue(issueNumRaw = null), SeriesAndPublisher(Series(), Publisher()), null)
        issueCredits = emptyList()
        issueStories = emptyList()
        issueAppearances = emptyList()
        variantCredits = emptyList()
        variantStories = emptyList()

        val issueId = if (VERSION.SDK_INT >= TIRAMISU) {
            arguments?.getSerializable(ARG_ISSUE_ID, Int::class.java)!!
        } else {
            arguments?.getSerializable(ARG_ISSUE_ID) as Int
        }
        val variantOf = if (VERSION.SDK_INT >= TIRAMISU) {
            arguments?.getSerializable(ARG_VARIANT_OF, Int::class.java)
        } else {
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

        val view = inflater.inflate(R.layout.fragment_issue_detail, container, false)

        coverImageView = view.findViewById<ImageButton>(R.id.issue_cover).apply {
            setOnClickListener {
                CoverDialogFragment(this.drawable, this@IssueDetailFragment.currentIssue).show(
                    childFragmentManager,
                    "cover_dialog"
                )
            }
        }
        issueCreditsFrame = view.findViewById(R.id.issue_credits_frame) as FrameLayout
        infoBox = view.findViewById(R.id.issue_info_box)
        gcdLinkButton = view.findViewById(R.id.gcd_link)
        ebayButton = view.findViewById(R.id.ebayButton)
        collectionButton =
            (view.findViewById(R.id.collectionButton) as AddCollectionButton).apply {
                callback = this@IssueDetailFragment
            }
        wishListButton =
            (view.findViewById(R.id.issue_add_wish_btn) as AddWishListButton).apply {
                callback = this@IssueDetailFragment
            }
        variantSpinnerHolder = view.findViewById(R.id.variant_spinner_holder)
        variantSpinner = view.findViewById(R.id.variant_spinner) as Spinner
        creditsBox = CreditsBox(requireContext(), this@IssueDetailFragment)
        issueCreditsFrame.addView(creditsBox)

        gotoStartButton = view.findViewById(R.id.goto_start_button) as Button
        gotoSkipBackButton = view.findViewById(R.id.goto_skip_back_button) as Button
        gotoPreviousButton = view.findViewById(R.id.goto_previous_button) as Button
        gotoNextButton = view.findViewById(R.id.goto_next_button) as Button
        gotoSkipForwardButton = view.findViewById(R.id.goto_skip_forward_button) as Button
        gotoEndButton = view.findViewById(R.id.goto_end_button) as Button

        return view
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
                    isVariant = fullVariant?.issue?.issueId != AUTO_ID
                    fullVariant = variant
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
                    val adapter = ArrayAdapter(
                        requireContext(),
                        R.layout.spinner_item_variant,
                        R.id.variant_name_text,
                        it
                    )
                    this.issueVariants = it
                    variantSpinner.adapter = adapter
                    variantSpinnerHolder.visibility = if (it.size <= 1) {
                        View.GONE
                    } else {
                        View.VISIBLE
                    }
                    updateUI()
                }
            }
        }

//        issueDetailViewModel.inCollectionLiveData.observe(
//            viewLifecycleOwner,
//            { count ->
//                if (!isVariant) {
//                    setCollectionButton(count)
//                }
//            }
//        )
//
//        issueDetailViewModel.variantInCollectionLiveData.observe(
//            viewLifecycleOwner,
//            { count ->
//                if (isVariant) {
//                    setCollectionButton(count)
//                }
//            }
//        )
    }

    override fun onDetach() {
        super.onDetach()

        listFragmentCallback = null
    }

    private fun updateNavBar() {
        val found = currentPos >= 0 && issuesInSeries.isNotEmpty()

        this.gotoStartButton.isEnabled = (currentPos > 0) && found
        this.gotoSkipBackButton.isEnabled = (currentPos >= 10) && found
        this.gotoPreviousButton.isEnabled = (currentPos > 0) && found
        this.gotoNextButton.isEnabled = (currentPos < this.issuesInSeries.size - 1) && found
        this.gotoSkipForwardButton.isEnabled =
            (currentPos <= this.issuesInSeries.size - 11) && found
        this.gotoEndButton.isEnabled = (currentPos < this.issuesInSeries.size - 1) && found
    }

    private fun jumpToIssue(skipNum: Int) {
        currentPos += skipNum
        issueDetailViewModel.loadIssue(this.issuesInSeries[currentPos])
        updateNavBar()
    }

    override fun onStart() {
        super.onStart()
        gcdLinkButton.url = { "https://www.comics.org/issue/${currentIssue.issue.issueId}" }

        ebayButton.setOnClickListener {
            val category = 259104
            val url =
                "https://www.ebay.com/sch/?_sacat=$category&_nkw=${
                    currentIssue.series.seriesName.replace(' ', '+')
                }+${currentIssue.issue.issueNumRaw}+${currentIssue.issue.variantName}+${
                    currentIssue.series.startDate?.year ?: ""
                }"

            val intent = Intent().apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse(url)
            }
            context?.startActivity(intent)
        }

        gotoStartButton.setOnClickListener {
            jumpToIssue(-currentPos)
        }

        gotoSkipBackButton.setOnClickListener {
            jumpToIssue(-10)
        }

        gotoPreviousButton.setOnClickListener {
            jumpToIssue(-1)
        }

        gotoNextButton.setOnClickListener {
            jumpToIssue(1)
        }

        gotoSkipForwardButton.setOnClickListener {
            jumpToIssue(10)
        }

        gotoEndButton.setOnClickListener {
            jumpToIssue(issuesInSeries.size - currentPos - 1)
        }

//         TODO: This is for editing. n/a anymore?
//        releaseDateTextView.setOnClickListener {
//            DatePickerFragment.newInstance(fullIssue.issue.releaseDate).apply {
//                setTargetFragment(this@IssueDetailFragment, RESULT_DATE_PICKER)
//                show(this@IssueDetailFragment.parentFragmentManager, DIALOG_DATE)
//            }
//        }
//

        val touchSelectListener =
            object : AdapterView.OnItemSelectedListener, View.OnTouchListener {

                private var userSelect = false

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                    if (userSelect) {
                        parent?.let {
                            val selectedIssueId =
                                (it.getItemAtPosition(position) as Issue).issueId

                            val selectionIsVariant =
                                selectedIssueId != issueDetailViewModel.primaryId.value

                            if (selectionIsVariant) {
                                issueDetailViewModel.loadVariant(selectedIssueId)
                            } else {
                                issueDetailViewModel.clearVariant()
                            }

                            updateCover()
                        }

                        userSelect = false
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {

                }

                override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                    userSelect = true
                    return false
                }
            }
        variantSpinner.onItemSelectedListener = touchSelectListener
        variantSpinner.setOnTouchListener(touchSelectListener)
    }

    ////    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
////        super.onCreateOptionsMenu(menu, inflater)
////        inflater.inflate(R.menu.fragment_issue, menu)
////    }
////
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        return when (item.itemId) {
//            R.id.delete_issue -> {
//                saveIssue = false
//                issueDetailViewModel.delete(fullIssue.issue)
//                requireActivity().onBackPressed()
//                true
//            }
//            else -> super.onOptionsItemSelected(item)
//        }
//    }
//
    private var i = 0

    private fun updateUI() {
        val issue: Issue = currentIssue.issue
        if (issue.issueId != AUTO_ID) {
            listFragmentCallback?.setTitle("$currentIssue")

            // TODO: Marked for deletion: 10/22/21
            if ((!isVariant && (issueStories.isEmpty() || issueCredits.isEmpty() || issueAppearances.isEmpty())) ||
                (isVariant && (variantStories.isEmpty() || variantCredits.isEmpty() || variantAppearances.isEmpty()))
            ) {
                // Not sure whether my intention was to give some sort of indication that there
                // was no info or whether this was before I had implemented progress bars
                Unit
            }

            infoBox.update(issue.releaseDate, issue.coverDate, issue.notes)
            val collectionIds: List<Int> = currentIssue.collectionItems.map { it.userCollection }
            collectionButton.inCollection = collectionIds.contains(BaseCollection.MY_COLL.id)
            wishListButton.inCollection = collectionIds.contains(BaseCollection.WISH_LIST.id)

            creditsBox.update(
                issueStories = issueStories,
                variantStories = variantStories,
                issueCredits = issueCredits,
                variantCredits = variantCredits,
                issueAppearances = issueAppearances,
                variantAppearances = variantAppearances
            )

            issue.let {
                val indexOf = issueVariants.indexOf(it)
                variantSpinner.setSelection(indexOf)
            }

            updateCover()
        }
    }

    private fun updateCover() {
        if (coverUri != null) {
            coverImageView.apply {
                setImageURI(coverUri)
                isClickable = true
            }
        } else {
            coverImageView.apply {
                setImageResource(R.drawable.issue_cover_missing)
                isClickable = false
            }
        }
    }

    override fun characterClicked(character: FullCharacter) {
        val filter = SearchFilter(
            character = character.character, myCollection = false,
            showVariants = true
        )
        listFragmentCallback?.updateFilter(filter)
    }

    override fun creatorClicked(creator: NameDetailAndCreator) {
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

    override fun addToCollection(collId: Int) = issueDetailViewModel.addToCollection(collId)

    override fun removeFromCollection(collId: Int) {
        Log.d(TAG, "REMOVING FROM COLLECTION $fullIssue $fullVariant")
        issueDetailViewModel.removeFromCollection(collId)
    }

}

class RoleNameTextView(context: Context) :
    androidx.appcompat.widget.AppCompatTextView(context) {

    constructor(context: Context, role: String = "") : this(context) {
        text = role.replaceFirstChar { it.uppercase() }
    }

    init {
        layoutParams = TableRow.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        setPaddingRelative(
            resources.getDimension(R.dimen.margin_wide).toInt(),
            resources.getDimension(R.dimen.margin_narrow).toInt(),
            resources.getDimension(R.dimen.margin_wide).toInt(),
            resources.getDimension(R.dimen.margin_narrow).toInt()
        )
        setTextAppearance(R.style.RoleNameText)
    }
}
