package com.gplio.cask

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer

private const val HEADER_SIZE = 3 * Int.SIZE_BYTES + 2 * Short.SIZE_BYTES

class Cask(private val caskPath: String) {
    // TODO (Gonçalo): Handle concurrent updates
    // TODO (Gonçalo): Is it worth to use FileChannel.map to map the file to memory completely? for small files it shouldn't
    // TODO (Gonçalo): Check FileChannel.lock

    private var endOfFile: Long = 0
    private val keyDirectory: HashMap<String, Long> = HashMap()

    fun init() {
        log("init - HEADER_SIZE=$HEADER_SIZE")
        // Merge - compact older entries
        // Update endOfFile
        // Update in-memory key offsets
    }

    fun read(key: String): String? {
        val offsetOfEntry = keyDirectory[key]
        log("read - key=$key, offsetOfEntry=$offsetOfEntry")
        if (offsetOfEntry == null) return null

        val file = File(caskPath)
        val fis = FileInputStream(file)
        fis.use {
            val channel = it.channel
            channel.position(offsetOfEntry)

            val size = channel.size()

            val headerBuffer = ByteBuffer.allocate(HEADER_SIZE)
            val header = arrayOf(headerBuffer)
            log("read - headerBuffer=$headerBuffer, offsetOfEntry=${offsetOfEntry.toInt()}, size=$size")
            val readHeaderBytes = channel.read(header, 0, 1)
            log("read - readHeaderBytes=$readHeaderBytes, headerBuffer=$headerBuffer, offsetOfEntry=${offsetOfEntry.toInt()}, size=$size")

            // TODO (Gonçalo): Check if header was completely read

            headerBuffer.flip()
            val epoch = headerBuffer.int
            val keySize = headerBuffer.int
            val valueSize = headerBuffer.int
            val keyType = headerBuffer.short
            val valueType = headerBuffer.short

            log("read - epoch=$epoch, keySize=$keySize, valueSize=$valueSize, keyType=$keyType, valueType=$valueType, key=$key")

            val bodyBuffer = ByteBuffer.allocate(keySize + valueSize)
            val body = arrayOf(bodyBuffer)
            val readBodyBytes = channel.read(body, 0, 1)
            log("read - readBodyBytes=$readBodyBytes, bodyBuffer=$bodyBuffer")

            // TODO (Gonçalo): Check if body was completely read

            bodyBuffer.flip()
            val bodyByteArray = bodyBuffer.array()
            val readKey = String(bodyByteArray, 0, keySize)
            val readValue = String(bodyByteArray, keySize, valueSize)
            log("read - readKey=$readKey, readValue=$readValue")

            // TODO (Gonçalo): check key matches
            return readValue
        }
    }

    fun add(key: String, value: String) {
        val entry = createEntry(key.toByteArray(), value.toByteArray())

        if (entry == null) {
            log("add - entry is null | key=$key, value=$value")
            return
        }

        appendToFile(caskPath) {
            val channel = it.channel

            val initialPosition =
                channel.position() // TODO (Gonçalo): Catch IOException; atomic updates of endOfFile
            val bytesWritten = channel.write(entry)
            log("add - key=$key, value=$value, initialPosition=$initialPosition, bytesWritten=$bytesWritten")

            if (bytesWritten == 0) return@appendToFile
            channel.force(false)

            endOfFile = initialPosition + bytesWritten
            keyDirectory[key] = initialPosition
            log("add - key=$key, initialPosition=$initialPosition, endOfFile=$endOfFile")
        }
    }

    private fun appendToFile(
        path: String,
        predicate: (fileOutputStream: FileOutputStream) -> Unit
    ) {
        val file = File(path)
        val append = true
        val fos = FileOutputStream(file, append)
        fos.use(predicate) // TODO (Gonçalo): Catch Throwable
    }

    private fun createEntry(key: ByteArray, value: ByteArray): ByteBuffer? {

        val epoch: Int = System.currentTimeMillis().toInt()
        val keySize: Int = key.size
        val valueSize: Int = value.size
        val keyType: Short = 1
        val valueType: Short = 2

        val entryNoCrc = with(ByteBuffer.allocate(HEADER_SIZE + key.size + value.size)) {
            putInt(epoch)
            putInt(keySize)
            putInt(valueSize)
            putShort(keyType)
            putShort(valueType)
            put(key)
            put(value)
        }

        log("createEntry - epoch=$epoch, keySize=$keySize, valueSize=$valueSize, keyType=$keyType, valueType=$valueType, key=$key, value=$value")
        return entryNoCrc.flip()
    }

    private fun log(text: String) {
        println(text) // TODO (Gonçalo): Inject real logger
    }
}