package com.wtb.comiccollector

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.database.sqlite.SQLiteConstraintException
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.wtb.comiccollector.database.IssueDatabase
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDate
import java.util.concurrent.Executors

private const val DATABASE_NAME = "issue-database"
private const val TAG = "IssueRepository"
private const val BASE_URL = "http://192.168.0.141:8000/"

class IssueRepository private constructor(context: Context) {

    private val executor = Executors.newSingleThreadExecutor()

    private val database: IssueDatabase = buildDatabase(context)

    private val issueDao = database.issueDao()

    private val filesDir = context.applicationContext.filesDir

    var allSeries: MutableLiveData<List<Series>> = MutableLiveData(issueDao.getAllSeries().value)

    init {
        val prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val last_updated = prefs.getString("series_list_updated", LocalDate.MIN.toString())

        if (LocalDate.parse(last_updated).plusDays(14) < LocalDate.now()) {
            Log.d(TAG, "init: updating series list from db")

            val retrofit: Retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val apiService: Webservice by lazy {
                retrofit.create(Webservice::class.java)
            }

            val publisherCall: Call<List<Item<GcdPublisherJson>>> = apiService.getPublishers()

            publisherCall.enqueue(
                object : Callback<List<Item<GcdPublisherJson>>> {
                    override fun onResponse(
                        call: Call<List<Item<GcdPublisherJson>>>,
                        response: Response<List<Item<GcdPublisherJson>>>
                    ) {
                        Log.d(TAG, "pubCall onResponse ${call.request()} ${response}")
                        if (response.code() == 200) {
                            val publisherList: List<Item<GcdPublisherJson>>? = response.body()

                            Log.d(TAG, "publisherList: $publisherList")

                            publisherList?.map {
                                Publisher.fromItem(it)
                            }?.let {
                                executor.execute {
                                    issueDao.insertPublisher(*it.toTypedArray())
                                }

                                val editor = prefs.edit()
                                editor.putString(
                                    "publisher_list_updated", LocalDate.now()
                                        .toString()
                                )
                                editor.apply()
                            }
                        }
                    }

                    override fun onFailure(call: Call<List<Item<GcdPublisherJson>>>, t: Throwable) {
                        Log.d(TAG, "pubCall onFailure $call $t")
                        val res = null
                    }
                }
            )

            val seriesCall: Call<List<Item<GcdSeriesJson>>> = apiService.getSeries()

            seriesCall.enqueue(
                object : Callback<List<Item<GcdSeriesJson>>> {
                    override fun onResponse(
                        call: Call<List<Item<GcdSeriesJson>>>,
                        response: Response<List<Item<GcdSeriesJson>>>
                    ) {
                        Log.d(TAG, "seriesCall onResponse ${call.request()} ${response}")
                        if (response.code() == 200) {
                            val seriesList: List<Item<GcdSeriesJson>>? = response.body()

                            Log.d(TAG, "seriesList: $seriesList")

                            seriesList?.map {
                                Series.fromItem(it)
                            }?.let {
                                allSeries.value = it
                                executor.execute {
                                    issueDao.insertSeries(*it.toTypedArray())
                                }

                                val editor = prefs.edit()
                                editor.putString("series_list_updated", LocalDate.now().toString())
                                editor.apply()
                            }
                        }
                    }

                    override fun onFailure(call: Call<List<Item<GcdSeriesJson>>>, t: Throwable) {
                        Log.d(TAG, "seriesCall onFailure ${call.request()} $t")
                        val res = null
                    }
                }
            )
        }
    }

    val allPublishers: LiveData<List<Publisher>> = issueDao.getPublishersList()

    val allCreators: LiveData<List<Creator>> = issueDao.getCreatorsList()

    val allWriters: LiveData<List<Creator>> = issueDao.getWritersList()

    val allRoles: LiveData<List<Role>> = issueDao.getRoleList()

    fun getIssues(): LiveData<List<FullIssue>> = issueDao.getIssues()

    fun getIssue(issueId: Int): LiveData<Issue?> = issueDao.getIssue(issueId)

    fun getIssuesBySeries(seriesId: Int) = issueDao.getIssuesBySeries(seriesId)

    fun getIssuesByDetails(seriesId: Int, issueNum: Int) =
        issueDao.getIssueByDetails(seriesId, issueNum)

