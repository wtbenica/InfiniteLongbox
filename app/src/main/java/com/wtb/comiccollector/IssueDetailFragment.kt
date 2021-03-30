package com.wtb.comiccollector

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import java.io.File
import java.time.format.DateTimeFormatter
import java.util.*

private const val TAG = "IssueDetailFragment"

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

/**
 * A simple [Fragment] subclass.
 * Use the [IssueDetailFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
// TODO: Do I need a separate fragment for editing vs viewing or can I do it all in this one?
class IssueDetailFragment : Fragment() {

    private var numUpdates = 0

    private lateinit var fullIssue: IssueAndSeries
    private lateinit var issueCredits: List<FullCredit>
    private lateinit var issueStories: List<Story>
    private lateinit var variantCredits: List<FullCredit>
    private lateinit var variantStories: List<Story>
    private lateinit var issueVariants: List<Issue>

    private lateinit var coverImageView: ImageView
    private lateinit var seriesTextView: TextView
    private lateinit var issueNumTextView: TextView

    private lateinit var variantSpinner: Spinner

    //    private lateinit var issueCreditsLabel: TextView
    private lateinit var issueCreditsFrame: ScrollView
    private lateinit var creditsBox: CreditsBox

    private lateinit var releaseDateTextView: TextView

    private lateinit var toggleEditButton: ImageButton
    private lateinit var coverFile: File
    private lateinit var coverUri: Uri

    private var saveIssue = true
    private var isEditable: Boolean = true

    private val issueDetailViewModel: IssueDetailViewModel by lazy {
        ViewModelProvider(this).get(IssueDetailViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        fullIssue = IssueAndSeries(Issue(), Series())
        issueCredits = emptyList()
        issueStories = emptyList()
        variantCredits = emptyList()
        variantStories = emptyList()

        issueDetailViewModel.loadIssue(arguments?.getSerializable(ARG_ISSUE_ID) as Int)
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
        toggleEditButton = view.findViewById(R.id.edit_button) as ImageButton
        variantSpinner = view.findViewById(R.id.variant_spinner) as Spinner
//        issueCreditsLabel = view.findViewById(R.id.issue_credits_box_label) as TextView
        creditsBox = CreditsBox(requireContext())
        issueCreditsFrame.addView(creditsBox)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        issueDetailViewModel.fullIssueLiveData.observe(
            viewLifecycleOwner,
            { issue: IssueAndSeries? ->
                issue?.let {
                    this.fullIssue = it
                    updateUI()
                }
            }
        )

        issueDetailViewModel.issueCreditsLiveData.observe(
            viewLifecycleOwner,
            { credits: List<FullCredit>? ->
                credits?.let {
                    this.issueCredits = it
                    creditsBox.displayCredit()
                    updateUI()
                }
            }
        )

        issueDetailViewModel.issueStoriesLiveData.observe(
            viewLifecycleOwner,
            { stories: List<Story>? ->
                stories?.let {
                    this.issueStories = it
                    updateUI()
                }
            }
        )

        issueDetailViewModel.variantCreditsLiveData.observe(
            viewLifecycleOwner,
            { credits: List<FullCredit>? ->
                credits?.let {
                    this.variantCredits = it
                    creditsBox.displayCredit()
                    updateUI()
                }
            }
        )

        issueDetailViewModel.variantStoriesLiveData.observe(
            viewLifecycleOwner,
            { stories: List<Story> ->
                stories.let {
                    this.variantStories = it
                    creditsBox.displayCredit()
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
                        androidx.appcompat.R.layout.support_simple_spinner_dropdown_item,
                        it
                    )
                    this.issueVariants = it
                    variantSpinner.adapter = adapter
                    updateUI()
                }
            }
        )
    }

    override fun onStart() {
        super.onStart()
        toggleEditButton.setOnClickListener { toggleEnable() }


        releaseDateTextView.setOnClickListener {
            DatePickerFragment.newInstance(fullIssue.issue.releaseDate).apply {
                setTargetFragment(this@IssueDetailFragment, RESULT_DATE_PICKER)
                show(this@IssueDetailFragment.parentFragmentManager, DIALOG_DATE)
            }
        }

        variantSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                parent?.let {
                    val selectedIssueId = (it.getItemAtPosition(position) as Issue).issueId
                    if (selectedIssueId != issueDetailViewModel.getIssue()) {
                        issueDetailViewModel.loadVariant(selectedIssueId)
                    } else {
                        issueDetailViewModel.loadVariant(null)
                    }
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
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateUI() {
        numUpdates += 1
        Log.d(TAG, "$numUpdates updates *****************************************************")

        seriesTextView.text = fullIssue.series.seriesName

        issueNumTextView.text = fullIssue.issue.issueNum.toString()

        this.fullIssue.issue.releaseDate?.format(DateTimeFormatter.ofPattern("MMMM d, y"))
            ?.let { releaseDateTextView.text = it }

        this.fullIssue.issue.coverUri?.let {
            coverImageView.setImageURI(this.fullIssue.issue.coverUri)
            coverImageView.contentDescription = "Issue Cover (set)"
        }
    }

    private fun toggleEnable() {

    }

    companion object {
        @JvmStatic
        fun newInstance(
            issueId: Int? = null,
            openAsEditable: Boolean = true
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
            val stories = issueStories + variantStories
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
    }

    inner class StoryRow(context: Context, val story: Story) : LinearLayout(context) {
        init {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)

            val t = listOf(
                story.feature, story.title, story.synopsis, story.characters, story.notes,
                story.sequenceNumber
            )

            this.addView(TextView(context).apply {
                text = if (story.storyType == 6) {
                    "Cover"
                } else {
                    story.title.toString()
                }
                layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                textSize = 18.0F
                setTextAppearance(R.style.TextAppearance_MaterialComponents_Headline6)
            })
        }
    }

    inner class CreditsRow(context: Context, private val fullCredit: FullCredit) :
        TableRow(context) {
        init {
            this.addView(TextView(context).apply {
                layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
                text = fullCredit.role.roleName
                setTextAppearance(R.style.TextAppearance_MaterialComponents_Subtitle1)
            })
            this.addView(TextView(context).apply {
                text = fullCredit.creator.name
                layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
            })
        }
    }
}
