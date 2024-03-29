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
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.benica.infinite_longbox.database.daos.*
import dev.benica.infinite_longbox.database.models.*
import dev.benica.infinite_longbox.database.models.Issue
import dev.benica.infinite_longbox.repository.DUMMY_ID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.intellij.lang.annotations.Language
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
        Brand::class, UserCollection::class, CollectionItem::class],
    version = 1,
)
@TypeConverters(IssueTypeConverters::class)
abstract class IssueDatabase : RoomDatabase() {

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

        fun getInstance(context: Context): IssueDatabase {
            return INSTANCE ?: synchronized(this) {
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
//                    .createFromAsset(DATABASE_NAME)
//                    .addMigrations(
//
//                    )
                    .build().also {
                        INSTANCE = it
                    }
            }
        }

        @Language("RoomSql")
        val migration_3_4 = SimpleMigration(
            3, 4,
            """CREATE TABLE 'CollectionItem' (
                            'collectionItemId' INTEGER NOT NULL,
                            'issue' INTEGER NOT NULL REFERENCES Issue(issueId) ON DELETE CASCADE,
                            'series' INTEGER NOT NULL REFERENCES Series(seriesId) ON DELETE CASCADE,
                            'userCollection' INTEGER NOT NULL REFERENCES UserCollection
                            (userCollectionId) ON DELETE CASCADE,
                            'lastUpdated' TEXT NOT NULL,
                            PRIMARY KEY('collectionItemId')
                            )""",
            """CREATE TABLE 'UserCollection' (
                            'userCollectionId' INTEGER NOT NULL,
                            'name' TEXT NOT NULL,
                            'permanent' INTEGER NOT NULL,
                            'lastUpdated' TEXT NOT NULL,
                            PRIMARY KEY('userCollectionId')
                            )""",
            """CREATE UNIQUE INDEX index_CollectionItem_issue_userCollection
                ON CollectionItem(issue, userCollection)
            """,
            """CREATE INDEX index_CollectionItem_series
                ON CollectionItem(series)
            """,
            """INSERT INTO UserCollection(userCollectionId, name, permanent, lastUpdated)
                VALUES (${BaseCollection.MY_COLL.id}, "My Collection", 0, DATETIME()),
                (${BaseCollection.WISH_LIST.id}, "Wish List", 0, DATETIME())
            """
        )

