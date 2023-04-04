package com.gplio.moose.storage.room.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.gplio.moose.storage.room.dao.EntryDao
import com.gplio.moose.storage.room.entity.Entry

@Database(entities = [Entry::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
}