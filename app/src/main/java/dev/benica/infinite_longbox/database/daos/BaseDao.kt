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

import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import androidx.room.*
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import dev.benica.infinite_longbox.APP
import dev.benica.infinite_longbox.database.models.Credit
import dev.benica.infinite_longbox.database.models.DataModel
import dev.benica.infinite_longbox.database.models.ExCredit
import dev.benica.infinite_longbox.database.models.Issue
import kotlinx.coroutines.ExperimentalCoroutinesApi

private const val TAG = APP + "BaseDao"
const val REQUEST_LIMIT = 30

@ExperimentalCoroutinesApi
@Dao
abstract class BaseDao<T : DataModel>(private val tableName: String) {

    @RawQuery
    protected abstract fun getDataModelByQuery(query: SupportSQLiteQuery): T?

    fun get(id: Int): T? {
        val query = SimpleSQLiteQuery("SELECT * FROM $tableName WHERE ${tableName + "Id"} = $id")

        return getDataModelByQuery(query)
    }


    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun insert(obj: T): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertSus(obj: T): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun insert(obj: List<T>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertSus(obj: List<T>): List<Long>

    @Update
    abstract fun update(obj: T)

    @Update
    abstract fun update(obj: List<T>)

    @Delete
    abstract fun delete(obj: T)

    @Delete
    abstract fun delete(obj: List<T>)

    @Transaction
    open fun upsert(obj: T) {
        val id = insert(obj)
        if (id == -1L) {
            update(obj)
        }
    }

    @Transaction
    open suspend fun upsertSus(obj: T) {
        val id = insert(obj)
        if (id == -1L) {
            update(obj)
        }
    }

    @Synchronized
    @Transaction
    open fun upsert(objList: List<T>) {

        val objClass = if (objList.isNotEmpty()) {
            objList[0]::class.simpleName
        } else {
            "Empty List"
        }

        for (obj in objList) {
            val s = when (obj) {
                is Issue -> "Issue(issueId=${obj.issueId}, seriesId=${obj.series}, variantOf=${obj.variantOf}"
                is Credit -> "Credit(story_id=${obj.story}, nameDetailId=${obj.nameDetail}"
                is ExCredit -> "ExCredit(story_id=${obj.story}, nameDetailId=${obj.nameDetail}"
                else -> obj
            }

            try {
                val insertResult = insert(obj)

                if (insertResult == -1L) {
                    update(obj)
                }
            } catch (sqlEx: SQLiteConstraintException) {
                if (objClass == "Issue")
                    Log.d(APP + "BaseDao", "UGH!: $objClass $s $sqlEx")
            }
        }
    }

    @Transaction
    open suspend fun upsertSus(objList: List<T>): Boolean {
        var success = true
        val objClass = if (objList.isNotEmpty()) {
            objList[0]::class.simpleName
        } else {
            "Empty List"
        }

        for (obj in objList) {
            try {
                val insertResult: Long = insertSus(obj)
                if (insertResult == -1L) {
                    update(obj)
                }
            } catch (sqlEx: SQLiteConstraintException) {
                if (objClass == "Issue")
                    Log.d(
                        TAG,
                        "UGH SUS: $objClass $obj $sqlEx ${sqlEx.stackTrace} ${sqlEx.message}"
                    )
                success = false
            }
        }

        return success
    }

    companion object {
        internal fun <T : DataModel> modelsToSqlIdString(models: Collection<T>) =
            idsToSqlIdString(models.map { it.id })

        private fun idsToSqlIdString(ids: Collection<Int>) =
            ids.toString().replace("[", "(").replace("]", ")")

        internal fun textFilterToString(text: String) =
            "%${text.replace(' ', '%').replace("'", "\'")}%"
    }
}
