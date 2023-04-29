package com.gplio.moose

import android.os.Debug
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gplio.cask.Cask

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ProfilingInstrumentedTest {

    @Test
    fun generate_method_tracing_files() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.gplio.moose", appContext.packageName)

        val path = "${appContext.filesDir}/test-1"
        val cask = Cask(path)
        cask.init()

        Debug.startMethodTracing("adding")
        for (i in 0 until 100) {
            cask.add("$i", "$i")
        }
        Debug.stopMethodTracing()

        Debug.startMethodTracing("adding-existing")
        for (i in 0 until 100) {
            for (v in 1 until 100) {
                cask.add("$i", "${i * v}")
            }
        }
        Debug.stopMethodTracing()

        Debug.startMethodTracing("reading")
        for (i in 0 until 100) {
            val value = cask.read("$i")
            println("$i -> $value")
        }
        Debug.stopMethodTracing()
    }

    @Test
    fun measureKeyValueUsingRoom() {
        // TODO
    }
}