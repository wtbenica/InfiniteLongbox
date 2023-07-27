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

package dev.benica.infinite_longbox.repository

import android.util.Log
import dev.benica.infinite_longbox.APP
import dev.benica.infinite_longbox.database.IssueDatabase
import dev.benica.infinite_longbox.database.daos.BaseDao
import dev.benica.infinite_longbox.database.models.*
import dev.benica.infinite_longbox.repository.Updater.PriorityDispatcher.Companion.lowPriorityDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant

@ExperimentalCoroutinesApi
abstract class Collector<ModelType : DataModel>(val dao: BaseDao<ModelType>) {
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

        saveList()

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
        private const val BUFFER_SIZE = 10
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

        class AppearanceCollector(database: IssueDatabase) :
            Collector<Appearance>(database.appearanceDao())

        class CreditCollector(database: IssueDatabase) : Collector<Credit>(database.creditDao())
        class ExCreditCollector(database: IssueDatabase) :
            Collector<ExCredit>(database.exCreditDao())

        class StoryCollector(database: IssueDatabase) : Collector<Story>(database.storyDao())
        class IssueCollector(database: IssueDatabase) : Collector<Issue>(database.issueDao())
        class NameDetailCollector(database: IssueDatabase) :
            Collector<NameDetail>(database.nameDetailDao())
    }
}
