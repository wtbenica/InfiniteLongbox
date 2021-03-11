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
import com.wtb.comiccollector.database.IssueDatabase
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
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

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val apiService: Webservice = retrofit.create(Webservice::class.java)

    private val call: Call<List<JsonRead.Item>> = apiService.getSeries()

    init {
        Log.d("POTATO", "graham cracker")
        call.enqueue(
            object : Callback<List<JsonRead.Item>> {
                override fun onResponse(
                    call: Call<List<JsonRead.Item>>, response: Response<List<JsonRead.Item>>
                ) {
                    Log.d(TAG, "call onResponse ${call.request()} ${response}")
                    val statusCode: Int = response.code()
                    val seriesList: List<JsonRead.Item>? = response.body()
                    Log.d(TAG, "seriesList: $seriesList")
                    seriesList?.let {
                        allSeries.value = seriesList.map {
                            Series.fromItem(it)
                        }
                    }
                    allSeries.value?.let {
                        val series = it.toTypedArray()
                        executor.execute {
                            issueDao.updateSeries(*series)
                        }
                    }
                }

                override fun onFailure(call: Call<List<JsonRead.Item>>, t: Throwable) {
                    Log.d(TAG, "call onFailure $call $t")
                    val res = null
                }
            })
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
    )
//        .addCallback(
//        object : RoomDatabase.Callback() {
//            override fun onCreate(db: SupportSQLiteDatabase) {
//                super.onCreate(db)
//                executor.execute {
//                    val publisherDC = Publisher(publisherId = Random().nextInt(), publisher = "DC")
//
//                    issueDao.insertPublisher(
//                        publisherDC,
//                        Publisher(publisherId = NEW_SERIES_ID, publisher = "New Publisher"),
//                        Publisher(publisher = "Marvel"),
//                        Publisher(publisher = "Image"),
//                        Publisher(publisher = "Dark Horse"),
//                        Publisher(publisher = "Valiant"),
//                        Publisher(publisher = "Fantagraphics"),
//                        Publisher(publisher = "Aftershock"),
//                        Publisher(publisher = "DC/Vertigo")
//                    )
//
//                    val writer = Role(roleName = "Writer", sortOrder = 0)
//                    val penciller = Role(roleName = "Penciller", sortOrder = 20)
//                    val inker = Role(roleName = "Inker", sortOrder = 40)
//
//                    issueDao.insertRole(
//                        writer,
//                        Role(roleName = "Plotter", sortOrder = 5),
//                        Role(roleName = "Scripter", sortOrder = 10),
//                        penciller,
//                        Role(roleName = "Artist", sortOrder = 21),
//                        inker,
//                        Role(roleName = "Colorist", sortOrder = 60),
//                        Role(roleName = "Letterer", sortOrder = 80),
//                        Role(roleName = "Cover Artist", sortOrder = 100),
//                        Role(roleName = "Editor", sortOrder = 120),
//                        Role(roleName = "Assistant Editor", sortOrder = 125)
//                    )
//
//                    val grantMorrison = Creator(firstName = "Grant", lastName = "Morrison")
//                    val philipBond = Creator(firstName = "Philip", lastName = "Bond")
//                    val johnNyberg = Creator(firstName = "John", lastName = "Nyberg")
//                    val richardCase = Creator(firstName = "Richard", lastName = "Case")
//
//                    issueDao.insertCreator(
//                        grantMorrison,
//                        philipBond,
//                        johnNyberg,
//                        richardCase,
//                        Creator(firstName = "Neil", lastName = "Gaiman"),
//                        Creator(firstName = "Jason", lastName = "Aaron")
//                    )
//
//                    val seriesDoomPatrol =
//                        Series(
//                            seriesName = "Doom Patrol",
//                            publisherId = publisherDC.publisherId,
//                            startDate = LocalDate.of(1987, 10, 1),
//                            endDate = LocalDate.of(1995, 2, 1)
//                        )
//
//                    issueDao.insertSeries(
//                        Series(
//                            seriesId = NEW_SERIES_ID,
//                            seriesName = "New Series",
//                            publisherId = publisherDC.publisherId,
//                            startDate = LocalDate.of(1995, 5, 13),
//                            endDate = LocalDate.of(2000, 3, 25)
//                        ),
//                        seriesDoomPatrol
//                    )
//
//                    val dp35 = Issue(
//                        seriesId = seriesDoomPatrol.seriesId,
//                        issueNum = 35,
//                        releaseDate = LocalDate.of(1990, 8, 1)
//                    )
//
//                    val dp33 = Issue(
//                        seriesId = seriesDoomPatrol.seriesId,
//                        issueNum = 33,
//                        releaseDate = LocalDate.of(1990, 6, 1)
//                    )
//                    issueDao.insertIssue(
//                        dp33,
//                        dp35
//                    )
//                    issueDao.insertCredit(
//                        Credit(
//                            issueId = dp35.issueId,
//                            creatorId = grantMorrison.creatorId,
//                            roleId = writer.roleId
//                        ),
//                        Credit(
//                            issueId = dp35.issueId,
//                            creatorId = richardCase.creatorId,
//                            roleId = penciller.roleId
//                        ),
//                        Credit(
//                            issueId = dp35.issueId,
//                            creatorId = johnNyberg.creatorId,
//                            roleId = inker.roleId
//                        ),
//                        Credit(
//                            issueId = dp33.issueId,
//                            creatorId = grantMorrison.creatorId,
//                            roleId = writer.roleId
//                        ),
//                        Credit(
//                            issueId = dp33.issueId,
//                            creatorId = richardCase.creatorId,
//                            roleId = penciller.roleId
//                        ),
//                        Credit(
//                            issueId = dp33.issueId,
//                            creatorId = johnNyberg.creatorId,
//                            roleId = inker.roleId
//                        )
//                    )
//                }
//            }
//        }
//    )
    .build()

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

    fun getSeries(seriesId: Int): LiveData<Series?> = issueDao.getSeriesById(seriesId)

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

    fun getPublisher(publisherId: Int): LiveData<Publisher?> = issueDao.getPublisher(publisherId)

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
            return INSTANCE ?: throw IllegalStateException("IssueRepository must be initialized")
        }
    }
}