    private fun buildDatabase(context: Context) = Room.databaseBuilder(
        context.applicationContext,
        IssueDatabase::class.java,
        DATABASE_NAME
    ).addCallback(
        object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                val publisher =
                    Publisher(publisherId = NEW_SERIES_ID, publisher = "New Publisher")
                executor.execute {
                    issueDao.insertPublisher(
                        publisher,
                    )

                    issueDao.insertSeries(
                        Series(
                            seriesId = NEW_SERIES_ID,
                            seriesName = "New Series",
                            publisherId = NEW_SERIES_ID,
                            startDate = LocalDate.MIN,
                            endDate = LocalDate.MIN
                        )
                    )
                }
            }
        }
    ).build()

    fun addIssue(issue: Issue) {
        executor.execute {
            try {
                issueDao.insertIssue(issue)
            } catch (e: SQLiteConstraintException) {
                // TODO: some real exception handling
                Log.d(TAG, "addIssue: $e")
            }
        }
    }

    fun addSeries(series: Series) {
        executor.execute {
            try {
                issueDao.insertSeries(series)
            } catch (e: SQLiteConstraintException) {
                Log.d(TAG, "addSeries: $e")
            }
        }
    }

    fun addCreator(creator: Creator) {
        executor.execute {
            issueDao.insertCreator(creator)
        }
    }

    fun addRole(role: Role) {
        executor.execute {
            issueDao.insertRole(role)
        }
    }

    fun addCredit(credit: Credit) {
        executor.execute {
            try {
                issueDao.insertCredit(credit)
            } catch (e: SQLiteConstraintException) {
                Log.d(TAG, "addCredit: $e")
                // TODO: notify user that they are updating an existing item
                issueDao.updateCredit(credit)
            }
        }
    }

    fun updateIssue(issue: Issue) {
        executor.execute {
            try {
                issueDao.updateIssue(issue)
            } catch (e: SQLiteConstraintException) {
                // TODO: some real exception handling
                Log.d(TAG, "updateIssue: $e")
            }
        }
    }

    fun updateSeries(series: Series) {
        executor.execute {
            try {
                issueDao.updateSeries(series)
            } catch (e: SQLiteConstraintException) {
                // TODO: some real exception handling
                Log.d(TAG, "updateSeries: $e")
            }
        }
    }

    fun deleteIssue(issue: Issue) {
        executor.execute {
            issueDao.deleteIssue(issue)
        }
    }

    fun deleteSeries(series: Series) {
        executor.execute {
            issueDao.deleteSeries(series)
        }
    }

    fun getSeriesList(): LiveData<List<Series>> {
//        TODO("update this to work with getSeriesList in IssueDao")
        return issueDao.getAllSeries()
    }

    fun getSeries(seriesId: Int): LiveData<Series?> =
        issueDao.getSeriesById(seriesId)

    fun getCreator(creatorId: Int): LiveData<Creator?> {
        return issueDao.getCreator(creatorId)
    }

    fun updateCreator(creator: Creator) {
        executor.execute {
            issueDao.updateCreator(creator)
        }
    }

    fun updateRole(role: Role) {
        executor.execute {
            issueDao.updateRole(role)
        }
    }

    class DuplicateFrament : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            return activity?.let {
                val builder = AlertDialog.Builder(it)
                builder.setTitle("Duplicate Issue")
                    .setMessage("This is a duplicate issue and will not be saved")
                    .setPositiveButton("OK") { dialogInterface: DialogInterface, i: Int -> }
                builder.create()
            } ?: throw IllegalStateException("Activity cannot be null")
        }
    }

    fun updateCredit(credit: Credit) {
        executor.execute {
            issueDao.updateCredit(credit)
        }
    }

    fun deleteCreator(creator: Creator) {
        executor.execute {
            issueDao.deleteCreator(creator)
        }
    }

    fun deleteRole(role: Role) {
        executor.execute {
            issueDao.deleteRole(role)
        }
    }

    fun deleteCredit(credit: Credit) {
        executor.execute {
            issueDao.deleteCredit(credit)
        }
    }

    fun getPublisher(publisherId: Int): LiveData<Publisher?> =
        issueDao.getPublisher(publisherId)

    fun getNewFullIssue(issueId: Int) = issueDao.getNewFullIssue(issueId)

    fun getNewIssueCredits(issueId: Int) = issueDao.getNewIssueCredits(issueId)

    fun getSeriesByCreator(creatorId: Int): LiveData<List<Series>> =
        issueDao.getSeriesList(creatorId)

    fun getCreatorBySeries(seriesId: Int): LiveData<List<Creator>> =
        issueDao.getCreatorList(seriesId)

/*
    FUTURE IMPLEMENTATION
    fun getCoverImage(issue: Issue): File = File(filesDir, issue.coverFileName)
*/

    companion object {
        private var INSTANCE: IssueRepository? = null

        fun initialize(context: Context) {
            if (INSTANCE == null) {
                INSTANCE = IssueRepository(context)
            }
        }

        fun get(): IssueRepository {
            return INSTANCE
                ?: throw IllegalStateException("IssueRepository must be initialized")
        }
    }
}