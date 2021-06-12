package com.wtb.comiccollector

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.wtb.comiccollector.database.models.Creator
import com.wtb.comiccollector.issue_details.view_models.IssueDetailViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.LocalDate

private const val TAG = "NewCreatorDialogFrag"

private const val RESULT_START_DATE = 33
private const val RESULT_END_DATE = 34

private const val DIALOG_START_DATE = "DialogStartDate"
private const val DIALOG_END_DATE = "DialogEndDate"

const val ARG_CREATOR_ID = "seriesId"

@ExperimentalCoroutinesApi
class NewCreatorDialogFragment : DialogFragment(),
    DatePickerFragment.Callbacks {

    private lateinit var listener: NewCreatorDialogListener

    private lateinit var firstNameEditText: EditText
    private lateinit var middleNameEditText: EditText
    private lateinit var lastNameEditText: EditText
    private lateinit var suffixSpinner: Spinner
    private lateinit var startDateEditText: TextView
    private lateinit var endDateEditText: TextView
    private lateinit var okayButton: Button
    private lateinit var cancelButton: Button

    private val issueDetailViewModel: IssueDetailViewModel by lazy {
        ViewModelProvider(this).get(IssueDetailViewModel::class.java)
    }

    interface NewCreatorDialogListener {
        fun onSaveCreatorClick(dialog: DialogFragment, creator: Creator)
        fun onCancelClick(dialog: DialogFragment)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as NewCreatorDialogListener
        } catch (e: ClassCastException) {
            throw java.lang.ClassCastException(("$context must implement NewCreatorDialogFragment"))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.dialog_fragment_new_creator, container, false)

        firstNameEditText = view.findViewById(R.id.first_name)
        middleNameEditText = view.findViewById(R.id.middle_name)
        lastNameEditText = view.findViewById(R.id.last_name)
        suffixSpinner = view.findViewById(R.id.suffix_spinner) as Spinner
        /***
         * Future implementation
         *         startDateEditText = view.findViewById(R.id.start_date_text_view) as TextView
         *         endDateEditText = view.findViewById(R.id.end_date_text_view) as TextView
         */
        okayButton = view.findViewById(R.id.button2) as Button
        cancelButton = view.findViewById(R.id.button)

        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.suffixes_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            suffixSpinner.adapter = adapter
        }

        return view
    }

    // Future implementation
/*
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when {
            resultCode != Activity.RESULT_OK -> return
            requestCode == RESULT_START_DATE && data != null -> {
                startDateEditText.text =
                    (data.getSerializableExtra(ARG_DATE) as LocalDate).toString()
            }
            requestCode == RESULT_END_DATE && data != null -> {
                endDateEditText.text = (data.getSerializableExtra(ARG_DATE) as LocalDate).toString()
            }
        }
    }
*/

    override fun onStart() {
        super.onStart()

// Future implementation
/*
        startDateEditText.setOnClickListener {
            DatePickerFragment.newInstance(LocalDate.now()).apply {
                setTargetFragment(this@NewCreatorDialogFragment, RESULT_START_DATE)
                show(this@NewCreatorDialogFragment.parentFragmentManager, DIALOG_START_DATE)
            }
        }

        endDateEditText.setOnClickListener {
            DatePickerFragment.newInstance(LocalDate.now()).apply {
                setTargetFragment(this@NewCreatorDialogFragment, RESULT_END_DATE)
                show(this@NewCreatorDialogFragment.parentFragmentManager, DIALOG_END_DATE)
            }
        }

*/
        okayButton.setOnClickListener { view ->
            // TODO: Need to validate to make sure that firstName is not blank
            val creator = Creator(
                name = firstNameEditText.text.toString(),
                sortName = lastNameEditText.text.toString()
            )

            issueDetailViewModel.upsertCreator(creator)

            val bundle = Bundle()
            bundle.putSerializable(ARG_CREATOR_ID, creator.creatorId)
            val intent = Intent().putExtras(bundle)
            targetFragment?.onActivityResult(targetRequestCode, Activity.RESULT_OK, intent)

            listener.onSaveCreatorClick(this, creator)
        }

        cancelButton.setOnClickListener { view ->
            listener.onCancelClick(this)
        }
    }

    private fun isNoneOrBlank(str: String?) = if (str == "None" || str == "") {
        null
    } else {
        str
    }

    override fun onResume() {
        super.onResume()

        val window = dialog?.window
        val size = Point()

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R) {
            context?.display?.getRealSize(size)
        } else {
            @Suppress("DEPRECATION")
            window?.windowManager?.defaultDisplay?.getRealSize(size)
        }

        window?.setLayout((size.x * .9).toInt(), (size.y * .9).toInt())
        window?.setGravity(Gravity.CENTER)
    }

    override fun onDateSelected(date: LocalDate) {

    }
}