package com.wtb.comiccollector.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.wtb.comiccollector.*

@Database(
    entities = [Issue::class, Series::class, Creator::class, Role::class, Credit::class, Publisher::class],
    version = 2
)
@TypeConverters(IssueTypeConverters::class)

abstract class IssueDatabase : RoomDatabase() {
    abstract fun issueDao(): IssueDao
}

val migration_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE issue ADD COLUMN releaseDate TEXT DEFAULT null"
        )
    }
}
