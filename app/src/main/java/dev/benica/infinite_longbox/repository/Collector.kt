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

import dev.benica.infinite_longbox.database.IssueDatabase
import dev.benica.infinite_longbox.database.daos.BaseDao
import dev.benica.infinite_longbox.database.models.*
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
abstract class Collector<ModelType : DataModel>(val dao: BaseDao<ModelType>) {
    companion object {
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

        class AppearanceCollector(database: IssueDatabase) :
            Collector<Appearance>(database.appearanceDao())

        class CreditCollector(database: IssueDatabase) : Collector<Credit>(database.creditDao())
        class ExCreditCollector(database: IssueDatabase) :
            Collector<ExCredit>(database.exCreditDao())

        class StoryCollector(database: IssueDatabase) : Collector<Story>(database.storyDao())
        class IssueCollector(database: IssueDatabase) : Collector<Issue>(database.issueDao())
    }
}