        /*
        I'm leaving these here as templates
         */
//        @Language("RoomSql")
//        val migration_1_2 = SimpleMigration(
//            1, 2,
//            """DROP INDEX index_MyCollection_series""",
//            """CREATE INDEX IF NOT EXISTS index_MyCollection_series ON mycollection(series)""",
//        )
//
//        @Language("RoomSql")
//        val migration_2_3 = SimpleMigration(
//            2, 3,
//            """ALTER TABLE cover ADD COLUMN markedDelete INTEGER NOT NULL DEFAULT 0""",
//            """UPDATE cover SET markedDelete = 0 WHERE issue IN ( SELECT firstIssue from series
//                ) OR issue in ( SELECT issue FROM mycollection )"""
//        )
//
        //        val migration_1_2 = SimpleMigration(
        //            1,
        //            2,
        //            """ALTER TABLE issue ADD COLUMN coverDateLong TEXT""",
        //            "ALTER TABLE issue ADD COLUMN onSaleDateUncertain INTEGER NOT NULL DEFAULT 1",
        //            "ALTER TABLE issue ADD COLUMN coverDate TEXT",
        //            "ALTER TABLE issue ADD COLUMN notes TEXT",
        //        )
        //
        //        @Language("RoomSql")
        //        val migration_2_3 = SimpleMigration(
        //            2, 3,
        //            """ALTER TABLE series ADD COLUMN notes TEXT""",
        //            """ALTER TABLE series ADD COLUMN issueCount INTEGER NOT NULL DEFAULT 0"""
        //        )
//
//                @Language("RoomSql")
//                val migration_3_4 = SimpleMigration(
//                    3, 4,
//                    """ALTER TABLE namedetail ADD COLUMN sortName TEXT"""
//                )
//
        //        @Language("RoomSql")
        //        val migration_4_5 = SimpleMigration(
        //            4, 5,
        //            """ALTER TABLE creator ADD COLUMN bio TEXT"""
        //        )
        //
        //        @Language("RoomSql")
        //        val migration_5_6 = SimpleMigration(
        //            5, 6,
        //            """ALTER TABLE publisher ADD COLUMN yearBegan TEXT""",
        //            """ALTER TABLE publisher ADD COLUMN yearBeganUncertain INTEGER NOT NULL DEFAULT 1""",
        //            """ALTER TABLE publisher ADD COLUMN yearEnded TEXT""",
        //            """ALTER TABLE publisher ADD COLUMN yearEndedUncertain INTEGER NOT NULL DEFAULT 1""",
        //            """ALTER TABLE publisher ADD COLUMN url TEXT"""
        //        )
        //
        //        @Language("RoomSql")
        //        val migration_6_7 = SimpleMigration(
        //            6, 7,
        //            """CREATE TABLE 'BondType' (
        //                'bondTypeId' INTEGER NOT NULL,
        //                'name' TEXT NOT NULL,
        //                'description' TEXT NOT NULL,
        //                'notes' TEXT,
        //                PRIMARY KEY('bondTypeId')
        //                )""",
        //            """CREATE TABLE 'SeriesBond' (
        //                    'bondId' INTEGER NOT NULL,
        //                    'originId' INTEGER NOT NULL,
        //                    'targetId' INTEGER NOT NULL,
        //                    'originIssueId' INTEGER,
        //                    'targetIssueId' INTEGER,
        //                    'bondTypeId' INTEGER NOT NULL,
        //                    'notes' TEXT,
        //                    PRIMARY KEY ('bondId'),
        //                    FOREIGN KEY ('originId') REFERENCES 'Series'('seriesId') ON DELETE CASCADE,
        //                    FOREIGN KEY ('targetId') REFERENCES 'Series'('seriesId') ON DELETE CASCADE,
        //                    FOREIGN KEY ('originIssueId') REFERENCES 'Issue'('issueId') ON DELETE CASCADE,
        //                    FOREIGN KEY ('targetIssueId') REFERENCES 'Issue'('issueId') ON DELETE CASCADE,
        //                    FOREIGN KEY ('bondTypeId') REFERENCES 'BondType'('bondTypeId') ON DELETE RESTRICT
        //                )""",
        //            """CREATE INDEX IF NOT EXISTS 'index_SeriesBond_targetIssueId' ON 'SeriesBond'('targetIssueId')""",
        //            """CREATE INDEX IF NOT EXISTS 'index_SeriesBond_originId' ON 'SeriesBond'('originId')""",
        //            """CREATE INDEX IF NOT EXISTS 'index_SeriesBond_targetId' ON 'SeriesBond'('targetId')""",
        //            """CREATE INDEX IF NOT EXISTS 'index_SeriesBond_originIssueId' ON 'SeriesBond'('originIssueId')""",
        //            """CREATE INDEX IF NOT EXISTS 'index_SeriesBond_bondTypeId' ON 'SeriesBond'('bondTypeId')""",
        //        )
        //
        //        @Language("RoomSql")
        //        val migration_7_8 = SimpleMigration(
        //            7, 8,
        //            """CREATE TABLE Brand (
        //                brandId INTEGER NOT NULL,
        //                name TEXT NOT NULL,
        //                yearBegan TEXT,
        //                yearEnded TEXT,
        //                notes TEXT,
        //                url TEXT,
        //                issueCount INTEGER NOT NULL,
        //                yearBeganUncertain INTEGER NOT NULL,
        //                yearEndedUncertain INTEGER NOT NULL,
        //                lastUpdated TEXT NOT NULL,
        //                PRIMARY KEY(brandId)
        //                )
        //            """,
        //            """ALTER TABLE issue ADD COLUMN brandId INTEGER""",
        //            """ALTER TABLE seriesbond ADD COLUMN lastUpdated TEXT NOT NULL DEFAULT '1900-01-01'""",
        //            """ALTER TABLE bondtype ADD COLUMN lastUpdated TEXT NOT NULL DEFAULT '1900-01-01'"""
        //        )
        //
        //        @Language("RoomSql")
        //        val migration_8_9 = SimpleMigration(
        //            8, 9,
        //            """ALTER TABLE character ADD COLUMN publisher INTEGER"""
        //        )
        //
        //        @Language("RoomSql")
        //        val migration_9_10 = SimpleMigration(
        //            9, 10,
        //            """DROP TABLE Appearance""",
        //            """DROP TABLE Character""",
        //            """CREATE TABLE Character (
        //                    characterId INTEGER NOT NULL,
        //                    name TEXT NOT NULL,
        //                    alterEgo TEXT,
        //                    publisher INTEGER NOT NULL,
        //                    lastUpdated TEXT NOT NULL,
        //                    PRIMARY KEY (characterId),
        //                    FOREIGN KEY (publisher) REFERENCES Publisher(publisherId) ON DELETE CASCADE
        //                )""",
        //            """CREATE INDEX IF NOT EXISTS index_Character_publisher ON Character(publisher)""",
        //            """CREATE TABLE Appearance (
        //                    appearanceId INTEGER NOT NULL,
        //                    story INTEGER NOT NULL,
        //                    character INTEGER NOT NULL,
        //                    details TEXT,
        //                    notes TEXT,
        //                    membership TEXT,
        //                    lastUpdated TEXT NOT NULL,
        //                    PRIMARY KEY (appearanceId),
        //                    FOREIGN KEY (story) REFERENCES Story(storyId) ON DELETE CASCADE,
        //                    FOREIGN KEY (character) REFERENCES Character(characterId) ON DELETE CASCADE
        //                )""",
        //            """CREATE INDEX IF NOT EXISTS index_Appearance_story ON Appearance(story)""",
        //            """CREATE INDEX IF NOT EXISTS index_Appearance_character ON Appearance(character)""",
        //        )
        //
        //        @Language("RoomSql")
        //        val migration_10_11 = SimpleMigration(
        //            10, 11,
        //            """CREATE INDEX IF NOT EXISTS index_Character_name ON Character(name)""",
        //            """CREATE INDEX IF NOT EXISTS index_Character_alterEgo ON Character(alterEgo)""",
        //            """CREATE INDEX IF NOT EXISTS index_Creator_name ON Creator(name)""",
        //            """CREATE INDEX IF NOT EXISTS index_Creator_alterEgo ON Creator(sortName)""",
        //            """CREATE INDEX IF NOT EXISTS index_NameDetail_name ON NameDetail(name)""",
        //            """CREATE INDEX IF NOT EXISTS index_NameDetail_sortName ON NameDetail(sortName)""",
        //        )
        //
        //        val migration_11_12 = SimpleMigration(
        //            11, 12,
        //            """ALTER TABLE credit
        //                ADD COLUMN issue INTEGER REFERENCES Issue(issueId) ON DELETE CASCADE
        //                """,
        //            """ALTER TABLE credit
        //                ADD COLUMN series INTEGER REFERENCES Series(seriesId) ON DELETE CASCADE
        //            """,
        //            """ALTER TABLE excredit
        //                ADD COLUMN issue INTEGER REFERENCES Issue(issueId) ON DELETE CASCADE
        //            """,
        //            """ALTER TABLE excredit
        //                ADD COLUMN series INTEGER REFERENCES Series(seriesId) ON DELETE CASCADE
        //            """,
        //            """ALTER TABLE appearance
        //                ADD COLUMN issue INTEGER REFERENCES Issue(issueId) ON DELETE CASCADE
        //            """,
        //            """ALTER TABLE appearance
        //                ADD COLUMN series INTEGER REFERENCES Series(seriesId) ON DELETE CASCADE
        //            """,
        //            """CREATE INDEX IF NOT EXISTS index_Credit_series ON Credit(series)""",
        //            """CREATE INDEX IF NOT EXISTS index_Credit_issue ON Credit(issue)""",
        //            """CREATE INDEX IF NOT EXISTS index_ExCredit_series ON ExCredit(series)""",
        //            """CREATE INDEX IF NOT EXISTS index_ExCredit_issue ON ExCredit(issue)""",
        //            """CREATE INDEX IF NOT EXISTS index_Appearance_series ON Appearance(series)""",
        //            """CREATE INDEX IF NOT EXISTS index_Appearance_issue ON Appearance(issue)""",
        //        )
        //
    }
}

class SimpleMigration(fromVersion: Int, toVersion: Int, private vararg val sql: String) :
    Migration(
        fromVersion,
        toVersion
    ) {
    override fun migrate(database: SupportSQLiteDatabase) {
        sql.forEach {
            database.execSQL(it)
        }
    }
}