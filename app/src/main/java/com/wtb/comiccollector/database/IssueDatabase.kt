package com.wtb.comiccollector.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.wtb.comiccollector.Creator
import com.wtb.comiccollector.Issue
import com.wtb.comiccollector.Role
import com.wtb.comiccollector.Series

@Database(entities = [Issue::class, Series::class, Creator::class, Role::class], version = 1)
@TypeConverters(IssueTypeConverters::class)

abstract class IssueDatabase : RoomDatabase() {
    abstract fun issueDao(): IssueDao
}
