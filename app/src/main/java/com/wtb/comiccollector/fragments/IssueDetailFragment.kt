package com.wtb.comiccollector.fragments

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.appbar.AppBarLayout
import com.wtb.comiccollector.*
import com.wtb.comiccollector.database.daos.Count
import com.wtb.comiccollector.database.models.*
import com.wtb.comiccollector.fragments_view_models.IssueDetailViewModel
import com.wtb.comiccollector.views.IssueInfoBox
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

// Bundle Argument Tags
private const val ARG_ISSUE_ID = "issue_id"
private const val ARG_EDITABLE = "open_as_editable"
private const val ARG_VARIANT_OF = "variant_of"

// onActivityResult Request Codes
private const val PICK_COVER_IMAGE = 0
private const val RESULT_DATE_PICKER = 107
private const val RESULT_SERIES_DETAIL = 108
private const val RESULT_NEW_WRITER = 109
private const val RESULT_NEW_PENCILLER = 110
private const val RESULT_NEW_INKER = 111

// Fragment Tags
private const val DIALOG_SERIES_DETAIL = "DialogNewSeries"
private const val DIALOG_NEW_CREATOR = "DialogNewCreator"
private const val DIALOG_DATE = "DialogDate"

private const val ADD_SERIES_ID = -2

fun View.toggleVisibility(): Int {
    this.visibility = if (this.visibility == View.GONE) {
        View.VISIBLE
    } else {
        View.GONE
    }
    return this.visibility
}

fun ImageButton.toggleIcon(view: View) =
    this.setImageResource(if (view.visibility == LinearLayout.GONE) {
        R.drawable.arrow_down_24
    } else {
        R.drawable.arrow_up_24
    })

