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

package dev.benica.infinite_longbox.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.benica.infinite_longbox.database.daos.*
import dev.benica.infinite_longbox.database.models.*
import dev.benica.infinite_longbox.database.models.Issue
import dev.benica.infinite_longbox.repository.DUMMY_ID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.LocalDate
import java.util.concurrent.Executors

private const val DATABASE_NAME = "issue-database"

/**
 * The Room database for this app.
 */
@ExperimentalCoroutinesApi
@Database(
    entities = [Issue::class, Series::class, Creator::class, Role::class, Credit::class,
        Publisher::class, Story::class, ExCredit::class, StoryType::class, NameDetail::class,
        Character::class, Appearance::class, Cover::class, SeriesBond::class, BondType::class,
        Brand::class, UserCollection::class, CollectionItem::class, CopySigner::class],
    version = 3,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3, spec = IssueDatabase.SignedByToCreators::class),
    ],
)
@TypeConverters(IssueTypeConverters::class)
abstract class IssueDatabase : RoomDatabase() {

    /** Who signed a copy became a link to a creator (CopySigner), not text. */
    @DeleteColumn(tableName = "CollectionItem", columnName = "signedBy")
    class SignedByToCreators : AutoMigrationSpec

    abstract fun issueDao(): IssueDao
    abstract fun seriesDao(): SeriesDao
    abstract fun creatorDao(): CreatorDao
    abstract fun storyDao(): StoryDao
    abstract fun publisherDao(): PublisherDao
    abstract fun roleDao(): RoleDao
    abstract fun creditDao(): CreditDao
    abstract fun exCreditDao(): ExCreditDao
    abstract fun storyTypeDao(): StoryTypeDao
    abstract fun nameDetailDao(): NameDetailDao
    abstract fun transactionDao(): TransactionDao
    abstract fun characterDao(): CharacterDao
    abstract fun appearanceDao(): AppearanceDao
    abstract fun userCollectionDao(): UserCollectionDao
    abstract fun collectionItemDao(): CollectionItemDao
    abstract fun coverDao(): CoverDao
    abstract fun bondTypeDao(): BondTypeDao
    abstract fun seriesBondDao(): SeriesBondDao

    companion object {
        @Volatile
        private var INSTANCE: IssueDatabase? = null

        /**
         * The one database. Room tells a list about changes made through the same instance only,
         * so a second instance on the file (two threads asking at once, the lock not checked
         * again once held) means lists that never hear about writes made through the other.
         */
        fun getInstance(context: Context): IssueDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: build(context).also { INSTANCE = it }
            }

        private fun build(context: Context): IssueDatabase {
            val executor = Executors.newSingleThreadExecutor()
            return Room.databaseBuilder(
                context.applicationContext,
                IssueDatabase::class.java,
                DATABASE_NAME
            ).addCallback(
                object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        val publisher =
                            Publisher(publisherId = DUMMY_ID, publisher = "Dummy Publisher")
                        executor.execute {
                            getInstance(context).publisherDao().upsert(
                                publisher,
                            )

                            getInstance(context).seriesDao().upsert(
                                Series(
                                    seriesId = DUMMY_ID,
                                    seriesName = "Dummy Series",
                                    publisher = DUMMY_ID,
                                    startDate = LocalDate.MIN,
                                    endDate = LocalDate.MIN,
                                )
                            )
                        }

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
                .build()
        }
    }
}
