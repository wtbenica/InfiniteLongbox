package com.wtb.comiccollector

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.EditText
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
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
    private lateinit var coverImageView: ImageView
    private lateinit var seriesEditText: EditText
    private lateinit var issueNumEditText: EditText
    private lateinit var writerEditText: EditText
    private lateinit var pencillerEditText: EditText
    private lateinit var inkerEditText: EditText

    private var saveIssue = true

    private val issueDetailViewModel: IssueDetailViewModel by lazy {
        ViewModelProvider(this).get(IssueDetailViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        issue = Issue()
        val issueId = arguments?.getSerializable(ARG_ISSUE_ID) as UUID
        issueDetailViewModel.loadIssue(issueId)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_issue, container, false)

        coverImageView = view.findViewById(R.id.issue_cover) as ImageView
        seriesEditText = view.findViewById(R.id.issue_series) as EditText
        issueNumEditText = view.findViewById(R.id.issue_number) as EditText
        writerEditText = view.findViewById(R.id.issue_writer) as EditText
        pencillerEditText = view.findViewById(R.id.issue_penciller) as EditText
        inkerEditText = view.findViewById(R.id.issue_inker) as EditText

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        issueDetailViewModel.issueLiveData.observe(
            viewLifecycleOwner,
            androidx.lifecycle.Observer { issue ->
                issue?.let {
                    this.issue = issue
                    updateUI()
                }
            }
        )
    }

    override fun onStart() {
        super.onStart()
        attachTextWatchers()

        coverImageView.apply {
            setOnClickListener {
                val getImageIntent =
                    Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                val chooserIntent = Intent.createChooser(getImageIntent, null)
                startActivityForResult(chooserIntent, PICK_COVER_IMAGE)
            }
        }
    }

    private fun attachTextWatchers() {
        val seriesWatcher = object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(
                sequence: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                issue.series = sequence.toString()
            }

            override fun afterTextChanged(s: Editable?) {}

        }

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

        seriesEditText.addTextChangedListener(seriesWatcher)
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
    }

    override fun onDetach() {
        super.onDetach()
    }

    private fun updateUI() {
        seriesEditText.setText(this.issue.series)
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
        fun newInstance(issueId: UUID): IssueFragment =
            IssueFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_ISSUE_ID, issueId)
                }
            }
    }
}