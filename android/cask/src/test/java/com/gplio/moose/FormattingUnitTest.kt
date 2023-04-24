package com.gplio.moose

import com.gplio.cask.Cask
import org.junit.Test

import org.junit.Assert.*
import java.io.File

class FormattingUnitTest {
    private val cask = Cask()

    @Test
    fun files_are_written() {
        assertEquals(4, 2 + 2)
        createFile("0", "aaaa")
    }

    private fun createFile(key: String, value: String) {
        println("$key -> $value")
        val bytes = cask.put(key.toByteArray(), value.toByteArray())
        File("$key.bin").writeBytes(bytes)
    }
}