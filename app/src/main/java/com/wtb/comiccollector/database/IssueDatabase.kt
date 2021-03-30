package com.wtb.comiccollector.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.wtb.comiccollector.*
import com.wtb.comiccollector.Daos.*

@Database(
    entities = [Issue::class, Series::class, Creator::class, Role::class, Credit::class,
        Publisher::class, Story::class, MyCredit::class, StoryType::class, NameDetail::class,
        Character::class, Appearance::class],
    version = 1
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
}

//val migration_1_2 = object : Migration(1, 2) {
//    override fun migrate(database: SupportSQLiteDatabase) {
//        database.execSQL(
//            """ALTER TABLE story
//                ADD COLUMN issueId INTEGER DEFAULT 1132"""
//        )
//    }
//}
