package com.wtb.comiccollector.repository

import android.util.Log
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.database.IssueDatabase
import com.wtb.comiccollector.database.daos.BaseDao
import com.wtb.comiccollector.database.models.*
import com.wtb.comiccollector.repository.Updater.PriorityDispatcher.Companion.lowPriorityDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant

@ExperimentalCoroutinesApi
abstract class Collector<ModelType : DataModel>(private val dao: BaseDao<ModelType>) {
    private var count: Int = 0
    private var startTime: Instant? = null
    private var itemList = mutableListOf<ModelType>()

    @Synchronized
    fun collect(result: List<ModelType>?) {
        if (startTime == null) {
            startTime = Instant.now()
        }
        result?.let { itemList.addAll(it) }

        val currentTime = Instant.now()

        if (++count == BUFFER_SIZE) {
            saveList()
        }

        CoroutineScope(lowPriorityDispatcher).launch {
            delay(3000)
            if (currentTime > (startTime?.plusSeconds(UPSERT_WAIT) ?: Instant.MIN) &&
                itemList.isNotEmpty()
            ) {
                Log.d(
                    TAG,
                    "Time's up. Saving the list. ${itemList.size} ${itemList[0]::class}"
                )
                saveList()
            }
        }
    }

    @Synchronized
    private fun saveList() {
        Log.d(TAG, "Saving items from collector ${itemList.size}")
        dao.upsert(itemList)
        itemList.clear()
        count = 0
    }
    
    companion object {
        private const val TAG = APP + "Collector"
        private const val BUFFER_SIZE = 500
        private const val UPSERT_WAIT: Long = 1

        fun initialize(database: IssueDatabase) {
            DATABASE = database
        }

        @Volatile
        private var DATABASE: IssueDatabase? = null

        private val database: IssueDatabase
            get() = DATABASE ?: throw IllegalStateException("Collector must be initialized")

        @Volatile
        private var APP_COLL: AppearanceCollector? = null
        @Volatile
        private var CRE_COLL: CreditCollector? = null
        @Volatile
        private var EXC_COLL: ExCreditCollector? = null
        @Volatile
        private var STO_COLL: StoryCollector? = null
        @Volatile
        private var ISS_COLL: IssueCollector? = null
        @Volatile
        private var NMD_COLL: NameDetailCollector? = null

        fun appearanceCollector(): AppearanceCollector {
            return synchronized(this) {
                APP_COLL ?: AppearanceCollector(database).also {
                    APP_COLL = it
                }
            }
        }

        fun creditCollector(): CreditCollector {
            return synchronized(this) {
                CRE_COLL ?: CreditCollector(database).also {
                    CRE_COLL = it
                }
            }
        }

        fun exCreditCollector(): ExCreditCollector {
            return synchronized(this) {
                EXC_COLL ?: ExCreditCollector(database).also {
                    EXC_COLL = it
                }
            }
        }

        fun storyCollector(): StoryCollector {
            return synchronized(this) {
                STO_COLL ?: StoryCollector(database).also {
                    STO_COLL = it
                }
            }
        }

        fun issueCollector(): IssueCollector {
            return synchronized(this) {
                ISS_COLL ?: IssueCollector(database).also {
                    ISS_COLL = it
                }
            }
        }

        fun nameDetailCollector(): NameDetailCollector {
            return synchronized(this) {
                NMD_COLL ?: NameDetailCollector(database).also {
                    NMD_COLL = it
                }
            }
        }

        class AppearanceCollector(database: IssueDatabase) : Collector<Appearance>(database.appearanceDao())
        class CreditCollector(database: IssueDatabase) : Collector<Credit>(database.creditDao())
        class ExCreditCollector(database: IssueDatabase) : Collector<ExCredit>(database.exCreditDao())
        class StoryCollector(database: IssueDatabase) : Collector<Story>(database.storyDao())
        class IssueCollector(database: IssueDatabase) : Collector<Issue>(database.issueDao())
        class NameDetailCollector(database: IssueDatabase) : Collector<NameDetail>(database.nameDetailDao())
    }
}
