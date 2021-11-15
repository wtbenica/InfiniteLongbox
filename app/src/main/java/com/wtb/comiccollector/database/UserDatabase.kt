package com.wtb.comiccollector.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.wtb.comiccollector.database.daos.CollectionItemDao
import com.wtb.comiccollector.database.daos.CoverDao
import com.wtb.comiccollector.database.daos.UserCollectionDao
import com.wtb.comiccollector.database.models.BaseCollection
import com.wtb.comiccollector.database.models.CollectionItem
import com.wtb.comiccollector.database.models.Cover
import com.wtb.comiccollector.database.models.UserCollection
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.util.concurrent.Executors

private const val USER_DATABASE_NAME = "user-database"

@ExperimentalCoroutinesApi
@Database(
    entities = [Cover::class, UserCollection::class, CollectionItem::class],
    version = 1,
)
@TypeConverters(IssueTypeConverters::class)
abstract class UserDatabase : RoomDatabase() {

    abstract fun userCollectionDao(): UserCollectionDao
    abstract fun collectionItemDao(): CollectionItemDao
    abstract fun coverDao(): CoverDao

    companion object {
        @Volatile
        private var INSTANCE: UserDatabase? = null

        fun getInstance(context: Context): UserDatabase {
            return INSTANCE ?: synchronized(this) {
                val executor = Executors.newSingleThreadExecutor()
                return Room.databaseBuilder(
                    context.applicationContext,
                    UserDatabase::class.java,
                    USER_DATABASE_NAME
                ).addCallback(
                    object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)

                            val myCollection =
                                UserCollection(BaseCollection.MY_COLL.id, "My Collection", true)
                            val wishList = UserCollection(
                                BaseCollection.WISH_LIST.id, "Wish List", true
                            )

                            executor.execute {
                                getInstance(context).userCollectionDao().upsert(
                                    listOf(myCollection, wishList)
                                )
                            }
                        }
                    }
                )
                    .build().also {
                        INSTANCE = it
                    }
            }
        }
    }
}
