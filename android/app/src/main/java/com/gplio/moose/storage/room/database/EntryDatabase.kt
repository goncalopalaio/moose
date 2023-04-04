package com.gplio.moose.storage.room.database

import android.content.Context
import com.gplio.moose.storage.room.entity.Entry
import com.gplio.moose.storage.room.utils.build
import com.gplio.moose.storage.StringKeyValueStorage

object EntryDatabase : StringKeyValueStorage {
    private lateinit var db: AppDatabase

    fun setup(applicationContext: Context) {
        db = build(applicationContext)
    }

    override fun insert(vararg entry: Pair<String, String>) {
        val entries = Array(entry.size) {
            val e = entry[it]
            Entry(e.first, e.second)
        }

        db.entryDao().insertAll(*entries)
    }
}