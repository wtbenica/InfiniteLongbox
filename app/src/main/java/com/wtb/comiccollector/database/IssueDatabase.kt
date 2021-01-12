package com.wtb.comiccollector.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.wtb.comiccollector.Issue

@Database(entities = [Issue::class], version = 1)
@TypeConverters(IssueTypeConverters::class)

abstract class IssueDatabase : RoomDatabase() {
    abstract fun issueDao(): IssueDao
}
