package com.gplio.moose

import android.app.Application
import com.gplio.moose.storage.room.database.EntryDatabase

class MooseApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        EntryDatabase.setup(this)
    }
}