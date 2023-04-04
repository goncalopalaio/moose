package com.gplio.moose.storage.room.dao

import androidx.room.*
import com.gplio.moose.storage.room.entity.Entry
import com.gplio.moose.storage.room.utils.ROOM_DATABASE_NAME

@Dao
interface EntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg entries: Entry)

    @Query("SELECT value FROM entry WHERE key=:key")
    fun loadSingle(key: String): String?

    @Delete
    fun delete(entry: Entry)
}