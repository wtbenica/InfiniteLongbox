package com.wtb.comiccollector.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.wtb.comiccollector.database.Daos.*
import com.wtb.comiccollector.database.models.*
import com.wtb.comiccollector.database.models.Issue

@Database(
    entities = [Issue::class, Series::class, Creator::class, Role::class, Credit::class,
        Publisher::class, Story::class, ExtractedCredit::class, StoryType::class, NameDetail::class,
        Character::class, Appearance::class, MyCollection::class, Cover::class],
    version = 4
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
    abstract fun storyTypeDao(): StoryTypeDao
    abstract fun nameDetailDao(): NameDetailDao
    abstract fun transactionDao(): TransactionDao
    abstract fun characterDao(): CharacterDao
    abstract fun appearanceDao(): AppearanceDao
    abstract fun collectionDao(): CollectionDao
    abstract fun coverDao(): CoverDao
}

val migration_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """CREATE TABLE IF NOT EXISTS 'MyCollection'(
                'collectionId' INTEGER NOT NULL PRIMARY KEY,
                'issueId' INTEGER NOT NULL,
                FOREIGN KEY ('issueId') REFERENCES Issue('issueId')
                ON DELETE CASCADE)"""
        )

        database.execSQL(
            """CREATE UNIQUE INDEX IF NOT EXISTS index_MyCollection_issueId
                ON MyCollection(issueId)
            """
        )
    }
}

val migration_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """CREATE TABLE IF NOT EXISTS 'Cover'(
                'coverId' INTEGER NOT NULL PRIMARY KEY,
                'issueId' INTEGER NOT NULL,
                'coverUri' TEXT,
                FOREIGN KEY ('issueId') REFERENCES Issue('issueId')
                ON DELETE CASCADE)"""
        )
    }
}

val migration_3_4 = SimpleMigration(
    3, 4,
    """CREATE UNIQUE INDEX IF NOT EXISTS index_Cover_issueId 
        ON Cover(issueId)"""
)

class SimpleMigration(from_version: Int, to_version: Int, private vararg val sql: String) :
    Migration(
        from_version,
        to_version
    ) {
    override fun migrate(database: SupportSQLiteDatabase) {
        sql.forEach {
            database.execSQL(it)
        }
    }
}