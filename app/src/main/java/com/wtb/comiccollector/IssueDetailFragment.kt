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

    private lateinit var issue: Issue
    private lateinit var series: Series
    private lateinit var writer: Creator
    private lateinit var penciller: Creator
    private lateinit var inker: Creator
    private lateinit var seriesList: List<Series>
    private lateinit var writersList: List<Creator>

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
        series = Series()
        issue = Issue()
        writer = Creator(firstName = "")
        penciller = Creator(firstName = "")
        inker = Creator(firstName = "")
        writersList = emptyList()
        isEditable = arguments?.getSerializable(ARG_EDITABLE) as Boolean
        seriesList = emptyList()

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

        issueDetailViewModel.issueLiveData.observe(
            viewLifecycleOwner,
            { issue ->
                issue?.let {
                    this.issue = it
                    updateUI()
                }
            }
        )

        issueDetailViewModel.seriesLiveData.observe(
            viewLifecycleOwner,
            { series ->
                series?.let {
                    this.series = it
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

        issueDetailViewModel.writerLiveData.observe(
            viewLifecycleOwner,
            { writer ->
                writer?.let {
                    this.writer = it
                    updateUI()
                }
            }
        )

        issueDetailViewModel.pencillerLiveData.observe(
            viewLifecycleOwner,
            { penciller ->
                penciller?.let {
                    this.penciller = it
                    updateUI()
                }
            }
        )

        issueDetailViewModel.inkerLiveData.observe(
            viewLifecycleOwner,
            { inker ->
                inker?.let {
                    this.inker = it
                    updateUI()
                }
            }
        )

        if (!isEditable) {
            // TODO: Create a separate layout for editing vs viewing instead of this
            toggleEnable()
        }
    }

    override fun onStart() {
        super.onStart()
        attachTextWatchers()

        toggleEditButton.setOnClickListener {
            toggleEnable()
        }

//        coverImageView.apply {
//            setOnClickListener {
//                val getImageIntent =
//                    Intent(Intent.ACTION_GET_CONTENT, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
//                val chooserIntent = Intent.createChooser(getImageIntent, null)
//                startActivityForResult(chooserIntent, PICK_COVER_IMAGE)
//            }
//        }

        addWriterButton.setOnClickListener(addNewRow(writersBox, addWriterButton))

        addPencillerButton.setOnClickListener(addNewRow(pencillersBox, addPencillerButton))

        addInkerButton.setOnClickListener(addNewRow(inkersBox, addInkerButton))

        writersLabel.setOnClickListener {
            val d = NewCreatorDialogFragment()
            d.setTargetFragment(this@IssueDetailFragment, RESULT_NEW_WRITER)
            d.show(parentFragmentManager, DIALOG_NEW_CREATOR)
        }

        pencillersLabel.setOnClickListener {
            val d = NewCreatorDialogFragment()
            d.setTargetFragment(this@IssueDetailFragment, RESULT_NEW_PENCILLER)
            d.show(parentFragmentManager, DIALOG_NEW_CREATOR)
        }

        inkersLabel.setOnClickListener {
            val d = NewCreatorDialogFragment()
            d.setTargetFragment(this@IssueDetailFragment, RESULT_NEW_INKER)
            d.show(parentFragmentManager, DIALOG_NEW_CREATOR)
        }

        releaseDateTextView.setOnClickListener {
            DatePickerFragment.newInstance(issue.releaseDate).apply {
                setTargetFragment(this@IssueDetailFragment, RESULT_DATE_PICKER)
                show(this@IssueDetailFragment.parentFragmentManager, DIALOG_DATE)
            }
        }
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

    private fun attachTextWatchers() {
        val issueNumWatcher = SimpleTextWatcher { sequence ->
            issue.issueNum = try {
                sequence.toString().toInt()
            } catch (e: Exception) {
                1
            }
        }

        writerSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                parent?.let {
                    issueDetailViewModel.allCreatorsLiveData.value?.let {
                        issue.writerId = (parent.getItemAtPosition(position) as Creator).creatorId
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }

        pencillerSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                parent?.let {
                    issue.pencillerId = (it.getItemAtPosition(position) as Creator).creatorId
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }

        inkerSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                parent?.let {
                    issue.inkerId = (it.getItemAtPosition(position) as Creator).creatorId
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }

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
                        issue.seriesId = selectedSeries.seriesId
                        issueDetailViewModel.updateIssue(issue)
                        issueDetailViewModel.loadIssue(issue.issueId)
                        val d = SeriesInfoDialogFragment.newInstance(issue.seriesId)
                        d.setTargetFragment(this@IssueDetailFragment, RESULT_SERIES_DETAIL)
                        d.show(parentFragmentManager, DIALOG_SERIES_DETAIL)
                    } else {
                        issue.seriesId = selectedSeries.seriesId
                        saveChanges()
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

        }

//        seriesAutoComplete.setOnDismissListener {
//            Log.d(TAG, "seriesAutoComplete Dismiss")
//            if (series.seriesName == "New Series") {
//                val d = NewSeriesDialogFragment.newInstance(seriesAutoComplete.text.toString())
//                d.setTargetFragment(this@IssueDetailFragment, RESULT_NEW_SERIES)
//                d.show(parentFragmentManager, DIALOG_NEW_SERIES)
//            }
//        }
//
        issueNumEditText.addTextChangedListener(issueNumWatcher)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        when {
            resultCode != Activity.RESULT_OK -> return
            requestCode == RESULT_SERIES_DETAIL && data != null -> {
                this.issue.seriesId = data.getSerializableExtra(ARG_SERIES_ID) as UUID
                saveChanges()
            }
            requestCode == RESULT_NEW_WRITER && data != null -> {
                this.issue.writerId = data.getSerializableExtra(ARG_CREATOR_ID) as UUID
                saveChanges()
            }
            requestCode == RESULT_NEW_PENCILLER && data != null -> {
                this.issue.pencillerId = data.getSerializableExtra(ARG_CREATOR_ID) as UUID
                saveChanges()
            }
            requestCode == RESULT_NEW_INKER && data != null -> {
                this.issue.inkerId = data.getSerializableExtra(ARG_CREATOR_ID) as UUID
                saveChanges()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        saveChanges()
        if (issue.seriesId == NEW_SERIES_ID || series.seriesName == "New Series") {
            issueDetailViewModel.deleteIssue(issue)
        }
    }

    private fun saveChanges() {
        issueDetailViewModel.updateIssue(issue)
        issueDetailViewModel.loadIssue(issue.issueId)
        updateUI()
    }

    private fun updateUI() {
        seriesSpinner.setSelection(maxOf(0, seriesList.indexOf(series)))
        writerSpinner.setSelection(maxOf(0, writersList.indexOf(writer)))
        pencillerSpinner.setSelection(maxOf(0, writersList.indexOf(penciller)))
        inkerSpinner.setSelection(maxOf(0, writersList.indexOf(inker)))

        issueNumEditText.setText(
            if (this.issue.issueNum == Int.MAX_VALUE) {
                "1"
            } else {
                this.issue.issueNum.toString()
            }
        )

        this.issue.releaseDate?.format(DateTimeFormatter.ofPattern("MMMM d, y"))
            ?.let { releaseDateTextView.text = it }

        this.issue.coverUri?.let {
            coverImageView.setImageURI(this.issue.coverUri)
            coverImageView.contentDescription = "Issue Cover (set)"
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
                issueDetailViewModel.deleteIssue(issue)
                requireActivity().onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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

    override fun onDateSelected(date: LocalDate) {
        issue.releaseDate = date
        saveChanges()
    }
}

