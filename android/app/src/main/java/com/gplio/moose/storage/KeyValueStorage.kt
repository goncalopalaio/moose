package com.gplio.moose.storage

interface StringKeyValueStorage {
    fun insert(vararg entry: Pair<String, String>)
}