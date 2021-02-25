package com.wtb.comiccollector

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*
import androidx.core.view.children
import androidx.core.view.contains
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList

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
class IssueDetailFragment : Fragment(),
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

    private lateinit var issueCreditsLabel: TextView
    private lateinit var issueCreditsFrame: LinearLayout
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

        val view = inflater.inflate(R.layout.fragment_edit_issue_new, container, false)

        seriesSpinner = view.findViewById(R.id.issue_series) as Spinner

        coverImageView = view.findViewById(R.id.issue_cover) as ImageView

        issueNumEditText = view.findViewById(R.id.issue_number) as EditText

        issueCreditsFrame = view.findViewById(R.id.issue_credits_table) as LinearLayout

        releaseDateTextView = view.findViewById(R.id.release_date_text_view)

        toggleEditButton = view.findViewById(R.id.edit_button) as ImageButton

        issueCreditsLabel = view.findViewById(R.id.issue_credits_box_label) as TextView

        creditsBox = CreditsBox(requireContext())

        issueCreditsFrame.addView(creditsBox)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        issueDetailViewModel.fullIssueLiveData.observe(
            viewLifecycleOwner,
            { issue ->
                issue?.let {
                    this.fullIssue = it
                    Log.d(TAG, this.fullIssue.series.seriesName)
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
                        androidx.appcompat.R.layout.support_simple_spinner_dropdown_item,
                        thisList
                    )
                    seriesList = thisList
                    seriesSpinner.adapter = adapter
                    updateUI()
                }
            })

        issueDetailViewModel.allRolesLiveData.observe(viewLifecycleOwner,
            { allRoles ->
                rolesList = allRoles
            })

        if (!isEditable) {
            // TODO: Create a separate layout for editing vs viewing instead of this
            toggleEnable()
        }
    }

    override fun onStart() {
        super.onStart()
        attachLabelListeners()
        toggleEditButton.setOnClickListener { toggleEnable() }

        attachSeriesListener()
        attachIssueNumListener()
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
                val myCredit: FullCredit? = getCredit("Writer")
                myCredit?.let { fullCredit ->
                    fullCredit.credit.creatorId = data.getSerializableExtra(ARG_CREATOR_ID) as UUID
                    issueDetailViewModel.updateCredit(fullCredit.credit)
                }
            }
            requestCode == RESULT_NEW_PENCILLER && data != null -> {
                val myCredit: FullCredit? = getCredit("Penciller")
                myCredit?.let { fullCredit ->
                    fullCredit.credit.creatorId = data.getSerializableExtra(ARG_CREATOR_ID) as UUID
                    issueDetailViewModel.updateCredit(fullCredit.credit)
                }
            }
            requestCode == RESULT_NEW_INKER && data != null -> {
                val myCredit: FullCredit? = getCredit("Inker")
                myCredit?.let { fullCredit ->
                    fullCredit.credit.creatorId = data.getSerializableExtra(ARG_CREATOR_ID) as UUID
                    issueDetailViewModel.updateCredit(fullCredit.credit)
                }
            }
        }
    }

    override fun onStop() {
        Log.d(TAG, "onStop")
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

    private fun saveChanges() {
        issueDetailViewModel.updateIssue(fullIssue.issue)
        issueDetailViewModel.loadIssue(fullIssue.issue.issueId)
        for (credit in creditsBox.getCredits()) {
            issueDetailViewModel.addCredit(credit)
        }
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

    private fun updateUI() {
        numUpdates += 1
        Log.d(TAG, "$numUpdates updates *****************************************************")

        seriesSpinner.setSelection(maxOf(0, seriesList.indexOf(fullIssue.series)))

        // Update creators table
        creditsBox.displayCredit(issueCredits)

        issueNumEditText.setText(this.fullIssue.issue.issueNum.toString())

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
                Log.d(TAG, "seriesSpinner ItemSelected")
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

    //    private fun attachAddCreditListeners() {
//        addCreatorRowButton.setOnClickListener()
//    }
//
    private fun attachLabelListeners() {
        issueCreditsLabel.setOnClickListener { addCreator(RESULT_NEW_WRITER) }
    }

    private fun getRoleByName(roleName: String): Role? {
        for (role in rolesList) {
            if (role.roleName == roleName) {
                return role
            }
        }
        return null
    }

    private fun toggleEnable() {
        seriesSpinner.isEnabled = !seriesSpinner.isEnabled

        creditsBox.isEnabled = !creditsBox.isEnabled

        issueNumEditText.isEnabled = !issueNumEditText.isEnabled
        releaseDateTextView.isEnabled = !releaseDateTextView.isEnabled

    }

    private fun getCredit(type: String): FullCredit? {
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

    inner class CreditsBox(context: Context) : TableLayout(context) {

        private val addRowButton: ImageButton

        init {
            addRowButton = ImageButton(context)
            addRowButton.setImageResource(R.drawable.ic_menu_add)
            addRowButton.setOnClickListener {
                addNewRow(addRowButton)
            }
            addNewRow(addRowButton)
        }

        private fun addNewRow(button: ImageButton, fullCredit: FullCredit? = null) {
            val prevRow = addRowButton.parent?.let { it as ViewGroup }
            val newRow = CreditsRow(context, fullCredit)
            prevRow?.removeView(addRowButton)
            newRow.addView(button)
            this.addView(newRow)
        }

        fun deleteRow(row: CreditsRow) {
            removeView(row)

            if (row.contains(addRowButton)) {
                row.removeView(addRowButton)
                val lastRow = children.lastOrNull()
                if (lastRow == null) {
                    addNewRow(addRowButton)
                } else {
                    (lastRow as CreditsRow).addView(addRowButton)
                }
            }

            row.getCredit()?.let { issueDetailViewModel.deleteCredit(it) }
        }

        fun getCredits(): ArrayList<Credit> {
            val result = ArrayList<Credit>()
            for (row in this.children) {
                val credit = (row as CreditsRow).getCredit()
                Log.d(TAG, "getCredit: ${credit?.creditId ?: "NONE"}")
                credit?.let { result.add(it) }
            }
            return result
        }

        fun displayCredit(credits: List<FullCredit>) {
            if (!credits.isEmpty()) {
                this.removeAllViews()
                for (credit in credits) {
                    this.addNewRow(addRowButton, credit)
                }
            }
        }

        override fun setEnabled(enabled: Boolean) {
            super.setEnabled(enabled)

            for (child in children) {
                child.isEnabled = enabled
            }
        }
    }

    inner class CreditsRow(context: Context, private val fullCredit: FullCredit?) :
        TableRow(context) {

        private var creatorSpinner: Spinner = Spinner(context)
        private var roleSpinner: Spinner = Spinner(context)
        private var deleteRowButton: ImageButton = ImageButton(context)

        private var creatorsList: List<Creator> = emptyList()
        private var roleList: List<Role> = emptyList()

        private var credit: Credit = fullCredit?.credit ?: Credit(
            issueId = fullIssue.issue.issueId,
            creatorId = NEW_SERIES_ID,
            roleId = NEW_SERIES_ID
        )

        init {
            creatorSpinner.layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
            this.addView(creatorSpinner)
            creatorSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val creator = creatorSpinner.getItemAtPosition(position) as Creator
                    Log.d(TAG, "Setting creator: ${creator.name}")
                    credit.creatorId = creator.creatorId
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // Do nothing
                }

            }

            roleSpinner.layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
            this.addView(roleSpinner)
            roleSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val role = roleSpinner.getItemAtPosition(position) as Role
                    Log.d(TAG, "Setting role: ${role.roleName}")
                    credit.roleId = role.roleId
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // Do nothing
                }

            }

            deleteRowButton.setImageResource(R.drawable.ic_menu_delete)
            this.addView(deleteRowButton)
            deleteRowButton.setOnClickListener { (parent as CreditsBox).deleteRow(this) }

            issueDetailViewModel.allCreatorsLiveData.observe(
                viewLifecycleOwner,
                {
                    creatorsList = listOf(Creator(NEW_SERIES_ID, firstName = "Creator")) + it
                    creatorSpinner.adapter = CreatorAdapter(
                        context,
                        creatorsList
                    )
                    if (fullCredit != null) {
                        creatorSpinner.setSelection(creatorsList.indexOf(fullCredit.creator))
                    }
                }
            )

            issueDetailViewModel.allRolesLiveData.observe(
                viewLifecycleOwner,
                {
                    roleList = listOf(Role(NEW_SERIES_ID, roleName = "Role", sortOrder = 0)) + it
                    roleSpinner.adapter = ArrayAdapter(
                        context,
                        androidx.appcompat.R.layout.support_simple_spinner_dropdown_item,
                        roleList
                    )
                    if (fullCredit != null) {
                        roleSpinner.setSelection(roleList.indexOf(fullCredit.role))
                    }
                }
            )
        }

        fun getCredit(): Credit? {
            return if (credit.creatorId == NEW_SERIES_ID || credit.roleId == NEW_SERIES_ID) {
                null
            } else {
                Log.d(TAG, "Returning Credit: ${credit.creatorId} ${credit.roleId}")
                credit
            }
        }

        override fun setEnabled(enabled: Boolean) {
            super.setEnabled(enabled)

            creatorSpinner.isEnabled = enabled
            roleSpinner.isEnabled = enabled
        }
    }
}

class CreatorAdapter(context: Context, data: List<Creator>) :
    ArrayAdapter<Creator>(
        context,
        androidx.appcompat.R.layout.support_simple_spinner_dropdown_item,
        data
    ),
    SpinnerAdapter {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

        val creator: Creator? = getItem(position)

        val res = convertView
            ?: LayoutInflater.from(context).inflate(
                androidx.appcompat.R.layout.support_simple_spinner_dropdown_item,
                parent,
                false
            )

        val text1 = res.findViewById(android.R.id.text1) as TextView
        text1.setText(creator?.toString())

        return text1
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val creator: Creator? = getItem(position)

        val res = convertView
            ?: LayoutInflater.from(context).inflate(
                androidx.appcompat.R.layout.support_simple_spinner_dropdown_item,
                parent,
                false
            )

        val text1 = res.findViewById(android.R.id.text1) as TextView
        text1.setText(creator?.sortName)

        return text1
    }
}
