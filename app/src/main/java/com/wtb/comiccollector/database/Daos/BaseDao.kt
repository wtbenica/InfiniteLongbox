package com.wtb.comiccollector.database.Daos

import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import androidx.room.*
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.database.models.DataModel

private const val TAG = APP + "BaseDao"
const val REQUEST_LIMIT = 50

/**
 * BaseDao provides generic insert, update, delete, and upsert (insert if not exist, else update)
 * I was having a problem where insert(REPLACE) is actually "try insert, if exists, delete then
 * insert," which, along with "on delete cascade" resulted in lost records
 */
@Dao
abstract class BaseDao<T : DataModel> {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun insert(obj: T): Long

    @Transaction
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun insert(obj: List<T>): List<Long>

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
    open fun upsert(objList: List<T>) {

        val insertResult = insert(objList)
        val updateList = mutableListOf<T>()

        for (i in insertResult.indices) {
            if (insertResult[i] == -1L) {
                updateList.add(objList[i])
            }
        }

        if (updateList.isNotEmpty()) {
            update(updateList)
        }
    }

    @Transaction
    open suspend fun upsertSus(objList: List<T>) {

        try {
            val insertResult = insert(objList)

            val updateList = mutableListOf<T>()

            for (i in insertResult.indices) {
                if (insertResult[i] == -1L) {
                    updateList.add(objList[i])
                }
            }

            if (updateList.isNotEmpty()) {
                update(updateList)
            }
        } catch (e: SQLiteConstraintException) {
            Log.d(TAG, "upsert Credit: $e")
        }

    }

    protected fun <T: DataModel> modelsToSqlIdString(mCreators: MutableSet<T>) =
        mCreators.map { it.id }.toString().replace(
            "[", "" +
                    "("
        ).replace("]", ")")
}