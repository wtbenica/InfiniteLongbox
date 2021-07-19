package com.wtb.comiccollector.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.wtb.comiccollector.database.Daos.*
import com.wtb.comiccollector.database.models.*
import com.wtb.comiccollector.database.models.Issue
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@Database(
    entities = [Issue::class, Series::class, Creator::class, Role::class, Credit::class,
        Publisher::class, Story::class, ExCredit::class, StoryType::class, NameDetail::class,
        Character::class, Appearance::class, MyCollection::class, Cover::class,
        SeriesBond::class, BondType::class, Brand::class],
    version = 9,
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
    abstract fun collectionDao(): CollectionDao
    abstract fun coverDao(): CoverDao
    abstract fun bondTypeDao(): BondTypeDao
    abstract fun seriesBondDao(): SeriesBondDao
}

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