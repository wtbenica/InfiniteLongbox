/*
 * Copyright (c) 2023. Wesley T. Benica
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.benica.infinite_longbox

import androidx.fragment.app.DialogFragment
import kotlinx.coroutines.ExperimentalCoroutinesApi

private const val TAG = "NewSeriesDialogFragment"

private const val RESULT_START_DATE = 33
private const val RESULT_END_DATE = 34

private const val DIALOG_START_DATE = "DialogStartDate"
private const val DIALOG_END_DATE = "DialogEndDate"

const val ARG_SERIES_ID = "seriesId"

/**
 * Currently unused, but if the ability to edit/enter series info is put back, this is the
 * fragment to do it, along with [dev.benica.infinite_longbox.fragments_view_models.SeriesInfoViewModel]
 */
@ExperimentalCoroutinesApi
class SeriesInfoDialogFragment private constructor() : DialogFragment() {
//
//    private val seriesInfoViewModel: SeriesInfoViewModel by viewModels()
//
//    private lateinit var callback: SeriesInfoDialogCallback
//    private lateinit var series: FullSeries
//    private lateinit var publisher: Publisher
//    private lateinit var publisherList: List<Publisher>
//
//    private lateinit var seriesNameEditText: EditText
//    private lateinit var volumeNumberEditText: EditText
//    private lateinit var publisherSpinner: Spinner
//    private lateinit var startDateEditText: TextView
//    private lateinit var endDateEditText: TextView
//    private lateinit var okayButton: Button
//    private lateinit var cancelButton: Button
//
//
//
//    interface SeriesInfoDialogCallback {
//        fun onSaveSeriesClick(dialog: DialogFragment, series: Series)
//        fun onCancelClick(dialog: DialogFragment)
//    }
//
//    override fun onAttach(context: Context) {
//        super.onAttach(context)
//        try {
//            callback = context as SeriesInfoDialogCallback
//        } catch (e: ClassCastException) {
//            throw java.lang.ClassCastException(("$context must implement NewSeriesDialogFragment"))
//        }
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        series = FullSeries(Series(), Publisher(), null, null, null)
//        publisher = Publisher()
//        publisherList = emptyList()
//
//        seriesInfoViewModel.loadSeries(arguments?.getSerializable(ARG_SERIES_ID) as Int)
//    }
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        val view = inflater.inflate(R.layout.dialog_fragment_new_series, container, false)
//
//        seriesNameEditText = view.findViewById(R.id.series_title)
//        seriesNameEditText.requestFocus()
//        volumeNumberEditText = view.findViewById(R.id.volume_num)
//        publisherSpinner = view.findViewById(R.id.publisher_spinner) as Spinner
//        startDateEditText = view.findViewById(R.id.start_date_text_view) as TextView
//        endDateEditText = view.findViewById(R.id.end_date_text_view) as TextView
//        okayButton = view.findViewById(R.id.button2) as Button
//        cancelButton = view.findViewById(R.id.button)
//
//        return view
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        Log.d(TAG, "onViewCreated")
//
//        seriesInfoViewModel.allPublishersLiveData.observe(
//            viewLifecycleOwner,
//            { publisherList ->
//                publisherList?.let {
//                    val adapter = ArrayAdapter(
//                        requireContext(),
//                        android.R.layout.simple_dropdown_item_1line,
//                        publisherList
//                    )
//                    publisherSpinner.adapter = adapter
//
//                    this.publisherList = publisherList
//                }
//            }
//        )
//
//        seriesInfoViewModel.seriesLiveData.observe(
//            viewLifecycleOwner,
//            {
//                it?.let {
//                    series = it
//                    publisher = it.publisher
//                    updateUI()
//                }
//            }
//        )
//    }
//
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        when {
//            resultCode != Activity.RESULT_OK -> return
//            requestCode == RESULT_START_DATE && data != null -> {
//                series.series.startDate = data.getSerializableExtra(ARG_DATE) as LocalDate
//            }
//            requestCode == RESULT_END_DATE && data != null -> {
//                series.series.endDate = data.getSerializableExtra(ARG_DATE) as LocalDate
//            }
//        }
//        updateUI()
//    }
//
//    override fun onStart() {
//        super.onStart()
//
//        seriesNameEditText.addTextChangedListener(
//            SimpleTextWatcher {
//                series.series.seriesName = it.toString()
//            }
//        )
//
//        volumeNumberEditText.addTextChangedListener(
//            SimpleTextWatcher {
//                series.series.volume = try {
//                    it.toString().toInt()
//                } catch (e: Exception) {
//                    1
//                }
//            }
//        )
//
//        // TODO: Deprecated
//        startDateEditText.setOnClickListener {
//            DatePickerFragment.newInstance(LocalDate.now()).apply {
//                setTargetFragment(this@SeriesInfoDialogFragment, RESULT_START_DATE)
//                show(this@SeriesInfoDialogFragment.parentFragmentManager, DIALOG_START_DATE)
//            }
//        }
//
//        // TODO: Deprecated
//        endDateEditText.setOnClickListener {
//            DatePickerFragment.newInstance(LocalDate.now(), reqKey = reqKey).apply {
//                setTargetFragment(this@SeriesInfoDialogFragment, RESULT_END_DATE)
//                show(this@SeriesInfoDialogFragment.parentFragmentManager, DIALOG_END_DATE)
//            }
//        }
//
//        publisherSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
//            override fun onItemSelected(
//                parent: AdapterView<*>?,
//                view: View?,
//                position: Int,
//                id: Long
//            ) {
//                parent?.let {
//                    publisher = it.getItemAtPosition(position) as Publisher
//                    series.series.publisher = publisher.publisherId
//                }
//            }
//
//            override fun onNothingSelected(parent: AdapterView<*>?) {
//
//            }
//        }
//
//        okayButton.setOnClickListener { view ->
//            seriesInfoViewModel.updateSeries(series.series)
//
//            val bundle = Bundle()
//            bundle.putSerializable(ARG_SERIES_ID, series.series.seriesId)
//            val intent = Intent().putExtras(bundle)
//            // TODO: Deprecated
//            targetFragment?.onActivityResult(targetRequestCode, Activity.RESULT_OK, intent)
//
//            callback.onSaveSeriesClick(this, series.series)
//        }
//
//        cancelButton.setOnClickListener { view ->
//            callback.onCancelClick(this)
//        }
//    }
//
//    override fun onResume() {
//        super.onResume()
//
//        val window = dialog?.window
//        val size = Point()
//
//        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R) {
//            context?.display?.getRealSize(size)
//        } else {
//            @Suppress("DEPRECATION")
//            window?.windowManager?.defaultDisplay?.getRealSize(size)
//        }
//
//        window?.setLayout((size.x * .9).toInt(), WRAP_CONTENT)
//        window?.setGravity(Gravity.CENTER)
//    }
//
//    private fun updateUI() {
//        seriesNameEditText.setText(series.series.seriesName)
//        volumeNumberEditText.setText(series.series.volume.toString())
//        publisherSpinner.setSelection(publisherList.indexOf(publisher))
//        startDateEditText.text = series.series.startDate.toString()
//        endDateEditText.text = series.series.endDate.toString()
//    }
//
//    companion object {
//        @JvmStatic
//        fun newInstance(seriesId: Int): SeriesInfoDialogFragment {
//            return SeriesInfoDialogFragment().apply {
//                arguments = Bundle().apply {
//                    putSerializable(ARG_SERIES_ID, seriesId)
//                }
//            }
//        }
//    }
}