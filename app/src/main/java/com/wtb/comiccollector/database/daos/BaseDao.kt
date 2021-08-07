package com.wtb.comiccollector.database.daos

import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import androidx.room.*
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.database.models.DataModel
import com.wtb.comiccollector.database.models.Issue
import kotlinx.coroutines.ExperimentalCoroutinesApi

private const val TAG = APP + "BaseDao"
const val REQUEST_LIMIT = 20

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

    @Transaction
    open fun upsert(objList: List<T>) {

        val objClass = if (objList.isNotEmpty()) {
            objList[0]::class.simpleName
        } else {
            "Empty List"
        }

        for (obj in objList) {
            try {
                val insertResult = insert(obj)

                if (insertResult == -1L) {
                    update(obj)
                }
            } catch (sqlEx: SQLiteConstraintException) {
                val s = when (obj) {
                    is Issue -> "Issue(issueId=${obj.issueId}, seriesId=${obj.series}, variantOf=${obj.variantOf}"
                    else     -> obj
                }
                Log.d(TAG, "UGH!: $objClass $s $sqlEx")
            }
        }
    }

    @Transaction
    open suspend fun upsertSus(objList: List<T>) {

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
                Log.d(TAG, "UGH SUS: $objClass $obj $sqlEx ${sqlEx.stackTrace} ${
                    sqlEx
                        .message
                }")
            }
        }
    }

    companion object {
        internal fun <T : DataModel> modelsToSqlIdString(models: Collection<T>) =
            idsToSqlIdString(models.map { it.id })

        internal fun idsToSqlIdString(ids: Collection<Int>) =
            ids.toString().replace("[", "(").replace("]", ")")

        internal fun textFilterToString(text: String) = "%${text.replace(' ', '%')}%"
    }
}
