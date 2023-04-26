package com.gplio.moose

import com.gplio.cask.Cask
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Stopwatch
import org.junit.runner.Description
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

private const val PATH = "bin/test"

class CaskUnitTest {
    @get:Rule
    val stopwatch = object : Stopwatch() {
        override fun succeeded(nanos: Long, description: Description?) =
            println(
                "${description?.className}: ${description?.methodName} took ${
                    TimeUnit.NANOSECONDS.toMillis(
                        nanos
                    )
                }ms"
            )
    }

    @Test
    fun new_values_are_added_to_new_file() {
        val path = "$PATH-1"
        File(path).delete()
        val cask = Cask(path)

        cask.init()

        cask.verifyAdd("A", "BBBB")
        cask.verifyAdd("B", "CCCCCCCC")
        cask.verifyAdd("D", "123456789EEEE123456789")
    }

    @Test
    fun single_entries_are_reindexed() {
        // Given
        val path = "$PATH-2"
        File(path).delete()

        // When
        with(Cask(path)) {
            init()
            verifyAdd("A", "1")
            verifyAdd("B", "2")
            verifyAdd("C", "3")
        }

        with(Cask(path)) {
            init()
            verifyAdd("A", "10")
            verifyAdd("B", "20")
            verifyAdd("C", "30")
        }

        with(Cask(path)) {
            init()
            verifyAdd("A", "100")
            verifyAdd("B", "200")
            verifyAdd("C", "300")
        }

        // Then
        with(Cask(path)) {
            init()
            val aValue = read("A")
            val bValue = read("B")
            val cValue = read("C")

            assertEquals("100", aValue)
            assertEquals("200", bValue)
            assertEquals("300", cValue)
        }
    }

    @Test
    fun last_version_of_key_is_returned() {
        val path = "$PATH-3"
        File(path).delete()

        val cask = Cask(path).apply { init() }
        val timeMs = measureTimeMillis {
            for (i in 1..11) {
                cask.verifyAdd("A", i.toString())
            }
        }

        println("timeMs=$timeMs") // TODO (Gonçalo): Getting ~220ms, real slow
    }

    private fun Cask.verifyAdd(key: String, value: String) {
        add(key, value)
        val readValue = read(key)
        assertEquals(value, readValue)
    }
}