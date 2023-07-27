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

package dev.benica.infinite_longbox.database.daos

import android.content.Context
import androidx.room.Dao
import androidx.room.Transaction
import dev.benica.infinite_longbox.APP
import dev.benica.infinite_longbox.InfiniteLongboxApplication.Companion.context
import dev.benica.infinite_longbox.database.IssueDatabase
import dev.benica.infinite_longbox.database.models.BondType
import dev.benica.infinite_longbox.database.models.Publisher
import dev.benica.infinite_longbox.database.models.Role
import dev.benica.infinite_longbox.database.models.StoryType
import dev.benica.infinite_longbox.repository.Repository
import dev.benica.infinite_longbox.repository.SHARED_PREFS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

private const val TAG = APP + "TransactionDao"

@ExperimentalCoroutinesApi
@Dao
abstract class TransactionDao {

    private val database: IssueDatabase
        get() = IssueDatabase.getInstance(context!!)

    @Transaction
    open suspend fun upsertStatic(
        publishers: List<Publisher>? = null,
        roles: List<Role>? = null,
        storyTypes: List<StoryType>? = null,
        bondTypes: List<BondType>? = null,
    ) {
        publishers?.let { if (it.isNotEmpty()) database.publisherDao().upsertSus(it) }
        roles?.let { if (it.isNotEmpty()) database.roleDao().upsertSus(it) }
        storyTypes?.let { if (it.isNotEmpty()) database.storyTypeDao().upsertSus(it) }
        bondTypes?.let { if (it.isNotEmpty()) database.bondTypeDao().upsertSus(it) }
    }

    fun cleanDatabase() {
        CoroutineScope(Dispatchers.IO).launch {
            database.collectionItemDao().dropAll()
            database.userCollectionDao().dropAll()
            database.coverDao().dropAll()
            database.appearanceDao().dropAll()
            database.creditDao().dropAll()
            database.exCreditDao().dropAll()
            database.storyDao().dropAll()
            database.issueDao().dropAll()
            context?.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE)?.let {
                Repository.removePrefs(
                    prefs = it,
                    keyPattern = "^((?!_page).)*\$"
                )
            }
        }
    }
}
