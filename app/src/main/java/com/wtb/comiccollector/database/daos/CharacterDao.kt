package com.wtb.comiccollector.database.daos

import androidx.room.Dao
import androidx.room.Query
import com.wtb.comiccollector.database.models.Character
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@Dao
abstract class CharacterDao : BaseDao<Character>() {

    @Query(
        """SELECT cr.*
            FROM character cr
            WHERE cr.name = :name
            AND cr.aka = :aka
        """
    )
    abstract suspend fun getCharacterByNameAndAka(name: String, aka: String): List<Character>?

    @Query(
        """SELECT cr.*
            FROM character cr
            WHERE cr.name = :name
        """
    )
    abstract suspend fun getCharacterByName(name: String): List<Character>?

    suspend fun getCharacterByInfo(name: String, aka: String? = null):List<Character>? {
        return if (aka == null) {
            getCharacterByName(name)
        } else {
            getCharacterByNameAndAka(name, aka)
        }
    }
}
