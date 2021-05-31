package com.wtb.comiccollector.database.Daos

import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import androidx.room.*
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.database.models.DataModel

private const val TAG = APP + "BaseDao"
const val REQUEST_LIMIT = 40

/**
 * BaseDao provides generic insert, update, delete, and upsert (insert if not exist, else update)
 * I was having a problem where insert(REPLACE) is actually "try insert, if exists, delete then
 * insert," which, along with "on delete cascade" resulted in lost records
 */
@Dao
abstract class BaseDao<T : DataModel> {
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

        try {
            val insertResult = insert(objList)
            val updateList = mutableListOf<T>()

            for (i in insertResult.indices) {
                if (insertResult[i] == -1L) {
                    updateList.add(objList[i])
                }
            }

            if (updateList.isNotEmpty()) {
                for (obj in updateList) {
                    update(obj)
                }
            }
        } catch (sqlEx: SQLiteConstraintException) {
            Log.d(TAG, "upsert(objList): ${this.javaClass} $sqlEx ${objList[0]} ${objList.size}")
        }
    }

    @Transaction
    open suspend fun upsertSus(objList: List<T>) {

        try {
            val insertResult: List<Long> = insertSus(objList)
            val updateList = mutableListOf<T>()

            val objClass = if (objList.size >0) {
                objList[0]::class.toString().split(".").last()
            } else {
                "Empty List"
            }
            Log.d(
                TAG,
                "${objClass} Num Inserted: ${insertResult.count { it == -1L }} / ${insertResult
                    .size}"
            )
            for (i in insertResult.indices) {
                if (insertResult[i] == -1L) {
                    val element = objList[i]
                    updateList.add(element)
                }
            }
            update(updateList)
        } catch (sqlEx: SQLiteConstraintException) {
            Log.d(TAG, "UGH $sqlEx ${sqlEx.stackTrace} ${sqlEx.message}")
        }
    }

    protected fun <T : DataModel> modelsToSqlIdString(models: Collection<T>) =
        models.map { it.id }.toString().replace(
            "[", "" +
                    "("
        ).replace("]", ")")
}