/**
 * A simple [Fragment] subclass.
 * Use the [IssueDetailFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
// TODO: Do I need a separate fragment for editing vs viewing or can I do it all in this one?
@ExperimentalCoroutinesApi
class IssueDetailFragment : Fragment(), CreditsBox.CreditsBoxCallback {

    private var numUpdates = 0
    private var listFragmentCallback: ListFragment.ListFragmentCallback? = null

    private lateinit var fullIssue: FullIssue
    private var fullVariant: FullIssue? = null
    private var issuesInSeries: List<Int> = emptyList()
    private var currentPos: Int = 0
    private lateinit var issueCredits: List<FullCredit>
    private lateinit var issueStories: List<Story>
    private lateinit var issueAppearances: List<FullAppearance>
    private lateinit var variantCredits: List<FullCredit>
    private lateinit var variantStories: List<Story>
    private var variantAppearances: List<FullAppearance> = emptyList()
    private lateinit var issueVariants: List<Issue>

    private lateinit var coverImageView: ImageView
    private lateinit var collectionButton: Button
    private lateinit var variantSpinnerHolder: LinearLayout
    private lateinit var variantSpinner: Spinner
    private var isVariant: Boolean = false

    //    private lateinit var issueCreditsLabel: TextView
    private lateinit var issueCreditsFrame: ScrollView
    private lateinit var creditsBox: CreditsBox

    private lateinit var infoBox: IssueInfoBox

    private lateinit var gotoStartButton: Button
    private lateinit var gotoSkipBackButton: Button
    private lateinit var gotoPreviousButton: Button
    private lateinit var gotoNextButton: Button
    private lateinit var gotoSkipForwardButton: Button
    private lateinit var gotoEndButton: Button

    private lateinit var gcdLinkButton: Button
    private lateinit var coverFile: File
    private var inCollection: Boolean = false

    private val currentIssue: FullIssue
        get() = fullVariant ?: fullIssue

    private val coverUri: Uri?
        get() = currentIssue.coverUri

    private var saveIssue = true
    private var isEditable: Boolean = true

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

        val issueId = arguments?.getSerializable(ARG_ISSUE_ID) as Int
        val variantOf = arguments?.getSerializable(ARG_VARIANT_OF) as Int?

        if (variantOf == null) {
            issueDetailViewModel.loadVariant(null)
            issueDetailViewModel.loadIssue(issueId)
        } else {
            Log.d(TAG, "ISSUE SELECTED IS A VARIANT!")
            issueDetailViewModel.loadVariant(issueId)
            issueDetailViewModel.loadIssue(variantOf)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                issueDetailViewModel.issue.collectLatest {
                    it?.let { issue ->
                        this@IssueDetailFragment.fullIssue = issue
                        listFragmentCallback?.setTitle("${issue.series.seriesName} #${issue.issue.issueNum}")
                        this@IssueDetailFragment.updateUI()
                    }
                }

            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {

        val view = inflater.inflate(R.layout.fragment_display_issue, container, false)

        coverImageView = view.findViewById(R.id.issue_cover) as ImageView
        issueCreditsFrame = view.findViewById(R.id.issue_credits_table) as ScrollView
        infoBox = view.findViewById(R.id.issue_info_box)
        gcdLinkButton = view.findViewById(R.id.gcd_link) as Button
        collectionButton = view.findViewById(R.id.collectionButton) as Button
        variantSpinnerHolder = view.findViewById(R.id.variant_spinner_holder)
        variantSpinner = view.findViewById(R.id.variant_spinner) as Spinner
//        issueCreditsLabel = view.findViewById(R.id.issue_credits_box_label) as TextView
        creditsBox = CreditsBox(requireContext()).apply { mCallback = this@IssueDetailFragment }
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
            viewLifecycleOwner,
            { issues ->
                Log.d(TAG, "issueList observation ${issues.size}")
                issuesInSeries = issues.map { it.issue.issueId }
                updateNavBar()
            }
        )

        issueDetailViewModel.issueCreditsLiveData.observe(
            viewLifecycleOwner,
            { credits: List<FullCredit>? ->
                credits?.let {
                    issueCredits = it
                    updateUI()
                }
            }
        )

        issueDetailViewModel.issueStoriesLiveData.observe(
            viewLifecycleOwner,
            { stories: List<Story>? ->
                stories?.let {
                    issueStories = it
                    updateUI()
                }
            }
        )

        issueDetailViewModel.issueAppearancesLiveData.observe(
            viewLifecycleOwner,
            { appearances: List<FullAppearance>? ->
                appearances?.let {
                    this@IssueDetailFragment.issueAppearances = it
                    updateUI()
                }
            }
        )

        issueDetailViewModel.variantLiveData.observe(
            viewLifecycleOwner,
            {
                it.let { variant ->
                    Log.d(TAG, "variant changed: $variant")
                    fullVariant = variant
                    updateUI()
                }
            }
        )

        issueDetailViewModel.variantCreditsLiveData.observe(
            viewLifecycleOwner,
            { credits: List<FullCredit>? ->
                credits?.let {
                    this.variantCredits = it
                    updateUI()
                }
            }
        )

        issueDetailViewModel.variantStoriesLiveData.observe(
            viewLifecycleOwner,
            { stories: List<Story> ->
                stories.let {
                    this.variantStories = it
                    updateUI()
                }
            }
        )

        issueDetailViewModel.variantAppearancesLiveData.observe(
            viewLifecycleOwner,
            { appearances: List<FullAppearance>? ->
                appearances?.let {
                    this.variantAppearances = it
                    updateUI()
                }
            }
        )

        issueDetailViewModel.variantsLiveData.observe(
            viewLifecycleOwner,
            { issues: List<Issue>? ->
                issues?.let {
                    val adapter = ArrayAdapter(
                        requireContext(),
                        R.layout.list_item_variant,
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
        )

        issueDetailViewModel.inCollectionLiveData.observe(
            viewLifecycleOwner,
            { count ->
                if (!isVariant) {
                    setCollectionButton(count)
                }
            }
        )

        issueDetailViewModel.variantInCollectionLiveData.observe(
            viewLifecycleOwner,
            { count ->
                if (isVariant) {
                    setCollectionButton(count)
                }
            }
        )
    }

    override fun onDetach() {
        super.onDetach()

        listFragmentCallback = null
    }

    private fun updateNavBar() {
        this.currentPos = this.issuesInSeries.indexOf(this.fullIssue.issue.issueId)
        val found = currentPos >= 0

        this.gotoStartButton.isEnabled = (currentPos > 0) && found
        this.gotoSkipBackButton.isEnabled = (currentPos >= 10) && found
        this.gotoPreviousButton.isEnabled = (currentPos > 0) && found
        this.gotoNextButton.isEnabled = (currentPos < this.issuesInSeries.size - 1) && found
        this.gotoSkipForwardButton.isEnabled =
            (currentPos <= this.issuesInSeries.size - 11) && found
        this.gotoEndButton.isEnabled = (currentPos < this.issuesInSeries.size - 1) && found
    }

    private fun setCollectionButton(count: Count) {
        if (count.count > 0) {
            this.collectionButton.text = getString(R.string.remove_from_collection)
            this.collectionButton.setOnClickListener {
                issueDetailViewModel.removeFromCollection()
            }
        } else {
            this.collectionButton.text = getString(R.string.add_to_collection)
            this.collectionButton.setOnClickListener {
                issueDetailViewModel.addToCollection()
            }
        }
    }

    private fun jumpToIssue(skipNum: Int) {
        issueDetailViewModel.loadIssue(this.issuesInSeries[currentPos + skipNum])
    }

    override fun onStart() {
        super.onStart()
        gcdLinkButton.setOnClickListener {
            val url = "https://www.comics.org/issue/${fullIssue.issue.issueId}"
            val intent = Intent().apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse(url)
            }
            startActivity(intent)
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

        collectionButton.setOnClickListener {
            issueDetailViewModel.addToCollection()
        }

//         TODO: This is for editing. n/a anymore?
//        releaseDateTextView.setOnClickListener {
//            DatePickerFragment.newInstance(fullIssue.issue.releaseDate).apply {
//                setTargetFragment(this@IssueDetailFragment, RESULT_DATE_PICKER)
//                show(this@IssueDetailFragment.parentFragmentManager, DIALOG_DATE)
//            }
//        }
//
        variantSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                    parent?.let {
                        val selectedIssueId =
                            (it.getItemAtPosition(position) as Issue).issueId

                        val selectionIsVariant =
                            selectedIssueId != issueDetailViewModel.issueId.value

                        if (selectionIsVariant) {
                            Log.d(TAG, "VARIANT")
                            issueDetailViewModel.loadVariant(selectedIssueId)
                        } else {
                            Log.d(TAG, "NOT VARIANT")
                            issueDetailViewModel.clearVariant()
                        }

                        updateCover()
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {

                }
            }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_issue, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.delete_issue -> {
                saveIssue = false
                issueDetailViewModel.delete(fullIssue.issue)
                requireActivity().onBackPressed()
                true
            }
            else              -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateUI() {
        val issue = currentIssue.issue
        if (issue.issueId != AUTO_ID) {
            numUpdates += 1

            listFragmentCallback?.setTitle("${currentIssue.series.seriesName} #${currentIssue.issue.issueNum}")

            infoBox.update(issue.releaseDate, issue.coverDate, issue.notes)
            creditsBox.update(issueStories, variantStories, issueCredits, variantCredits,
                              issueAppearances, variantAppearances)

            fullVariant?.issue?.let {
                Log.d(TAG, "SETtting SPINNNEER TO  A V DSLKRIANT")
                val indexOf = issueVariants.indexOf(it)
                Log.d(TAG, "AND THE INDEX IS: $indexOf")
                variantSpinner.setSelection(indexOf)
            }

            updateCover()
        }
    }

    private fun updateCover() {
        if (coverUri != null) {
            coverImageView.setImageURI(coverUri)
        } else {
            coverImageView.setImageResource(R.drawable.cover_missing)
        }
    }

    override fun characterClicked(character: FullCharacter) {
        val filter = SearchFilter(character = character.character, myCollection = false)
        listFragmentCallback?.updateFilter(filter)
    }

    override fun creatorClicked(creator: NameDetailAndCreator) {
        val filter = SearchFilter(creators = setOf(creator.creator), myCollection = false)
        listFragmentCallback?.updateFilter(filter)
    }

    companion object {
        @JvmStatic
        fun newInstance(
            issueId: Int? = null,
            openAsEditable: Boolean = true,
            variantOf: Int? = null,
        ): IssueDetailFragment =
            IssueDetailFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_ISSUE_ID, issueId)
                    putSerializable(ARG_EDITABLE, openAsEditable)
                    putSerializable(ARG_VARIANT_OF, variantOf)
                }
            }

        private const val TAG = APP + "IssueDetailFragment"
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
