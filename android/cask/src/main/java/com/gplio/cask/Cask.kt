package com.gplio.cask

import java.nio.ByteBuffer
import java.util.zip.CRC32

class Cask {
    fun put(key: ByteArray, value: ByteArray): ByteArray {

        val epoch: Int = System.currentTimeMillis().toInt()
        val keySize: Int = key.size
        val valueSize: Int = value.size
        val keyType: Short = 1
        val valueType: Short = 2

        val headerNoCrc = with(ByteBuffer.allocate(3 * Int.SIZE_BYTES + 2 * Short.SIZE_BYTES)) {
            putInt(epoch)
            putInt(keySize)
            putInt(valueSize)
            putShort(keyType)
            put(valueType.toByte())
        }.array()

        val crc: Int = generateCrc32(headerNoCrc)
        println("$crc | $epoch | $keySize | $valueSize | $keyType | $valueType | $key | $value")
        val bb =
            ByteBuffer.allocate(4 * UInt.SIZE_BYTES + 2 * UShort.SIZE_BYTES + key.size + value.size)
        bb.putInt(crc)
        bb.put(headerNoCrc)
        bb.put(key)
        bb.put(value)

        return bb.array()
    }

    private fun generateCrc32(input: ByteArray): Int {
        val crc32 = CRC32()
        crc32.update(input)
        val value = crc32.value
        val result = value.toInt() // Fragile / Incorrect?
        println("crc32: $value")
        return result
    }
}