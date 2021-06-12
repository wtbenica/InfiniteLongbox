package com.wtb.comiccollector.issue_details.fragments

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.wtb.comiccollector.*
import com.wtb.comiccollector.database.Daos.Count
import com.wtb.comiccollector.database.models.*
import com.wtb.comiccollector.issue_details.view_models.IssueDetailViewModel
import com.wtb.comiccollector.repository.DUMMY_ID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import java.io.File
import java.time.format.DateTimeFormatter
import java.util.*

private const val TAG = APP + "IssueDetailFragment"

// Bundle Argument Tags
private const val ARG_ISSUE_ID = "issue_id"
private const val ARG_EDITABLE = "open_as_editable"

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
private const val STORY_TYPE_COVER = 6

/**
 * A simple [Fragment] subclass.
 * Use the [IssueDetailFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
// TODO: Do I need a separate fragment for editing vs viewing or can I do it all in this one?
@ExperimentalCoroutinesApi
class IssueDetailFragment : Fragment() {

    private var numUpdates = 0

    private var theJob: Job? = null

    private lateinit var fullIssue: FullIssue
    private var issuesInSeries: List<Int> = emptyList()
    private var currentPos: Int = 0
    private lateinit var issueCredits: List<FullCredit>
    private lateinit var issueStories: List<Story>
    private lateinit var variantCredits: List<FullCredit>
    private lateinit var variantStories: List<Story>
    private lateinit var issueVariants: List<Issue>

    private lateinit var coverImageView: ImageView
    private lateinit var seriesTextView: TextView
    private lateinit var issueNumTextView: TextView
    private lateinit var collectionButton: Button
    private lateinit var variantSpinner: Spinner
    private var isVariant: Boolean = false

    //    private lateinit var issueCreditsLabel: TextView
    private lateinit var issueCreditsFrame: ScrollView
    private lateinit var creditsBox: CreditsBox

    private lateinit var releaseDateTextView: TextView

    private lateinit var gotoStartButton: Button
    private lateinit var gotoSkipBackButton: Button
    private lateinit var gotoPreviousButton: Button
    private lateinit var gotoNextButton: Button
    private lateinit var gotoSkipForwardButton: Button
    private lateinit var gotoEndButton: Button

    private lateinit var gcdLinkButton: Button
    private lateinit var coverFile: File
    private var inCollection: Boolean = false
    private var coverUri: Uri? = null
    private var variantUri: Uri? = null

    private var saveIssue = true
    private var isEditable: Boolean = true

    private val issueDetailViewModel: IssueDetailViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        fullIssue = FullIssue(Issue(), SeriesAndPublisher(Series(), Publisher()), null)
        issueCredits = emptyList()
        issueStories = emptyList()
        variantCredits = emptyList()
        variantStories = emptyList()

        issueDetailViewModel.loadIssue(arguments?.getSerializable(ARG_ISSUE_ID) as Int)

        lifecycleScope.launchWhenCreated {
            issueDetailViewModel.issue.collect {
                it?.let { issue ->
                    this@IssueDetailFragment.fullIssue = issue
                    this@IssueDetailFragment.coverUri = issue.coverUri
                    (requireActivity() as MainActivity).supportActionBar?.apply {
                        it.let { title = "${issue.series.seriesName} #${issue.issue.issueNum}" }
                    }
                    this@IssueDetailFragment.updateUI()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_display_issue, container, false)

        seriesTextView = view.findViewById(R.id.issue_series) as TextView
        coverImageView = view.findViewById(R.id.issue_cover) as ImageView
        issueNumTextView = view.findViewById(R.id.issue_number) as TextView
        issueCreditsFrame = view.findViewById(R.id.issue_credits_table) as ScrollView
        releaseDateTextView = view.findViewById(R.id.release_date_text_view)
        gcdLinkButton = view.findViewById(R.id.gcd_link) as Button
        collectionButton = view.findViewById(R.id.collectionButton) as Button
        variantSpinner = view.findViewById(R.id.variant_spinner) as Spinner
//        issueCreditsLabel = view.findViewById(R.id.issue_credits_box_label) as TextView
        creditsBox = CreditsBox(requireContext())
        issueCreditsFrame.addView(creditsBox)

        gotoStartButton = view.findViewById(R.id.goto_start_button) as Button
        gotoSkipBackButton = view.findViewById(R.id.goto_skip_back_button) as Button
        gotoPreviousButton = view.findViewById(R.id.goto_previous_button) as Button
        gotoNextButton = view.findViewById(R.id.goto_next_button) as Button
        gotoSkipForwardButton = view.findViewById(R.id.goto_skip_forward_button) as Button
        gotoEndButton = view.findViewById(R.id.goto_end_button) as Button



        return view
    }

    @ExperimentalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        issueDetailViewModel.issueList.observe(
            viewLifecycleOwner,
            { issues ->
                this@IssueDetailFragment.issuesInSeries = issues.map { it.issue.issueId }
                updateNavBar()
            }
        )


        issueDetailViewModel.issueCreditsLiveData.observe(
            viewLifecycleOwner,
            { credits: List<FullCredit>? ->
                credits?.let {
                    this.issueCredits = it
                    updateUI()
                }
            }
        )

        issueDetailViewModel.issueStoriesLiveData.observe(
            viewLifecycleOwner,
            { stories: List<Story>? ->
                stories?.let {
                    Log.d(TAG, "issueStories updated: ${stories.size}")
                    this.issueStories = it
                    updateUI()
                }
            }
        )

        issueDetailViewModel.variant.observe(
            viewLifecycleOwner,
            { issue ->
                issue.let {
                    this.isVariant = issue != null
                    this.variantUri = issue?.coverUri
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

        issueDetailViewModel.variantsLiveData.observe(
            viewLifecycleOwner,
            { issues: List<Issue>? ->
                issues?.let {
                    val adapter = ArrayAdapter(
                        requireContext(),
                        R.layout.list_item_variant,
                        it
                    )
                    this.issueVariants = it
                    variantSpinner.adapter = adapter
                    variantSpinner.visibility = if (it.size <= 1) {
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

    private fun updateNavBar() {
        Log.d(TAG, "updateNavBar")
        this.currentPos = this.issuesInSeries.indexOf(this.fullIssue.issue.issueId)
        val found = currentPos != -1

        this.gotoStartButton.isEnabled = (currentPos != 0) && found
        this.gotoSkipBackButton.isEnabled = (currentPos >= 10) && found
        this.gotoPreviousButton.isEnabled = (currentPos != 0) && found
        this.gotoNextButton.isEnabled =
            (currentPos != this.issuesInSeries.size - 1) && found
        this.gotoSkipForwardButton.isEnabled =
            (currentPos <= this.issuesInSeries.size - 11) && found
        this.gotoEndButton.isEnabled = (currentPos != this.issuesInSeries.size - 1) && found
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

        releaseDateTextView.setOnClickListener {
            DatePickerFragment.newInstance(fullIssue.issue.releaseDate).apply {
                setTargetFragment(this@IssueDetailFragment, RESULT_DATE_PICKER)
                show(this@IssueDetailFragment.parentFragmentManager, DIALOG_DATE)
            }
        }

        variantSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    parent?.let {
                        val selectedIssueId =
                            (it.getItemAtPosition(position) as Issue).issueId
                        if (selectedIssueId != issueDetailViewModel.getIssueId()) {
                            issueDetailViewModel.loadVariant(selectedIssueId)
                            isVariant = true
                        } else {
                            issueDetailViewModel.clearVariant()
                            this@IssueDetailFragment.variantUri = null
                            isVariant = false
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
                issueDetailViewModel.deleteIssue(fullIssue.issue)
                requireActivity().onBackPressed()
                true
            }
            else              -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateUI() {

        if (fullIssue.issue.issueId != DUMMY_ID) {
            numUpdates += 1

            seriesTextView.text = fullIssue.series.seriesName

            issueNumTextView.text = fullIssue.issue.issueNum.toString()

            this.fullIssue.issue.releaseDate?.format(DateTimeFormatter.ofPattern("MMM d, y"))
                ?.let { releaseDateTextView.text = it }

            creditsBox.displayCredit()

            updateCover()
        } else {
            Log.d(TAG, "updateUI: Dummy")
        }
    }

    private fun updateCover() {
        val uri = when (isVariant) {
            true -> this.variantUri
            false -> this.coverUri
        }
        if (uri != null) {
            coverImageView.setImageURI(uri)
        } else {
            coverImageView.setImageResource(R.drawable.ic_issue_add_cover)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(
            issueId: Int? = null,
            openAsEditable: Boolean = true,
        ): IssueDetailFragment =
            IssueDetailFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_ISSUE_ID, issueId)
                    putSerializable(ARG_EDITABLE, openAsEditable)
                }
            }
    }

    inner class CreditsBox(context: Context) : TableLayout(context) {
        init {
            orientation = VERTICAL
            layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
            isStretchAllColumns = true
        }

        fun displayCredit() {
            this.removeAllViews()
            val stories = combineCredits(issueStories, variantStories)
            Log.d(
                TAG,
                "DISPLAY STORIES ${stories.size} ${issueStories.size} ${variantStories.size}"
            )
            stories.forEach { story ->
                this.addView(StoryRow(context, story))
                val credits = issueCredits + variantCredits
                credits.forEach { credit ->
                    if (credit.story.storyId == story.storyId) {
                        this.addView(CreditsRow(context, credit))
                    }
                }
            }
        }

        private fun combineCredits(
            original: List<Story>,
            variant: List<Story>
        ): List<Story> =
            if (STORY_TYPE_COVER in variant.map { it.storyType }) {
                original.mapNotNull {
                    if (it.storyType != STORY_TYPE_COVER) {
                        it
                    } else {
                        null
                    }
                } + variant
            } else {
                original + variant
            }
    }

    inner class StoryRow(context: Context, val story: Story) : LinearLayout(context) {
        init {
            orientation = VERTICAL
            layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)

            layoutInflater.inflate(R.layout.story_box, this, true)

            val storyDetailButton: ImageButton = findViewById(R.id.story_dropdown_button)
            val storyDetailBox: LinearLayout = findViewById(R.id.story_details_box)
            storyDetailButton.setOnClickListener(toggleVisibility(storyDetailBox))

            if (story.synopsis != null && story.synopsis != "") {
                val synopsisButton: ImageButton =
                    findViewById(R.id.synopsis_dropdown_button)
                val synopsis = findViewById<TextView>(R.id.synopsis)
                synopsis.text = story.synopsis
                synopsisButton.setOnClickListener(toggleVisibility(synopsis))
            } else {
                val synopsisBox = findViewById<LinearLayout>(R.id.synopsis_box)
                synopsisBox.visibility = GONE
            }

            if (story.characters != null && story.characters != "") {
                val charactersButton: ImageButton =
                    findViewById(R.id.characters_dropdown_button)
                val characters = findViewById<TextView>(R.id.characters)
                characters.text = story.characters
                charactersButton.setOnClickListener(toggleVisibility(characters))
            } else {
                val charactersBox = findViewById<LinearLayout>(R.id.characters_box)
                charactersBox.visibility = GONE
            }
            val storyTitle = findViewById<TextView>(R.id.story_title)
            storyTitle.text = if (story.storyType == STORY_TYPE_COVER) {
                "Cover"
            } else {
                story.title
            }
        }

        private fun <T : View> toggleVisibility(view: T): (v: View) -> Unit {
            return {
                if (view.visibility == GONE) {
                    view.visibility = VISIBLE
                } else {
                    view.visibility = GONE
                }
            }
        }
    }

    inner class CreditsRow(context: Context, private val fullCredit: FullCredit) :
        TableRow(context) {
        init {
            this.addView(TextView(context).apply {
                layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
                text = fullCredit.role.roleName
                setPaddingRelative(16, 4, 16, 4)
                setTextAppearance(R.style.TextAppearance_MaterialComponents_Subtitle1)
            })
            this.addView(TextView(context).apply {
                text = fullCredit.nameDetail.creator.name
                layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
            })
        }
    }
}
