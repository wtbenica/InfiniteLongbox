package com.wtb.comiccollector.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.wtb.comiccollector.database.daos.*
import com.wtb.comiccollector.database.models.*
import com.wtb.comiccollector.database.models.Issue
import com.wtb.comiccollector.repository.DUMMY_ID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.intellij.lang.annotations.Language
import java.time.LocalDate
import java.util.concurrent.Executors

private const val DATABASE_NAME = "incoming-issue-database"

@ExperimentalCoroutinesApi
@Database(
    entities = [Issue::class, Series::class, Creator::class, Role::class, Credit::class,
        Publisher::class, Story::class, ExCredit::class, StoryType::class, NameDetail::class,
        Character::class, Appearance::class, Cover::class, SeriesBond::class, BondType::class,
        Brand::class, UserCollection::class, CollectionItem::class],
    version = 1,
)
@TypeConverters(IssueTypeConverters::class)
abstract class IncomingIssueDatabase : RoomDatabase() {

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
        private var INSTANCE: IncomingIssueDatabase? = null

        fun getInstance(context: Context): IncomingIssueDatabase {
            return INSTANCE ?: synchronized(this) {
                return Room.inMemoryDatabaseBuilder(
                    context.applicationContext,
                    IncomingIssueDatabase::class.java,
                )
                    .createFromAsset(DATABASE_NAME)
                    .build().also {
                        INSTANCE = it
                    }
            }
        }
    }

    override fun close() {
        super.close()
        INSTANCE = null
    }
}
