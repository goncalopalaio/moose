package com.gplio.benchmark

import android.util.Log
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.gplio.cask.Cask
import org.junit.Assert.assertEquals
import org.junit.rules.Stopwatch
import org.junit.runner.Description
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import android.os.Environment

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
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        val path = "${appContext.getFilesDir()}/test-1"
        val file = File(path)
        file.delete()
        val cask = Cask(file).apply { init() }

        benchmarkRule.measureRepeated {
            cask.add("aaa", "bbb")
        }
    }
}