package com.gplio.moose.storage.room.utils

import android.content.Context
import androidx.room.Room
import com.gplio.moose.storage.room.database.AppDatabase

const val ROOM_DATABASE_NAME = "room-db"

fun build(applicationContext: Context): AppDatabase =
    Room.databaseBuilder(applicationContext, AppDatabase::class.java, ROOM_DATABASE_NAME)
        .build()