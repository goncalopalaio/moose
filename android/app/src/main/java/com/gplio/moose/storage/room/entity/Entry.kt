package com.gplio.moose.storage.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Entry(
    @PrimaryKey val key: String,
    @ColumnInfo val value: String,
)