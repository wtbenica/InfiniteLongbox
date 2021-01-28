package com.wtb.comiccollector

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
private const val RESULT_NEW_SERIES = 108

// Fragment Tags
private const val DIALOG_NEW_SERIES = "DialogNewSeries"
private const val DIALOG_DATE = "DialogDate"


/**
 * A simple [Fragment] subclass.
 * Use the [IssueFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
// TODO: Do I need a separate fragment for editing vs viewing or can I do it all in this one?
class IssueFragment : Fragment(),
    DatePickerFragment.Callbacks {

    private lateinit var issue: Issue
    private lateinit var series: Series
    private lateinit var seriesList: List<Series>

    private lateinit var coverImageView: ImageView
    private lateinit var seriesSpinner: Spinner
    private lateinit var addSeriesButton: ImageButton
    private lateinit var issueNumEditText: EditText

    private lateinit var writersBox: TableLayout
    private lateinit var writerEditText: EditText
    private lateinit var addWriterButton: ImageButton

    private lateinit var pencillersBox: TableLayout
    private lateinit var pencillerEditText: EditText
    private lateinit var addPencillerButton: ImageButton

    private lateinit var inkersBox: TableLayout
    private lateinit var inkerEditText: EditText
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
        isEditable = arguments?.getSerializable(ARG_EDITABLE) as Boolean
        val issueId = arguments?.getSerializable(ARG_ISSUE_ID) as UUID
        seriesList = emptyList()
        issueDetailViewModel.loadIssue(issueId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_edit_issue, container, false)

        seriesSpinner = view.findViewById(R.id.issue_series) as Spinner
        seriesSpinner.prompt = "Series Name"
        addSeriesButton = view.findViewById(R.id.add_series_button) as ImageButton
        coverImageView = view.findViewById(R.id.issue_cover) as ImageView
        issueNumEditText = view.findViewById(R.id.issue_number) as EditText
        writersBox = view.findViewById(R.id.writers_box) as TableLayout
        writerEditText = view.findViewById(R.id.issue_writer) as EditText
        addWriterButton = view.findViewById(R.id.add_writer_button) as ImageButton
        pencillersBox = view.findViewById(R.id.pencillers_box) as TableLayout
        pencillerEditText = view.findViewById(R.id.issue_penciller) as EditText
        addPencillerButton = view.findViewById(R.id.add_penciller_button) as ImageButton
        inkersBox = view.findViewById(R.id.inkers_box) as TableLayout
        inkerEditText = view.findViewById(R.id.issue_inker) as EditText
        releaseDateTextView = view.findViewById(R.id.release_date_text_view)
        toggleEditButton = view.findViewById(R.id.edit_button) as ImageButton
        addInkerButton = view.findViewById(R.id.add_inker_button) as ImageButton
        addInkerButton = view.findViewById(R.id.add_inker_button) as ImageButton

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        issueDetailViewModel.allSeriesLiveData.observe(viewLifecycleOwner,
            { allSeries ->
                allSeries?.let {
                    val adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        allSeries
                    )
                    seriesList = allSeries
                    seriesSpinner.adapter = adapter

                    if (seriesList.isEmpty()) {
                        Log.d(TAG, "series list is zero")
                        val d = NewSeriesDialogFragment()
                        d.setTargetFragment(this@IssueFragment, RESULT_NEW_SERIES)
                        d.show(parentFragmentManager, DIALOG_NEW_SERIES)
                    } else if (issue.seriesId == NEW_SERIES_ID) {
                        issue.seriesId = seriesList[0].seriesId
                    }
                }
            })

        issueDetailViewModel.issueLiveData.observe(
            viewLifecycleOwner,
            { issue ->
                issue?.let {
                    this.issue = issue
                    updateUI()
                }
            }
        )

        issueDetailViewModel.seriesLiveData.observe(
            viewLifecycleOwner,
            { series ->
                series?.let {
                    this.series = series
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

        addSeriesButton.setOnClickListener {
            val d = NewSeriesDialogFragment()
            d.setTargetFragment(this@IssueFragment, RESULT_NEW_SERIES)
            d.show(parentFragmentManager, DIALOG_NEW_SERIES)
        }

        releaseDateTextView.setOnClickListener {
            DatePickerFragment.newInstance(issue.releaseDate).apply {
                setTargetFragment(this@IssueFragment, RESULT_DATE_PICKER)
                show(this@IssueFragment.parentFragmentManager, DIALOG_DATE)
            }
        }

        if (!isEditable) {
            val indexOf = this.seriesList.indexOf(series)
            seriesSpinner.setSelection(indexOf)

        }

    }

    private fun toggleEnable() {
        seriesSpinner.isEnabled = !seriesSpinner.isEnabled
        writerEditText.isEnabled = !writerEditText.isEnabled
        addWriterButton.isVisible = !addWriterButton.isVisible
        pencillerEditText.isEnabled = !pencillerEditText.isEnabled
        addPencillerButton.isVisible = !addPencillerButton.isVisible
        inkerEditText.isEnabled = !inkerEditText.isEnabled
        addInkerButton.isVisible = !addInkerButton.isVisible
        issueNumEditText.isEnabled = !issueNumEditText.isEnabled
        releaseDateTextView.isEnabled = !releaseDateTextView.isEnabled
    }

    // TODO: Add textWatchers. as of now, they editTexts dont save anything
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
        val issueNumWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(
                sequence: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                issue.issueNum = try {
                    sequence.toString().toInt()
                } catch (e: Exception) {
                    1
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        }

        val writerWatcher = object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(
                sequence: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                issue.writer = sequence.toString()
            }

            override fun afterTextChanged(s: Editable?) {}

        }

        val pencillerWatcher = object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(
                sequence: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                issue.penciller = sequence.toString()
            }

            override fun afterTextChanged(s: Editable?) {}

        }


        val inkerWatcher = object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(
                sequence: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                issue.inker = sequence.toString()
            }

            override fun afterTextChanged(s: Editable?) {}

        }

        seriesSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                parent?.let {
                    // TODO: NewSeriesDialog opens even when it shouldn't
                    //  bc spinner is always initially NEW_SERIES_ID
                    Log.d(TAG, "series spinner set to something other than new")
                    issueDetailViewModel.allSeriesLiveData.value?.let {
                        series = parent.getItemAtPosition(position) as Series
                        issue.seriesId = series.seriesId
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // TODO: Not yet implemented
            }

        }
        issueNumEditText.addTextChangedListener(issueNumWatcher)
        writerEditText.addTextChangedListener(writerWatcher)
        pencillerEditText.addTextChangedListener(pencillerWatcher)
        inkerEditText.addTextChangedListener(inkerWatcher)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        when {
            resultCode != Activity.RESULT_OK -> return
            requestCode == RESULT_NEW_SERIES && data != null -> {
                this.issue.seriesId = data.getSerializableExtra(ARG_SERIES_ID) as UUID
                issueDetailViewModel.saveIssue(this.issue)
                issueDetailViewModel.loadIssue(this.issue.issueId)
                updateUI()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        issueDetailViewModel.saveIssue(this.issue)
        issueDetailViewModel.saveSeries(this.series)
    }

    private fun updateUI() {
        seriesSpinner.setSelection(seriesList.indexOf(series))
        issueNumEditText.setText(
            if (this.issue.issueNum == Int.MAX_VALUE) {
                "1"
            } else {
                this.issue.issueNum.toString()
            }
        )
        writerEditText.setText(this.issue.writer)
        pencillerEditText.setText(this.issue.penciller)
        inkerEditText.setText(this.issue.inker)
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
        fun newInstance(issueId: UUID? = null, openAsEditable: Boolean = true): IssueFragment =
            IssueFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_ISSUE_ID, issueId)
                    putSerializable(ARG_EDITABLE, openAsEditable)
                }
            }
    }

    override fun onDateSelected(date: LocalDate) {
        issue.releaseDate = date
        updateUI()
    }
}

