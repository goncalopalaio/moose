package com.gplio.moose.ui.main

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.gplio.cask.Cask
import com.gplio.moose.storage.room.database.EntryDatabase
import kotlin.system.measureTimeMillis

private const val TAG = "MainViewModel"

class MainViewModel : ViewModel() {
    private val _report = MutableLiveData<String>()
    val report: LiveData<String> = _report

    fun runCask() {
        Cask("cask").apply { init() }
    }

    fun runRoom() {
        // TODO (Gonçalo): Inject database objects through view model.
        // TODO (Gonçalo): Use benchmarking library

        val runMultiple = false
        val runSingle = true

        if(runMultiple) {
            val measurementsMs = runMultipleRoom(10)
            val report = "multipleMs -> avg: ${measurementsMs.average()} ms | $measurementsMs"
            log(report)
        }

        if(runSingle) {
            val measurementsMs = runSingleRoom(10)
            val report = "singleMs -> avg: ${measurementsMs.average()} ms | $measurementsMs"
            log(report)
        }
    }

    private fun runMultipleRoom(times: Int): List<Long> {
        var i = 0
        val measurementsMs = mutableListOf<Long>()
        for (t in 1..times) {
            val multipleMs = measureTimeMillis {
                val newEntries = arrayOf(
                    "test${++i}" to "$i",
                    "test${++i}" to "$i",
                    "test${++i}" to "$i",
                    "test${++i}" to "$i",
                    "test${++i}" to "$i",
                    "test${++i}" to "$i",
                )
                EntryDatabase.insert(
                    *newEntries
                )
            }
            measurementsMs.add(multipleMs)
        }

        return measurementsMs
    }

    private fun runSingleRoom(times: Int): List<Long> {
        var i = 0
        val measurementsMs = mutableListOf<Long>()
        for (t in 1..times) {
            val multipleMs = measureTimeMillis {
                val newEntries = arrayOf(
                    "test${++i}" to "$i",
                )
                EntryDatabase.insert(
                    *newEntries
                )
            }
            measurementsMs.add(multipleMs)
        }

        return measurementsMs
    }

    private fun log(message: String) = Log.d(TAG, message)
}