package com.wtb.comiccollector.repository

import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Deferred
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.time.LocalDate

/**
 * Updater
 *
 * @constructor Create empty Updater
 */
open class Updater {

    companion object {
        private const val TAG = "Updater"

        suspend fun <T : Any, A : Any> runSafely(
            name: String,
            arg: A,
            queryFunction: (A) -> Deferred<T>,
        ): T? {
            return try {
                if (arg !is List<*> || arg.isNotEmpty()) {
                    Log.d(TAG, "Trying to run safely with args: $queryFunction")
                    queryFunction(arg).await()
                } else {
                    null
                }
            } catch (e: SocketTimeoutException) {
                Log.d(TAG, "$name $e")
                null
            } catch (e: ConnectException) {
                Log.d(TAG, "$name $e")
                null
            } catch (e: HttpException) {
                Log.d(TAG, "$name $e")
                null
            }
        }

        suspend fun <T : Any, A : Nothing?> runSafely(
            name: String,
            arg: A,
            queryFunction: () -> Deferred<T>,
        ): T? {
            Log.d(TAG, "Trying to run safely start: $queryFunction")
            val result: T? = try {
                Log.d(TAG, "Trying to run safely return: $queryFunction")
                queryFunction().await()
            } catch (e: SocketTimeoutException) {
                Log.d(TAG, "$name $e")
                null
            } catch (e: ConnectException) {
                Log.d(TAG, "$name $e")
                null
            } catch (e: HttpException) {
                Log.d(TAG, "$name $e")
                null
            }

            return result
        }

        /**
         * Check if stale
         *
         * @param prefsKey
         * @param shelfLife
         * @param prefs
         * @return
         */// TODO: This should probably get moved out of SharedPreferences and stored with each record.
        //  The tradeoff: an extra local db query vs. having a larger prefs which will end up having
        //  a value for every item in the database.
        internal fun checkIfStale(
            prefsKey: String,
            shelfLife: Long,
            prefs: SharedPreferences
        ): Boolean {
            val lastUpdated = LocalDate.parse(prefs.getString(prefsKey, "${LocalDate.MIN}"))
            val isStale = LocalDate.now() > lastUpdated.plusDays(shelfLife)
            return DEBUG || isStale
        }
    }
}