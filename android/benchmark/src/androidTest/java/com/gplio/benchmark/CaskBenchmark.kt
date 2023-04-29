package com.gplio.benchmark

import android.util.Log
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val PATH = "bin/test"

/**
 * Benchmark, which will execute on an Android device.
 *
 * The body of [BenchmarkRule.measureRepeated] is measured in a loop, and Studio will
 * output the result. Modify your code to see how it affects performance.
 */
@RunWith(AndroidJUnit4::class)
class CaskBenchmark {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Test
    fun log() {
        val path = "$PATH-1"
        File(path).delete()
        val cask = Cask(path).apply { init() }

        benchmarkRule.measureRepeated {
            cask.add("aaa", "bbb")
        }
    }
}