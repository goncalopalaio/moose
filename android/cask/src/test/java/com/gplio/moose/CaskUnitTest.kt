package com.gplio.moose

import com.gplio.cask.Cask
import org.junit.Assert
import org.junit.Test

import org.junit.Assert.*
import org.junit.BeforeClass
import java.io.File

class CaskUnitTest {
    private val filePath = "bin/test"
    private val cask = Cask(filePath)

    init {
        cask.init()
    }

    @Test
    fun new_values_are_added_to_new_file() {
        val existingFile = File(filePath)
        existingFile.delete()

        with("A" to "BBBB") {
            cask.add(first, second)
            val value = cask.read(first)
            assertEquals(second, value)
        }
        with("B" to "CCCCCCCC") {
            cask.add(first, second)
            val value = cask.read(first)
            assertEquals(second, value)
        }

        with("D" to "123456789EEEE123456789") {
            cask.add(first, second)
            val value = cask.read(first)
            assertEquals(second, value)
        }
    }
}