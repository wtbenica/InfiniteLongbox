package com.wtb.comiccollector

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

private const val TAG = "IssueFragment"

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

/**
 * A simple [Fragment] subclass.
 * Use the [IssueDetailFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
// TODO: Do I need a separate fragment for editing vs viewing or can I do it all in this one?
class IssueDetailFragment private constructor() : Fragment(),
    DatePickerFragment.Callbacks {

    private var numUpdates = 0

    private lateinit var fullIssue: IssueAndSeries
    private lateinit var issueCredits: List<FullCredit>

    private lateinit var seriesList: List<Series>
    private lateinit var writersList: List<Creator>
    private lateinit var rolesList: List<Role>

    private lateinit var coverImageView: ImageView
    private lateinit var seriesSpinner: Spinner
    private lateinit var issueNumEditText: EditText

    private lateinit var writersLabel: TextView
    private lateinit var writersBox: TableLayout
    private lateinit var writerSpinner: Spinner
    private lateinit var addWriterButton: ImageButton

    private lateinit var pencillersLabel: TextView
    private lateinit var pencillersBox: TableLayout
    private lateinit var pencillerSpinner: Spinner
    private lateinit var addPencillerButton: ImageButton

    private lateinit var inkersLabel: TextView
    private lateinit var inkersBox: TableLayout
    private lateinit var inkerSpinner: Spinner
    private lateinit var addInkerButton: ImageButton

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

        writersList = emptyList()
        rolesList = emptyList()
        isEditable = arguments?.getSerializable(ARG_EDITABLE) as Boolean
        seriesList = emptyList()

        fullIssue = IssueAndSeries(Issue(), Series())
        issueCredits = emptyList()

        issueDetailViewModel.loadIssue(arguments?.getSerializable(ARG_ISSUE_ID) as UUID)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_edit_issue, container, false)

        seriesSpinner = view.findViewById(R.id.issue_series) as Spinner

        coverImageView = view.findViewById(R.id.issue_cover) as ImageView

        issueNumEditText = view.findViewById(R.id.issue_number) as EditText

        writersLabel = view.findViewById(R.id.writer_label) as TextView
        writersBox = view.findViewById(R.id.writers_box) as TableLayout
        writerSpinner = view.findViewById(R.id.issue_writer) as Spinner
        addWriterButton = view.findViewById(R.id.add_writer_button) as ImageButton

        pencillersLabel = view.findViewById(R.id.pencillers_label)
        pencillersBox = view.findViewById(R.id.pencillers_box) as TableLayout
        pencillerSpinner = view.findViewById(R.id.issue_penciller) as Spinner
        addPencillerButton = view.findViewById(R.id.add_penciller_button) as ImageButton

        inkersLabel = view.findViewById(R.id.inkers_label)
        inkersBox = view.findViewById(R.id.inkers_box) as TableLayout
        inkerSpinner = view.findViewById(R.id.issue_inker) as Spinner
        addInkerButton = view.findViewById(R.id.add_inker_button) as ImageButton

        releaseDateTextView = view.findViewById(R.id.release_date_text_view)

        toggleEditButton = view.findViewById(R.id.edit_button) as ImageButton

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        issueDetailViewModel.fullIssueLiveData.observe(
            viewLifecycleOwner,
            { issue ->
                issue?.let {
                    this.fullIssue = it
                    updateUI()
                }
            }
        )

        issueDetailViewModel.issueCreditsLiveData.observe(
            viewLifecycleOwner,
            { issue ->
                issue?.let {
                    this.issueCredits = it
                    updateUI()
                }
            }
        )

        issueDetailViewModel.allSeriesLiveData.observe(viewLifecycleOwner,
            { allSeries ->
                allSeries?.let {
                    val thisList = it + Series(publisherId = NEW_SERIES_ID)
                    val adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        thisList
                    )
                    seriesList = thisList
                    seriesSpinner.adapter = adapter
                }
            })

        issueDetailViewModel.allRolesLiveData.observe(viewLifecycleOwner,
            {allRoles ->
                rolesList = allRoles
            })

        issueDetailViewModel.allCreatorsLiveData.observe(viewLifecycleOwner,
            { allWriters ->
                allWriters?.let {
                    val adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        it
                    )
                    writersList = it
                    writerSpinner.adapter = adapter
                    pencillerSpinner.adapter = adapter
                    inkerSpinner.adapter = adapter
                }
            })

        if (!isEditable) {
            // TODO: Create a separate layout for editing vs viewing instead of this
            toggleEnable()
        }
    }

    override fun onStart() {
        super.onStart()
        attachLabelListeners()
        attachAddCreditListeners()
        toggleEditButton.setOnClickListener { toggleEnable() }

        attachSeriesListener()
        attachIssueNumListener()
        attachCreatorListeners()
        attachCoverImageListener()

        releaseDateTextView.setOnClickListener {
            DatePickerFragment.newInstance(fullIssue.issue.releaseDate).apply {
                setTargetFragment(this@IssueDetailFragment, RESULT_DATE_PICKER)
                show(this@IssueDetailFragment.parentFragmentManager, DIALOG_DATE)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        when {
            resultCode != Activity.RESULT_OK -> return
            requestCode == RESULT_SERIES_DETAIL && data != null -> {
                this.fullIssue.issue.seriesId = data.getSerializableExtra(ARG_SERIES_ID) as UUID
                saveChanges()
            }
            requestCode == RESULT_NEW_WRITER && data != null -> {
                this.fullIssue.issue.writerId = data.getSerializableExtra(ARG_CREATOR_ID) as UUID
                saveChanges()
            }
            requestCode == RESULT_NEW_PENCILLER && data != null -> {
                this.fullIssue.issue.pencillerId = data.getSerializableExtra(ARG_CREATOR_ID) as UUID
                saveChanges()
            }
            requestCode == RESULT_NEW_INKER && data != null -> {
                this.fullIssue.issue.inkerId = data.getSerializableExtra(ARG_CREATOR_ID) as UUID
                saveChanges()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        saveChanges()
        if (fullIssue.issue.seriesId == NEW_SERIES_ID || fullIssue.series.seriesName == "New Series") {
            issueDetailViewModel.deleteIssue(fullIssue.issue)
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

    override fun onDateSelected(date: LocalDate) {
        fullIssue.issue.releaseDate = date
        saveChanges()
    }

    private fun attachIssueNumListener() {
        issueNumEditText.addTextChangedListener(
            SimpleTextWatcher { sequence ->
                fullIssue.issue.issueNum = try {
                    sequence.toString().toInt()
                } catch (e: Exception) {
                    1
                }
            }
        )
    }

    private fun saveChanges() {
        issueDetailViewModel.updateIssue(fullIssue.issue)
        issueDetailViewModel.loadIssue(fullIssue.issue.issueId)
        updateUI()
    }

    private fun updateCreator(spinner: Spinner, roleName: String) {
        spinner.setSelection(maxOf(0, writersList.indexOf(getCredit(roleName)?.creator)))
    }

    private fun updateUI() {
        numUpdates += 1
        Log.d(TAG, "$numUpdates updates *****************************************************")

        seriesSpinner.setSelection(maxOf(0, seriesList.indexOf(fullIssue.series)))

        updateCreator(writerSpinner, "Writer")
        updateCreator(pencillerSpinner, "Penciller")
        updateCreator(inkerSpinner, "Inker")

        issueNumEditText.setText(
            if (this.fullIssue.issue.issueNum == Int.MAX_VALUE) {
                "1"
            } else {
                this.fullIssue.issue.issueNum.toString()
            }
        )

        this.fullIssue.issue.releaseDate?.format(DateTimeFormatter.ofPattern("MMMM d, y"))
            ?.let { releaseDateTextView.text = it }

        this.fullIssue.issue.coverUri?.let {
            coverImageView.setImageURI(this.fullIssue.issue.coverUri)
            coverImageView.contentDescription = "Issue Cover (set)"
        }
    }

    private fun attachSeriesListener() {
        seriesSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                Log.d(TAG, "seriesAutoComplete ItemSelected")
                parent?.let {
                    val selectedSeries = it.getItemAtPosition(position) as Series
                    if (selectedSeries.seriesName == "New Series") {
                        selectedSeries.seriesName = ""
                        issueDetailViewModel.addSeries(selectedSeries)
                        fullIssue.issue.seriesId = selectedSeries.seriesId
                        issueDetailViewModel.updateIssue(fullIssue.issue)
                        issueDetailViewModel.loadIssue(fullIssue.issue.issueId)
                        val d = SeriesInfoDialogFragment.newInstance(fullIssue.issue.seriesId)
                        d.setTargetFragment(this@IssueDetailFragment, RESULT_SERIES_DETAIL)
                        d.show(parentFragmentManager, DIALOG_SERIES_DETAIL)
                    } else {
                        fullIssue.issue.seriesId = selectedSeries.seriesId
                        saveChanges()
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

        }
    }

    private fun attachCoverImageListener() {
        //        coverImageView.apply {
        //            setOnClickListener {
        //                val getImageIntent =
        //                    Intent(Intent.ACTION_GET_CONTENT, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        //                val chooserIntent = Intent.createChooser(getImageIntent, null)
        //                startActivityForResult(chooserIntent, PICK_COVER_IMAGE)
        //            }
        //        }
    }

    private fun attachAddCreditListeners() {
        addWriterButton.setOnClickListener(addNewRow(writersBox, addWriterButton))

        addPencillerButton.setOnClickListener(addNewRow(pencillersBox, addPencillerButton))

        addInkerButton.setOnClickListener(addNewRow(inkersBox, addInkerButton))
    }

    private fun attachLabelListeners() {
        writersLabel.setOnClickListener { addCreator(RESULT_NEW_WRITER) }

        pencillersLabel.setOnClickListener { addCreator(RESULT_NEW_PENCILLER) }

        inkersLabel.setOnClickListener { addCreator(RESULT_NEW_INKER) }
    }

    private fun getRoleByName(roleName: String) : Role? {
        for (role in rolesList) {
            if (role.roleName == roleName) {
                return role
            }
        }
        return null
    }

    private fun attachCreatorListeners() {

        class CreditsOnItemSelectedListener(val roleName: String) : AdapterView
        .OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                parent?.let {
                    val fullCredit: FullCredit? = getCredit(roleName)
                    val creatorId = (parent.getItemAtPosition(position) as Creator).creatorId

                    if (fullCredit != null) {
                        fullCredit.credit.creatorId = creatorId
                        issueDetailViewModel.updateCredit(fullCredit.credit)
                    } else {
                        issueDetailViewModel.addCredit(
                            Credit(
                                issueId = fullIssue.issue.issueId,
                                creatorId = creatorId,
                                roleId = getRoleByName(roleName)?.roleId!!
                            )
                        )
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }

        }

        writerSpinner.onItemSelectedListener = CreditsOnItemSelectedListener("Writer")

        pencillerSpinner.onItemSelectedListener = CreditsOnItemSelectedListener("Penciller")

        inkerSpinner.onItemSelectedListener = CreditsOnItemSelectedListener("Inker")

    }

    private fun toggleEnable() {
        seriesSpinner.isEnabled = !seriesSpinner.isEnabled

        writerSpinner.isEnabled = !writerSpinner.isEnabled
        pencillerSpinner.isEnabled = !pencillerSpinner.isEnabled
        inkerSpinner.isEnabled = !inkerSpinner.isEnabled

        addWriterButton.isVisible = !addWriterButton.isVisible
        addPencillerButton.isVisible = !addPencillerButton.isVisible
        addInkerButton.isVisible = !addInkerButton.isVisible

        issueNumEditText.isEnabled = !issueNumEditText.isEnabled
        releaseDateTextView.isEnabled = !releaseDateTextView.isEnabled
    }

    // TODO: Add textWatchers. as of now, the editTexts dont save anything
    private fun addNewRow(parentTable: TableLayout, addButton: ImageButton): (v: View) -> Unit = {
        val numChildren = parentTable.childCount
        val newRow = TableRow(context)
        val newText = EditText(context)
        newText.layoutParams = TableRow.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1.0f
        )
        newRow.addView(newText)
        (parentTable.children.elementAt(numChildren - 1) as TableRow).removeView(addButton)
        newRow.addView(addButton)
        parentTable.addView(newRow)
    }

    fun getCredit(type: String): FullCredit? {
        for (credit in issueCredits) {
            if (credit.role.roleName == type) {
                return credit
            }
        }

        return null
    }

    private fun addCreator(requestCode: Int) {
        val d = NewCreatorDialogFragment()
        d.setTargetFragment(this@IssueDetailFragment, requestCode)
        d.show(parentFragmentManager, DIALOG_NEW_CREATOR)
    }

    companion object {
        @JvmStatic
        fun newInstance(
            issueId: UUID? = null,
            openAsEditable: Boolean = true
        ): IssueDetailFragment =
            IssueDetailFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_ISSUE_ID, issueId)
                    putSerializable(ARG_EDITABLE, openAsEditable)
                }
            }
    }
}

