package com.wtb.comiccollector

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.*
import androidx.core.view.children
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import java.io.File
import java.util.*

private const val TAG = "IssueFragment"
private const val ARG_ISSUE_ID = "issue_id"
private const val PICK_COVER_IMAGE = 0

/**
 * A simple [Fragment] subclass.
 * Use the [IssueFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class IssueFragment : Fragment() {
    private lateinit var issue: Issue
    private lateinit var series: Series
    private lateinit var coverImageView: ImageView
    private lateinit var seriesSpinner: Spinner
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
    private lateinit var toggleEditButton: Button
    private lateinit var coverFile: File
    private lateinit var coverUri: Uri

    private var saveIssue = true

    private val issueDetailViewModel: IssueDetailViewModel by lazy {
        ViewModelProvider(this).get(IssueDetailViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        series = Series()
        issue = Issue(seriesId = series.seriesId)
        val issueId = arguments?.getSerializable(ARG_ISSUE_ID) as UUID
        issueDetailViewModel.loadIssue(issueId)

        issueDetailViewModel.allSeriesLiveData.observe(this,
            { seriesList ->
                seriesList?.let {
                    val newSeriesList: MutableList<Series> = mutableListOf(series)
                    newSeriesList.addAll(seriesList)
                    val adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        newSeriesList
                    )

                    seriesSpinner.adapter = adapter
                }
            })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_issue_new, container, false)

        seriesSpinner = view.findViewById(R.id.issue_series) as Spinner
        seriesSpinner.prompt = "Series Name"
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
        toggleEditButton = view.findViewById(R.id.edit_button)
        addInkerButton = view.findViewById(R.id.add_inker_button) as ImageButton
        addInkerButton = view.findViewById(R.id.add_inker_button) as ImageButton

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
    }

    override fun onStart() {
        super.onStart()
        attachTextWatchers()
        toggleEditButton.setOnClickListener {
            seriesSpinner.isEnabled = !seriesSpinner.isEnabled
            writerEditText.isEnabled = !writerEditText.isEnabled
            pencillerEditText.isEnabled = !pencillerEditText.isEnabled
            inkerEditText.isEnabled = !inkerEditText.isEnabled
            issueNumEditText.isEnabled = !issueNumEditText.isEnabled
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
    }

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
                if (parent != null) {
                    if ((parent.getItemAtPosition(position) as Series).seriesName == "New Series") {
                        val d = NewSeriesDialogFragment()
                        d.show(parentFragmentManager, "NDF")
                    } else {
                        issueDetailViewModel.allSeriesLiveData.value?.let {
                            series = parent.getItemAtPosition(position) as Series
                            issue.seriesId = series.seriesId
                        }
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
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
            requestCode == PICK_COVER_IMAGE && data != null -> {
                this.issue.coverUri = data.data
                issueDetailViewModel.saveIssue(this.issue)
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
//        seriesSpinner.setText(this.series.seriesName)
        issueNumEditText.setText(this.issue.issueNum.toString())
        writerEditText.setText(this.issue.writer)
        pencillerEditText.setText(this.issue.penciller)
        inkerEditText.setText(this.issue.inker)

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
        fun newInstance(issueId: UUID? = null): IssueFragment =
            IssueFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_ISSUE_ID, issueId)
                }
            }
    }
}

class NewSeriesDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setMessage("Mogwai")
                .setPositiveButton("Yes", DialogInterface.OnClickListener { dialog, id -> })
                .setNegativeButton("No", DialogInterface.OnClickListener { dialog, id -> })
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}